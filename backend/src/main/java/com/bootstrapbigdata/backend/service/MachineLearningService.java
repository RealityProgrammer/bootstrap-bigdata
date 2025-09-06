package com.bootstrapbigdata.backend.service;

import com.bootstrapbigdata.backend.entity.Product;
import com.bootstrapbigdata.backend.entity.Sale;
import com.bootstrapbigdata.backend.repository.SaleRepository;
import com.bootstrapbigdata.backend.repository.ProductRepository;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.feature.StandardScaler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.GBTRegressor;
import org.apache.spark.ml.regression.RandomForestRegressor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.RowFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Service
public class MachineLearningService {
    
    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private SparkSession sparkSession;
    
    private PipelineModel trainedModel;
    private Random random = new Random();
    
    /**
     * 8. Tạo mô hình dự đoán sales tăng bao nhiêu khi giảm 10%
     */
    public SalesPredictionResult predictSalesIncrease(Long productId, BigDecimal discountPercentage) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            
            List<Sale> productSales = saleRepository.findByProductId(productId);
            if (productSales.isEmpty()) {
                return buildDefaultPrediction(productId, discountPercentage, "No historical sales data");
            }
            
            // Try ML prediction first
            try {
                return predictWithMLlib(product, productSales, discountPercentage);
            } catch (Exception e) {
                // Fallback to statistical prediction
                return predictWithStatistics(product, productSales, discountPercentage);
            }
            
        } catch (Exception e) {
            return buildDefaultPrediction(productId, discountPercentage, "Error: " + e.getMessage());
        }
    }
    
    /**
     * MLlib-based prediction using trained models
     */
    private SalesPredictionResult predictWithMLlib(Product product, List<Sale> sales, BigDecimal discountPercentage) {
        // Prepare training data
        Dataset<Row> trainingData = prepareTrainingData(sales);
        
        if (trainingData.count() < 3) {
            return predictWithStatistics(product, sales, discountPercentage);
        }
        
        // Train multiple models and ensemble
        LinearRegression lr = new LinearRegression()
                .setFeaturesCol("scaledFeatures")
                .setLabelCol("salesVolume")
                .setRegParam(0.01)
                .setMaxIter(100);
        
        GBTRegressor gbt = new GBTRegressor()
                .setFeaturesCol("scaledFeatures")
                .setLabelCol("salesVolume")
                .setMaxIter(50)
                .setMaxDepth(5);
        
        RandomForestRegressor rf = new RandomForestRegressor()
                .setFeaturesCol("scaledFeatures")
                .setLabelCol("salesVolume")
                .setNumTrees(10)
                .setMaxDepth(5);
        
        // Prepare features
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[]{"price", "discountPercentage", "dayOfYear", "isWeekend"})
                .setOutputCol("features");
        
        StandardScaler scaler = new StandardScaler()
                .setInputCol("features")
                .setOutputCol("scaledFeatures");
        
        // Create pipelines
        Pipeline lrPipeline = new Pipeline().setStages(new PipelineStage[]{assembler, scaler, lr});
        Pipeline gbtPipeline = new Pipeline().setStages(new PipelineStage[]{assembler, scaler, gbt});
        Pipeline rfPipeline = new Pipeline().setStages(new PipelineStage[]{assembler, scaler, rf});
        
        // Split data
        Dataset<Row>[] splits = trainingData.randomSplit(new double[]{0.8, 0.2});
        Dataset<Row> train = splits[0];
        Dataset<Row> test = splits[1];
        
        try {
            // Train models
            PipelineModel lrModel = lrPipeline.fit(train);
            PipelineModel gbtModel = gbtPipeline.fit(train);
            PipelineModel rfModel = rfPipeline.fit(train);
            
            // Evaluate models
            RegressionEvaluator evaluator = new RegressionEvaluator()
                    .setLabelCol("salesVolume")
                    .setPredictionCol("prediction")
                    .setMetricName("rmse");
            
            double lrRmse = test.count() > 0 ? evaluator.evaluate(lrModel.transform(test)) : Double.MAX_VALUE;
            double gbtRmse = test.count() > 0 ? evaluator.evaluate(gbtModel.transform(test)) : Double.MAX_VALUE;
            double rfRmse = test.count() > 0 ? evaluator.evaluate(rfModel.transform(test)) : Double.MAX_VALUE;
            
            // Choose best model
            PipelineModel bestModel;
            String bestModelType;
            double bestRmse;
            
            if (lrRmse <= gbtRmse && lrRmse <= rfRmse) {
                bestModel = lrModel;
                bestModelType = "Linear Regression";
                bestRmse = lrRmse;
            } else if (gbtRmse <= rfRmse) {
                bestModel = gbtModel;
                bestModelType = "Gradient Boosted Trees";
                bestRmse = gbtRmse;
            } else {
                bestModel = rfModel;
                bestModelType = "Random Forest";
                bestRmse = rfRmse;
            }
            
            // Make prediction
            Dataset<Row> predictionInput = createPredictionInput(product, discountPercentage);
            Dataset<Row> prediction = bestModel.transform(predictionInput);
            
            double predictedSales = prediction.select("prediction").first().getDouble(0);
            
            // Calculate baseline (average sales without promotion)
            double baselineSales = sales.stream()
                    .filter(s -> !s.getIsPromotion())
                    .mapToInt(Sale::getSalesVolume)
                    .average()
                    .orElse(0.0);
            
            double increasePercent = baselineSales > 0 ? ((predictedSales - baselineSales) / baselineSales) * 100 : 0;
            
            return new SalesPredictionResult(
                    product.getId(),
                    Math.max(0, (int) Math.round(predictedSales)),
                    BigDecimal.valueOf(increasePercent).setScale(2, RoundingMode.HALF_UP),
                    String.format("MLlib prediction using %s model (RMSE: %.2f)", bestModelType, bestRmse),
                    discountPercentage,
                    BigDecimal.valueOf(baselineSales).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(calculateConfidenceScore(bestRmse, sales.size())),
                    generateRecommendation(increasePercent, discountPercentage.doubleValue())
            );
            
        } catch (Exception e) {
            return predictWithStatistics(product, sales, discountPercentage);
        }
    }
    
    /**
     * Statistical fallback prediction
     */
    private SalesPredictionResult predictWithStatistics(Product product, List<Sale> sales, BigDecimal discountPercentage) {
        List<Sale> promotionSales = sales.stream()
                .filter(Sale::getIsPromotion)
                .collect(Collectors.toList());
        
        List<Sale> regularSales = sales.stream()
                .filter(s -> !s.getIsPromotion())
                .collect(Collectors.toList());
        
        double avgRegularSales = regularSales.stream()
                .mapToInt(Sale::getSalesVolume)
                .average()
                .orElse(0.0);
        
        double avgPromotionSales = promotionSales.isEmpty() ? avgRegularSales * 1.3 :
                promotionSales.stream()
                        .mapToInt(Sale::getSalesVolume)
                        .average()
                        .orElse(avgRegularSales * 1.3);
        
        // Apply discount factor
        double discountFactor = 1 + (discountPercentage.doubleValue() / 100) * 1.5; // 1.5x multiplier
        double predictedSales = avgRegularSales * discountFactor;
        
        double increasePercent = avgRegularSales > 0 ? 
                ((predictedSales - avgRegularSales) / avgRegularSales) * 100 : 30.0;
        
        return new SalesPredictionResult(
                product.getId(),
                Math.max(1, (int) Math.round(predictedSales)),
                BigDecimal.valueOf(increasePercent).setScale(2, RoundingMode.HALF_UP),
                "Statistical prediction based on historical patterns",
                discountPercentage,
                BigDecimal.valueOf(avgRegularSales).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(calculateConfidenceScore(0, sales.size())),
                generateRecommendation(increasePercent, discountPercentage.doubleValue())
        );
    }
    
    /**
     * Prepare training data for ML models
     */
    private Dataset<Row> prepareTrainingData(List<Sale> sales) {
        List<Row> rows = sales.stream().map(sale -> {
            LocalDateTime saleTime = sale.getSaleTime();
            return RowFactory.create(
                    sale.getSalePrice().doubleValue(),
                    sale.getDiscountPercentage().doubleValue(),
                    saleTime.getDayOfYear(),
                    saleTime.getDayOfWeek().getValue() >= 6 ? 1.0 : 0.0, // weekend
                    sale.getSalesVolume()
            );
        }).collect(Collectors.toList());
        
        StructType schema = DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("price", DataTypes.DoubleType, false),
                DataTypes.createStructField("discountPercentage", DataTypes.DoubleType, false),
                DataTypes.createStructField("dayOfYear", DataTypes.IntegerType, false),
                DataTypes.createStructField("isWeekend", DataTypes.DoubleType, false),
                DataTypes.createStructField("salesVolume", DataTypes.IntegerType, false)
        ));
        
        return sparkSession.createDataFrame(rows, schema);
    }
    
    /**
     * Create input for prediction
     */
    private Dataset<Row> createPredictionInput(Product product, BigDecimal discountPercentage) {
        LocalDateTime now = LocalDateTime.now();
        Row row = RowFactory.create(
                product.getPrice().doubleValue(),
                discountPercentage.doubleValue(),
                now.getDayOfYear(),
                now.getDayOfWeek().getValue() >= 6 ? 1.0 : 0.0
        );
        
        StructType schema = DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("price", DataTypes.DoubleType, false),
                DataTypes.createStructField("discountPercentage", DataTypes.DoubleType, false),
                DataTypes.createStructField("dayOfYear", DataTypes.IntegerType, false),
                DataTypes.createStructField("isWeekend", DataTypes.DoubleType, false)
        ));
        
        return sparkSession.createDataFrame(Arrays.asList(row), schema);
    }
    
    /**
     * Batch prediction for multiple products
     */
    public List<SalesPredictionResult> predictSalesIncreaseForMultipleProducts(
            List<Long> productIds, BigDecimal discountPercentage) {
        return productIds.stream()
                .map(productId -> predictSalesIncrease(productId, discountPercentage))
                .collect(Collectors.toList());
    }
    
    /**
     * Brand-level prediction
     */
    public BrandPredictionResult predictSalesIncreaseByBrand(
            String brand, String productType, BigDecimal discountPercentage) {
        
        List<Product> products = productRepository.findByBrandAndProductType(brand, productType);
        if (products.isEmpty()) {
            return new BrandPredictionResult(brand, productType, Collections.emptyList(), 
                    BigDecimal.ZERO, "No products found for this brand/type combination");
        }
        
        List<SalesPredictionResult> productPredictions = products.stream()
                .map(product -> predictSalesIncrease(product.getId(), discountPercentage))
                .collect(Collectors.toList());
        
        double avgIncrease = productPredictions.stream()
                .mapToDouble(p -> p.getIncreasePercentage().doubleValue())
                .average()
                .orElse(0.0);
        
        return new BrandPredictionResult(
                brand, 
                productType, 
                productPredictions,
                BigDecimal.valueOf(avgIncrease).setScale(2, RoundingMode.HALF_UP),
                String.format("Brand-level prediction for %d products", products.size())
        );
    }
    
    /**
     * 9. Generate pricing policy recommendations
     */
    public PricingPolicyRecommendation generatePricingPolicy(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        List<PricingRecommendation> recommendations = new ArrayList<>();
        
        // Test different discount levels
        int[] discountLevels = {5, 10, 15, 20, 25, 30};
        
        for (int discount : discountLevels) {
            SalesPredictionResult prediction = predictSalesIncrease(productId, BigDecimal.valueOf(discount));
            
            double roi = calculateROI(product, discount, prediction.getPredictedSalesVolume());
            String riskLevel = getRiskLevel(discount, roi);
            
            recommendations.add(new PricingRecommendation(
                    discount,
                    prediction.getPredictedSalesVolume(),
                    prediction.getIncreasePercentage(),
                    BigDecimal.valueOf(roi).setScale(2, RoundingMode.HALF_UP),
                    riskLevel,
                    generateRecommendationText(discount, roi, prediction.getIncreasePercentage().doubleValue())
            ));
        }
        
        // Sort by ROI
        recommendations.sort((a, b) -> b.getRoi().compareTo(a.getRoi()));
        
        return new PricingPolicyRecommendation(
                productId,
                product.getProductName(),
                recommendations,
                generateOverallRecommendation(recommendations)
        );
    }
    
    // Helper methods
    private SalesPredictionResult buildDefaultPrediction(Long productId, BigDecimal discountPercentage, String reason) {
        return new SalesPredictionResult(
                productId,
                50, // default prediction
                BigDecimal.valueOf(25.0), // 25% increase assumption
                reason,
                discountPercentage,
                BigDecimal.valueOf(40.0), // assumed baseline
                BigDecimal.valueOf(0.5), // low confidence
                "Consider gathering more historical data for better predictions"
        );
    }
    
    private double calculateROI(Product product, int discountPercent, int predictedSales) {
        if (product.getPrice() == null) return 0.0;
        
        double originalPrice = product.getPrice().doubleValue();
        double discountedPrice = originalPrice * (1 - discountPercent / 100.0);
        double revenue = discountedPrice * predictedSales;
        double cost = originalPrice * 0.6 * predictedSales; // Assume 60% cost ratio
        
        return cost > 0 ? ((revenue - cost) / cost) * 100 : 0;
    }
    
    private double calculateConfidenceScore(double rmse, int dataPoints) {
        if (rmse == 0) { // Statistical method
            return Math.min(0.9, 0.3 + (dataPoints * 0.05));
        } else { // ML method
            return Math.max(0.1, Math.min(0.95, 1.0 - (rmse / 100.0)));
        }
    }
    
    private String getRiskLevel(int discount, double roi) {
        if (discount > 25 || roi < 10) return "HIGH";
        if (discount > 15 || roi < 20) return "MEDIUM";
        return "LOW";
    }
    
    private String generateRecommendation(double increasePercent, double discountPercent) {
        if (increasePercent > 50 && discountPercent <= 20) {
            return "HIGHLY_RECOMMENDED - Excellent sales boost with reasonable discount";
        } else if (increasePercent > 30 && discountPercent <= 25) {
            return "RECOMMENDED - Good sales improvement expected";
        } else if (increasePercent > 15) {
            return "MODERATE - Modest improvement, monitor closely";
        } else {
            return "CAUTION - Limited sales improvement, consider alternatives";
        }
    }
    
    private String generateRecommendationText(int discount, double roi, double salesIncrease) {
        if (roi > 30 && salesIncrease > 40) {
            return "Excellent opportunity - high ROI and strong sales growth";
        } else if (roi > 20 && salesIncrease > 25) {
            return "Good strategy - balanced returns and growth";
        } else if (roi > 10) {
            return "Acceptable approach - positive returns expected";
        } else {
            return "Consider alternatives - limited financial benefit";
        }
    }
    
    private String generateOverallRecommendation(List<PricingRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return "No recommendations available";
        }
        
        PricingRecommendation best = recommendations.get(0);
        return String.format(
                "Recommended discount: %d%% for optimal ROI of %.1f%% and sales increase of %.1f%%",
                best.getDiscountPercentage(),
                best.getRoi().doubleValue(),
                best.getSalesIncreasePercent().doubleValue()
        );
    }
    
    // Data classes
    public static class SalesPredictionResult {
        private Long productId;
        private Integer predictedSalesVolume;
        private BigDecimal increasePercentage;
        private String explanation;
        private BigDecimal discountPercentage;
        private BigDecimal baselineSales;
        private BigDecimal confidenceScore;
        private String recommendation;
        
        public SalesPredictionResult(Long productId, Integer predictedSalesVolume, 
                BigDecimal increasePercentage, String explanation, BigDecimal discountPercentage,
                BigDecimal baselineSales, BigDecimal confidenceScore, String recommendation) {
            this.productId = productId;
            this.predictedSalesVolume = predictedSalesVolume;
            this.increasePercentage = increasePercentage;
            this.explanation = explanation;
            this.discountPercentage = discountPercentage;
            this.baselineSales = baselineSales;
            this.confidenceScore = confidenceScore;
            this.recommendation = recommendation;
        }
        
        // Getters
        public Long getProductId() { return productId; }
        public Integer getPredictedSalesVolume() { return predictedSalesVolume; }
        public BigDecimal getIncreasePercentage() { return increasePercentage; }
        public String getExplanation() { return explanation; }
        public BigDecimal getDiscountPercentage() { return discountPercentage; }
        public BigDecimal getBaselineSales() { return baselineSales; }
        public BigDecimal getConfidenceScore() { return confidenceScore; }
        public String getRecommendation() { return recommendation; }
    }
    
    public static class BrandPredictionResult {
        private String brand;
        private String productType;
        private List<SalesPredictionResult> productPredictions;
        private BigDecimal avgIncreasePercentage;
        private String explanation;
        
        public BrandPredictionResult(String brand, String productType, 
                List<SalesPredictionResult> productPredictions, 
                BigDecimal avgIncreasePercentage, String explanation) {
            this.brand = brand;
            this.productType = productType;
            this.productPredictions = productPredictions;
            this.avgIncreasePercentage = avgIncreasePercentage;
            this.explanation = explanation;
        }
        
        // Getters
        public String getBrand() { return brand; }
        public String getProductType() { return productType; }
        public List<SalesPredictionResult> getProductPredictions() { return productPredictions; }
        public BigDecimal getAvgIncreasePercentage() { return avgIncreasePercentage; }
        public String getExplanation() { return explanation; }
    }
    
    public static class PricingPolicyRecommendation {
        private Long productId;
        private String productName;
        private List<PricingRecommendation> recommendations;
        private String overallRecommendation;
        
        public PricingPolicyRecommendation(Long productId, String productName,
                List<PricingRecommendation> recommendations, String overallRecommendation) {
            this.productId = productId;
            this.productName = productName;
            this.recommendations = recommendations;
            this.overallRecommendation = overallRecommendation;
        }
        
        // Getters
        public Long getProductId() { return productId; }
        public String getProductName() { return productName; }
        public List<PricingRecommendation> getRecommendations() { return recommendations; }
        public String getOverallRecommendation() { return overallRecommendation; }
    }
    
    public static class PricingRecommendation {
        private Integer discountPercentage;
        private Integer predictedSalesVolume;
        private BigDecimal salesIncreasePercent;
        private BigDecimal roi;
        private String riskLevel;
        private String recommendation;
        
        public PricingRecommendation(Integer discountPercentage, Integer predictedSalesVolume,
                BigDecimal salesIncreasePercent, BigDecimal roi, String riskLevel, String recommendation) {
            this.discountPercentage = discountPercentage;
            this.predictedSalesVolume = predictedSalesVolume;
            this.salesIncreasePercent = salesIncreasePercent;
            this.roi = roi;
            this.riskLevel = riskLevel;
            this.recommendation = recommendation;
        }
        
        // Getters
        public Integer getDiscountPercentage() { return discountPercentage; }
        public Integer getPredictedSalesVolume() { return predictedSalesVolume; }
        public BigDecimal getSalesIncreasePercent() { return salesIncreasePercent; }
        public BigDecimal getRoi() { return roi; }
        public String getRiskLevel() { return riskLevel; }
        public String getRecommendation() { return recommendation; }
    }
}
