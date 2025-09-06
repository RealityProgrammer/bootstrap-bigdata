package com.bootstrapbigdata.backend.service;

import com.bootstrapbigdata.backend.entity.*;
import com.bootstrapbigdata.backend.repository.*;
import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.RowFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Service
public class SparkAnalyticsService {
    
    @Autowired
    private SparkSession sparkSession;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    /**
     * 1. Tạo UDF phân loại sản phẩm khuyến mãi dựa trên giá ban đầu
     */
    public void registerPromotionCategoryUDF() {
        // UDF to categorize promotion based on discount percentage
        UserDefinedFunction promotionCategoryUDF = udf((Double originalPrice, Double salePrice) -> {
            if (originalPrice == null || salePrice == null) return "NO_DATA";
            
            double discountPercent = ((originalPrice - salePrice) / originalPrice) * 100.0;
            
            if (discountPercent <= 0) return "NO_DISCOUNT";
            if (discountPercent <= 5) return "MINIMAL_DISCOUNT";
            if (discountPercent <= 15) return "SMALL_DISCOUNT";
            if (discountPercent <= 30) return "MEDIUM_DISCOUNT";
            if (discountPercent <= 50) return "LARGE_DISCOUNT";
            return "MEGA_DISCOUNT";
        }, DataTypes.StringType);
        
        sparkSession.udf().register("promotionCategory", promotionCategoryUDF);
        
        // UDF to calculate price effectiveness
        UserDefinedFunction priceEffectivenessUDF = udf((Integer salesVolume, Double discountPercent) -> {
            if (salesVolume == null || discountPercent == null) return 0.0;
            
            double baseScore = salesVolume.doubleValue();
            double discountPenalty = discountPercent > 20 ? discountPercent * 0.1 : 0;
            
            return Math.max(0, baseScore - discountPenalty);
        }, DataTypes.DoubleType);
        
        sparkSession.udf().register("priceEffectiveness", priceEffectivenessUDF);
        
        // UDF for rating impact assessment
        UserDefinedFunction ratingImpactUDF = udf((Double rating, Boolean isPromotion) -> {
            if (rating == null) return "NO_RATING";
            if (isPromotion == null) isPromotion = false;
            
            String ratingLevel = rating >= 4.5 ? "EXCELLENT" : 
                                rating >= 4.0 ? "GOOD" :
                                rating >= 3.0 ? "AVERAGE" : "POOR";
            
            return isPromotion ? ratingLevel + "_WITH_PROMO" : ratingLevel + "_NO_PROMO";
        }, DataTypes.StringType);
        
        sparkSession.udf().register("ratingImpact", ratingImpactUDF);
    }
    
    /**
     * 2. So sánh sales_volume giữa hàng giảm giá và không giảm giá
     */
    public Dataset<Row> comparePromotionVsNonPromotionSales() {
        Dataset<Row> salesData = loadSalesDataToSpark();
        
        return sparkSession.sql("""
            SELECT 
                is_promotion,
                COUNT(*) as total_transactions,
                SUM(sales_volume) as total_volume,
                AVG(sales_volume) as avg_volume,
                SUM(sale_price * sales_volume) as total_revenue,
                AVG(sale_price) as avg_price,
                AVG(discount_percentage) as avg_discount
            FROM sales_temp
            GROUP BY is_promotion
            ORDER BY is_promotion DESC
        """);
    }
    
    /**
     * 3. Phân tích ảnh hưởng giảm giá đến rating
     */
    public Dataset<Row> analyzePromotionImpactOnRating() {
        Dataset<Row> combinedData = loadCombinedDataToSpark();
        
        return sparkSession.sql("""
            SELECT 
                promotionCategory(original_price, sale_price) as promotion_category,
                COUNT(DISTINCT product_id) as product_count,
                AVG(rating) as avg_rating,
                AVG(sales_volume) as avg_sales,
                ratingImpact(rating, is_promotion) as rating_impact_category,
                COUNT(rating_impact_category) as impact_count
            FROM combined_temp
            WHERE rating IS NOT NULL
            GROUP BY promotionCategory(original_price, sale_price), ratingImpact(rating, is_promotion)
            ORDER BY avg_rating DESC
        """);
    }
    
