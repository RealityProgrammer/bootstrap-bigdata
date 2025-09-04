package com.bootstrapbigdata.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import static org.apache.spark.sql.functions.callUDF;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.monotonically_increasing_id;
import static org.apache.spark.sql.functions.rand;
import static org.apache.spark.sql.functions.udf;
import static org.apache.spark.sql.functions.when;
import org.apache.spark.sql.types.DataTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bootstrapbigdata.backend.config.AppConfig;

@Service
public class AnalysisService {

    @Autowired
    private SparkSession spark;
    
    @Autowired
    private AppConfig appConfig;

    private Dataset<Row> readDataset(String path) {
        String fullPath = path.startsWith("hdfs://") ? path : appConfig.getHdfsUri() + path;
        
        Dataset<Row> df = spark.read()
                .option("header", "true")
                .option("inferSchema", "true")
                .option("sep", ";")  // CSV sử dụng dấu chấm phẩy
                .csv(fullPath);

        // Chuẩn hóa tên cột và tạo cột fake cần thiết
        return df.withColumnRenamed("product_type", "category")
                .withColumnRenamed("reviews", "review_count")
                .withColumn("product_id", monotonically_increasing_id())
                // Fake data: Giả sử price hiện tại là giá khuyến mãi
                .withColumn("promo_price", col("price"))
                // Fake original_price: Tăng 15-40% so với price hiện tại
                .withColumn("original_price", 
                    col("price").multiply(lit(1.15).plus(rand().multiply(lit(0.25)))))
                // Tính discount_rate từ original_price và promo_price
                .withColumn("discount_rate", 
                    (col("original_price").minus(col("promo_price"))).divide(col("original_price")))
                // Tạo cột auxiliary
                .withColumn("is_discount", col("discount_rate").gt(0.05)) // > 5% mới coi là khuyến mãi
                .withColumn("revenue", col("promo_price").multiply(col("sales_volume")))
                .filter(col("sales_volume").isNotNull()
                    .and(col("price").isNotNull())
                    .and(col("brand").isNotNull())
                    .and(col("category").isNotNull())
                    .and(col("rating").isNotNull())
                    .and(col("review_count").isNotNull()));
    }

    // Sửa UDF để phù hợp với dữ liệu
    private void registerPromoUDF() {
        UserDefinedFunction promoClassUDF = udf((Double originalPrice, Double promoPrice) -> {
            if (originalPrice == null || promoPrice == null || originalPrice == 0) {
                return "NO_DISCOUNT";
            }
            
            double discountRate = (originalPrice - promoPrice) / originalPrice;
            
            if (discountRate <= 0.05) return "NO_DISCOUNT";
            else if (discountRate <= 0.15) return "LOW_DISCOUNT";
            else if (discountRate <= 0.30) return "MEDIUM_DISCOUNT";
            else return "HIGH_DISCOUNT";
        }, DataTypes.StringType);

        spark.udf().register("promoClass", promoClassUDF);
    }

    public Map<String, Object> runFullAnalysis(String datasetPath) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            Dataset<Row> df = readDataset(datasetPath);
            
            registerPromoUDF();
            
            // Tạo các cột phân tích với dữ liệu thực
            df = df.withColumn("promo_class", callUDF("promoClass", col("original_price"), col("promo_price")))
                .withColumn("review_per_sale", 
                    when(col("sales_volume").equalTo(0), 0.0)
                    .otherwise(col("review_count").divide(col("sales_volume"))))
                // Thêm phân loại theo sale_time (mùa vụ)
                .withColumn("season_type", 
                    when(col("sale_time").isin("Tháng 12", "Tháng 1", "Tháng 2"), "Winter")
                    .when(col("sale_time").isin("Tháng 3", "Tháng 4", "Tháng 5"), "Spring")
                    .when(col("sale_time").isin("Tháng 6", "Tháng 7", "Tháng 8"), "Summer")
                    .otherwise("Autumn"));

            df.createOrReplaceTempView("sales_data");

            // Thực hiện các phân tích theo yêu cầu
            results.put("promotion_classification", analyzePromotionClassification());
            results.put("sales_comparison", analyzeSalesComparison());
            results.put("rating_impact", analyzeRatingImpact());
            results.put("review_conversion", analyzeReviewConversion());
            results.put("optimal_price", analyzeOptimalPrice());
            results.put("overall_kpis", calculateOverallKPIs());
            results.put("brand_category_analysis", analyzeBrandCategory());
            results.put("platform_comparison", analyzePlatformComparison()); // Mới
            results.put("seasonal_analysis", analyzeSeasonalImpact()); // Mới