    /**
     * 4. Tính hệ số chuyển đổi review/doanh số khi có khuyến mãi
     */
    public Dataset<Row> calculateReviewConversionRate() {
        Dataset<Row> combinedData = loadCombinedDataToSpark();
        
        return sparkSession.sql("""
            SELECT 
                brand,
                is_promotion,
                SUM(sales_volume) as total_sales,
                COUNT(DISTINCT CASE WHEN rating IS NOT NULL THEN review_id END) as total_reviews,
                CASE 
                    WHEN SUM(sales_volume) > 0 THEN 
                        COUNT(DISTINCT CASE WHEN rating IS NOT NULL THEN review_id END) / SUM(sales_volume)
                    ELSE 0 
                END as review_conversion_rate,
                AVG(rating) as avg_rating,
                AVG(discount_percentage) as avg_discount
            FROM combined_temp
            GROUP BY brand, is_promotion
            HAVING SUM(sales_volume) > 0
            ORDER BY review_conversion_rate DESC
        """);
    }
    
    /**
     * 5. Tìm ngưỡng giá tối ưu để đạt doanh số cao
     */
    public Dataset<Row> findOptimalPriceThreshold() {
        Dataset<Row> salesData = loadSalesDataToSpark();
        
        return sparkSession.sql("""
            WITH price_buckets AS (
                SELECT 
                    brand,
                    product_type,
                    CASE 
                        WHEN sale_price < 100 THEN 'UNDER_100'
                        WHEN sale_price < 300 THEN '100_300'
                        WHEN sale_price < 500 THEN '300_500'
                        WHEN sale_price < 1000 THEN '500_1000'
                        ELSE 'OVER_1000'
                    END as price_range,
                    is_promotion,
                    sales_volume,
                    sale_price,
                    priceEffectiveness(sales_volume, discount_percentage) as effectiveness
                FROM sales_temp
            )
            SELECT 
                brand,
                product_type,
                price_range,
                is_promotion,
                COUNT(*) as transaction_count,
                SUM(sales_volume) as total_volume,
                AVG(sale_price) as avg_price,
                SUM(effectiveness) as total_effectiveness,
                AVG(effectiveness) as avg_effectiveness
            FROM price_buckets
            GROUP BY brand, product_type, price_range, is_promotion
            ORDER BY avg_effectiveness DESC, total_volume DESC
        """);
    }
    
    /**
     * 6. Tính toán các chỉ số hiệu quả bán hàng
     */
    public Dataset<Row> calculateSalesEfficiencyMetrics() {
        Dataset<Row> combinedData = loadCombinedDataToSpark();
        
        return sparkSession.sql("""
            WITH efficiency_metrics AS (
                SELECT 
                    product_id,
                    product_name,
                    brand,
                    product_type,
                    SUM(sales_volume) as total_sales,
                    COUNT(DISTINCT sale_id) as transaction_count,
                    SUM(sale_price * sales_volume) as total_revenue,
                    AVG(rating) as avg_rating,
                    COUNT(DISTINCT CASE WHEN rating IS NOT NULL THEN review_id END) as review_count,
                    AVG(CASE WHEN is_promotion THEN discount_percentage ELSE 0 END) as avg_discount_when_promo,
                    SUM(CASE WHEN is_promotion THEN sales_volume ELSE 0 END) as promo_sales,
                    SUM(CASE WHEN is_promotion = false THEN sales_volume ELSE 0 END) as regular_sales
                FROM combined_temp
                GROUP BY product_id, product_name, brand, product_type
            )
            SELECT 
                *,
                total_revenue / total_sales as revenue_per_unit,
                review_count / total_sales as review_rate,
                CASE 
                    WHEN regular_sales > 0 THEN (promo_sales - regular_sales) / regular_sales * 100
                    ELSE 0 
                END as promotion_lift_percent,
                (total_revenue * 0.3 + total_sales * 0.4 + avg_rating * 20 + review_rate * 100) as efficiency_score
            FROM efficiency_metrics
            ORDER BY efficiency_score DESC
        """);
    }
    
    /**
     * 7. So sánh theo brand và loại sản phẩm
     */
    public Dataset<Row> compareBrandAndProductType() {
        Dataset<Row> combinedData = loadCombinedDataToSpark();
        
        return sparkSession.sql("""
            SELECT 
                brand,
                product_type,
                COUNT(DISTINCT product_id) as product_count,
                SUM(sales_volume) as total_sales,
                SUM(sale_price * sales_volume) as total_revenue,
                AVG(sale_price) as avg_price,
                AVG(rating) as avg_rating,
                COUNT(DISTINCT CASE WHEN rating IS NOT NULL THEN review_id END) as total_reviews,
                SUM(CASE WHEN is_promotion THEN sales_volume ELSE 0 END) as promo_sales,
                AVG(CASE WHEN is_promotion THEN discount_percentage ELSE 0 END) as avg_discount,
                CASE 
                    WHEN COUNT(DISTINCT product_id) > 0 
                    THEN total_revenue / COUNT(DISTINCT product_id)
                    ELSE 0 
                END as revenue_per_product
            FROM combined_temp
            GROUP BY brand, product_type
            ORDER BY total_revenue DESC, avg_rating DESC
        """);
    }
    
    /**
     * Load sales data từ database vào Spark DataFrame
     */
    private Dataset<Row> loadSalesDataToSpark() {
        List<Sale> sales = saleRepository.findAll();
        
        List<Row> rows = sales.stream().map(sale -> {
            Product product = sale.getProduct();
            return RowFactory.create(
                sale.getId(),
                product.getId(),
                product.getProductName(),
                product.getBrand(),
                product.getProductType(),
                product.getPlatform(),
                product.getOriginalPrice() != null ? product.getOriginalPrice().doubleValue() : null,
                sale.getSalesVolume(),
                sale.getSalePrice() != null ? sale.getSalePrice().doubleValue() : null,
                sale.getIsPromotion(),
                sale.getDiscountPercentage() != null ? sale.getDiscountPercentage().doubleValue() : 0.0,
                Timestamp.valueOf(sale.getSaleTime())
            );
        }).collect(Collectors.toList());
        
        StructType schema = DataTypes.createStructType(Arrays.asList(
            DataTypes.createStructField("sale_id", DataTypes.LongType, false),
            DataTypes.createStructField("product_id", DataTypes.LongType, false),
            DataTypes.createStructField("product_name", DataTypes.StringType, false),
            DataTypes.createStructField("brand", DataTypes.StringType, false),
            DataTypes.createStructField("product_type", DataTypes.StringType, false),
            DataTypes.createStructField("platform", DataTypes.StringType, false),
            DataTypes.createStructField("original_price", DataTypes.DoubleType, true),
            DataTypes.createStructField("sales_volume", DataTypes.IntegerType, false),
            DataTypes.createStructField("sale_price", DataTypes.DoubleType, false),
            DataTypes.createStructField("is_promotion", DataTypes.BooleanType, false),
            DataTypes.createStructField("discount_percentage", DataTypes.DoubleType, false),
            DataTypes.createStructField("sale_time", DataTypes.TimestampType, false)
        ));
        
        Dataset<Row> salesDataset = sparkSession.createDataFrame(rows, schema);
        salesDataset.createOrReplaceTempView("sales_temp");
        
        return salesDataset;
    }
    