            String outputBase = appConfig.getHdfsUri() + appConfig.getPaths().getOutputDir() + "/analysis";
            results.put("hdfs_outputs", saveAnalysisResults(outputBase));

        } catch (Exception e) {
            throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
        }

        return results;
    }

    private Map<String, Object> analyzePromotionClassification() {
        Dataset<Row> result = spark.sql("""
            SELECT promo_class,
                   COUNT(*) as count,
                   AVG(discount_rate) as avg_discount_rate,
                   AVG(sales_volume) as avg_sales_volume,
                   SUM(revenue) as total_revenue
            FROM sales_data
            GROUP BY promo_class
            ORDER BY avg_sales_volume DESC
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "promotion_classification")
        );
    }

    private Map<String, Object> analyzeSalesComparison() {
        Dataset<Row> result = spark.sql("""
            SELECT is_discount,
                   COUNT(*) as count,
                   AVG(sales_volume) as avg_sales_volume,
                   SUM(sales_volume) as total_sales_volume,
                   AVG(revenue) as avg_revenue,
                   SUM(revenue) as total_revenue
            FROM sales_data
            GROUP BY is_discount
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "sales_comparison")
        );
    }

    private Map<String, Object> analyzeRatingImpact() {
        Dataset<Row> result = spark.sql("""
            SELECT CASE 
                     WHEN discount_rate = 0 THEN '0%'
                     WHEN discount_rate <= 0.05 THEN '0-5%'
                     WHEN discount_rate <= 0.10 THEN '5-10%'
                     WHEN discount_rate <= 0.20 THEN '10-20%'
                     WHEN discount_rate <= 0.30 THEN '20-30%'
                     ELSE '30%+'
                   END as discount_bucket,
                   AVG(rating) as avg_rating,
                   COUNT(*) as count,
                   STDDEV(rating) as rating_stddev
            FROM sales_data
            WHERE rating IS NOT NULL
            GROUP BY CASE 
                       WHEN discount_rate = 0 THEN '0%'
                       WHEN discount_rate <= 0.05 THEN '0-5%'
                       WHEN discount_rate <= 0.10 THEN '5-10%'
                       WHEN discount_rate <= 0.20 THEN '10-20%'
                       WHEN discount_rate <= 0.30 THEN '20-30%'
                       ELSE '30%+'
                     END
            ORDER BY avg_rating DESC
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "rating_impact")
        );
    }

    private Map<String, Object> analyzeReviewConversion() {
        Dataset<Row> result = spark.sql("""
            SELECT CASE 
                     WHEN discount_rate = 0 THEN '0%'
                     WHEN discount_rate <= 0.05 THEN '0-5%'
                     WHEN discount_rate <= 0.10 THEN '5-10%'
                     WHEN discount_rate <= 0.20 THEN '10-20%'
                     WHEN discount_rate <= 0.30 THEN '20-30%'
                     ELSE '30%+'
                   END as discount_bucket,
                   AVG(review_per_sale) as avg_review_per_sale,
                   COUNT(*) as count
            FROM sales_data
            WHERE sales_volume > 0
            GROUP BY CASE 
                       WHEN discount_rate = 0 THEN '0%'
                       WHEN discount_rate <= 0.05 THEN '0-5%'
                       WHEN discount_rate <= 0.10 THEN '5-10%'
                       WHEN discount_rate <= 0.20 THEN '10-20%'
                       WHEN discount_rate <= 0.30 THEN '20-30%'
                       ELSE '30%+'
                     END
            ORDER BY avg_review_per_sale DESC
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "review_conversion")
        );
    }

    private Map<String, Object> analyzeOptimalPrice() {
        // Calculate price quantiles for bucketing
        Dataset<Row> quantiles = spark.sql("""
            SELECT percentile_approx(promo_price, array(0.2, 0.4, 0.6, 0.8)) as quantiles
            FROM sales_data
            WHERE promo_price > 0
        """);

        Dataset<Row> result = spark.sql("""
            SELECT CASE 
                     WHEN promo_price <= 50000 THEN '0-50K'
                     WHEN promo_price <= 100000 THEN '50K-100K'
                     WHEN promo_price <= 200000 THEN '100K-200K'
                     WHEN promo_price <= 500000 THEN '200K-500K'
                     ELSE '500K+'
                   END as price_bucket,
                   AVG(sales_volume) as avg_sales_volume,
                   COUNT(*) as count,
                   AVG(promo_price) as avg_price,
                   SUM(revenue) as total_revenue
            FROM sales_data
            WHERE promo_price > 0
            GROUP BY CASE 
                       WHEN promo_price <= 50000 THEN '0-50K'
                       WHEN promo_price <= 100000 THEN '50K-100K'
                       WHEN promo_price <= 200000 THEN '100K-200K'
                       WHEN promo_price <= 500000 THEN '200K-500K'
                       ELSE '500K+'
                     END
            ORDER BY avg_sales_volume DESC
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "optimal_price")
        );
    }

    private Map<String, Object> calculateOverallKPIs() {
        Dataset<Row> result = spark.sql("""
            SELECT 
                COUNT(*) as total_products,
                AVG(discount_rate) as avg_discount_rate,
                AVG(sales_volume) as avg_sales_volume,
                SUM(revenue) as total_revenue,
                AVG(rating) as avg_rating,
                AVG(review_per_sale) as avg_review_per_sale,
                COUNT(CASE WHEN is_discount THEN 1 END) as discounted_products,
                COUNT(CASE WHEN is_discount THEN 1 END) * 100.0 / COUNT(*) as discount_penetration_pct
            FROM sales_data
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "overall_kpis")
        );
    }

    private Map<String, Object> analyzeBrandCategory() {
        Dataset<Row> result = spark.sql("""
            SELECT brand,
                   category,
                   COUNT(*) as product_count,
                   AVG(discount_rate) as avg_discount_rate,
                   AVG(sales_volume) as avg_sales_volume,
                   SUM(revenue) as total_revenue,
                   AVG(rating) as avg_rating
            FROM sales_data
            GROUP BY brand, category
            ORDER BY total_revenue DESC
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "brand_category_analysis")
        );
    }

    // Thêm phân tích platform
    private Map<String, Object> analyzePlatformComparison() {
        Dataset<Row> result = spark.sql("""
            SELECT platform,
                   COUNT(*) as product_count,
                   AVG(sales_volume) as avg_sales_volume,
                   AVG(original_price) as avg_original_price,
                   AVG(promo_price) as avg_promo_price,
                   AVG(discount_rate) as avg_discount_rate,
                   SUM(revenue) as total_revenue,
                   AVG(rating) as avg_rating,
                   AVG(review_count) as avg_reviews
            FROM sales_data
            GROUP BY platform
            ORDER BY total_revenue DESC
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "platform_comparison")
        );
    }

    // Thêm phân tích theo mùa vụ
    private Map<String, Object> analyzeSeasonalImpact() {
        Dataset<Row> result = spark.sql("""
            SELECT season_type,
                   sale_time,
                   COUNT(*) as product_count,
                   AVG(sales_volume) as avg_sales_volume,
                   AVG(discount_rate) as avg_discount_rate,
                   SUM(revenue) as total_revenue,
                   AVG(rating) as avg_rating
            FROM sales_data
            GROUP BY season_type, sale_time
            ORDER BY avg_sales_volume DESC
        """);

        return Map.of(
            "data", result.collectAsList(),
            "csv_path", saveDatasetAsCSV(result, "seasonal_analysis")
        );
    }

    private String saveDatasetAsCSV(Dataset<Row> dataset, String fileName) {
        String outputPath = appConfig.getHdfsUri() + appConfig.getPaths().getOutputDir() + "/analysis/" + fileName;
        
        dataset.coalesce(1)
                .write()
                .mode(SaveMode.Overwrite)
                .option("header", "true")
                .csv(outputPath);
        
        return outputPath;
    }

    private Map<String, String> saveAnalysisResults(String outputBase) {
        Map<String, String> outputs = new HashMap<>();
        // Return the paths where CSV files are saved
        outputs.put("promotion_classification", outputBase + "/promotion_classification");
        outputs.put("sales_comparison", outputBase + "/sales_comparison");
        outputs.put("rating_impact", outputBase + "/rating_impact");
        outputs.put("review_conversion", outputBase + "/review_conversion");
        outputs.put("optimal_price", outputBase + "/optimal_price");
        outputs.put("overall_kpis", outputBase + "/overall_kpis");
        outputs.put("brand_category_analysis", outputBase + "/brand_category_analysis");
        return outputs;
    }

    // Thêm method tạo fake data realistic hơn
    private Dataset<Row> enrichDataWithBusinessLogic(Dataset<Row> df) {
        return df
            // Tạo original_price dựa trên business logic
            .withColumn("price_multiplier",
                when(col("category").like("%Kem chống nắng%"), 1.3)
                .when(col("category").like("%Mỹ phẩm%"), 1.25)
                .when(col("category").like("%Điện thoại%"), 1.15)
                .when(col("platform").equalTo("Lazada"), 1.2)
                .otherwise(1.18))
            .withColumn("original_price", 
                col("price").multiply(col("price_multiplier")))
                
            // Tạo seasonal discount pattern
            .withColumn("seasonal_discount_boost",
                when(col("sale_time").isin("Tháng 11", "Tháng 12"), 0.05) // Black Friday, Xmas
                .when(col("sale_time").isin("Tháng 6", "Tháng 7"), 0.03) // Mid year sale
                .otherwise(0.0))
            
            .withColumn("discount_rate", 
                (col("original_price").minus(col("promo_price"))).divide(col("original_price"))
                .plus(col("seasonal_discount_boost")))
            
            // Business rules: Higher rating products have lower discount
            .withColumn("rating_discount_factor",
                when(col("rating").gt(4.5), -0.02)
                .when(col("rating").lt(2.0), 0.05)
                .otherwise(0.0))
            
            .withColumn("final_discount_rate",
                col("discount_rate").plus(col("rating_discount_factor")))
            
            .drop("price_multiplier", "seasonal_discount_boost", "rating_discount_factor");
}
}