    /**
     * Load combined data (sales + reviews + products) vào Spark DataFrame
     */
    private Dataset<Row> loadCombinedDataToSpark() {
        List<Object[]> combinedData = saleRepository.findCombinedSalesData();
        
        List<Row> rows = combinedData.stream().map(data -> {
            return RowFactory.create(
                data[0], // sale_id
                data[1], // product_id
                data[2], // product_name
                data[3], // brand
                data[4], // product_type
                data[5], // platform
                data[6], // sales_volume
                data[7] != null ? ((Number) data[7]).doubleValue() : null, // sale_price
                data[8], // is_promotion
                data[9] != null ? ((Number) data[9]).doubleValue() : 0.0, // discount_percentage
                data[10], // sale_time
                data[11], // review_id
                data[12] != null ? ((Number) data[12]).doubleValue() : null, // rating
                data[13] // review_date
            );
        }).collect(Collectors.toList());
        
        StructType schema = DataTypes.createStructType(Arrays.asList(
            DataTypes.createStructField("sale_id", DataTypes.LongType, false),
            DataTypes.createStructField("product_id", DataTypes.LongType, false),
            DataTypes.createStructField("product_name", DataTypes.StringType, false),
            DataTypes.createStructField("brand", DataTypes.StringType, false),
            DataTypes.createStructField("product_type", DataTypes.StringType, false),
            DataTypes.createStructField("platform", DataTypes.StringType, false),
            DataTypes.createStructField("sales_volume", DataTypes.IntegerType, false),
            DataTypes.createStructField("sale_price", DataTypes.DoubleType, true),
            DataTypes.createStructField("is_promotion", DataTypes.BooleanType, false),
            DataTypes.createStructField("discount_percentage", DataTypes.DoubleType, false),
            DataTypes.createStructField("sale_time", DataTypes.TimestampType, false),
            DataTypes.createStructField("review_id", DataTypes.LongType, true),
            DataTypes.createStructField("rating", DataTypes.DoubleType, true),
            DataTypes.createStructField("review_date", DataTypes.TimestampType, true)
        ));
        
        Dataset<Row> combinedDataset = sparkSession.createDataFrame(rows, schema);
        combinedDataset.createOrReplaceTempView("combined_temp");
        
        // Add original_price column by joining with products
        List<Product> products = productRepository.findAll();
        Map<Long, Double> originalPriceMap = products.stream()
                .filter(p -> p.getOriginalPrice() != null)
                .collect(Collectors.toMap(Product::getId, p -> p.getOriginalPrice().doubleValue()));
        
        Dataset<Row> withOriginalPrice = combinedDataset.withColumn("original_price", 
            when(col("product_id").isNotNull(), 
                lit(originalPriceMap.getOrDefault(1L, 0.0))) // This should be improved with proper join
            .otherwise(lit(0.0))
        );
        
        withOriginalPrice.createOrReplaceTempView("combined_temp");
        
        return withOriginalPrice;
    }
    
    /**
     * Initialize UDFs - should be called when service starts
     */
    public void initializeUDFs() {
        registerPromotionCategoryUDF();
    }
    
    /**
     * Get top performing products with Spark analysis
     */
    public List<Map<String, Object>> getTopPerformingProducts(int limit) {
        Dataset<Row> result = calculateSalesEfficiencyMetrics().limit(limit);
        
        return result.collectAsList().stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", row.getLong(0));
            map.put("productName", row.getString(1));
            map.put("brand", row.getString(2));
            map.put("productType", row.getString(3));
            map.put("totalSales", row.getLong(4));
            map.put("totalRevenue", row.getDouble(6));
            map.put("avgRating", row.getDouble(7));
            map.put("efficiencyScore", row.getDouble(row.size() - 1));
            return map;
        }).collect(Collectors.toList());
    }
    
    /**
     * Get promotion effectiveness analysis
     */
    public List<Map<String, Object>> getPromotionEffectivenessAnalysis() {
        Dataset<Row> result = comparePromotionVsNonPromotionSales();
        
        return result.collectAsList().stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("isPromotion", row.getBoolean(0));
            map.put("totalTransactions", row.getLong(1));
            map.put("totalVolume", row.getLong(2));
            map.put("avgVolume", row.getDouble(3));
            map.put("totalRevenue", row.getDouble(4));
            map.put("avgPrice", row.getDouble(5));
            map.put("avgDiscount", row.getDouble(6));
            return map;
        }).collect(Collectors.toList());
    }
    
    /**
     * Get brand comparison analysis
     */
    public List<Map<String, Object>> getBrandComparisonAnalysis() {
        Dataset<Row> result = compareBrandAndProductType();
        
        return result.collectAsList().stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("brand", row.getString(0));
            map.put("productType", row.getString(1));
            map.put("productCount", row.getLong(2));
            map.put("totalSales", row.getLong(3));
            map.put("totalRevenue", row.getDouble(4));
            map.put("avgPrice", row.getDouble(5));
            map.put("avgRating", row.getDouble(6));
            map.put("totalReviews", row.getLong(7));
            map.put("promoSales", row.getLong(8));
            map.put("avgDiscount", row.getDouble(9));
            map.put("revenuePerProduct", row.getDouble(10));
            return map;
        }).collect(Collectors.toList());
    }
}
