package com.bootstrapbigdata.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.monotonically_increasing_id;
import static org.apache.spark.sql.functions.rand;
import static org.apache.spark.sql.functions.sum;
import static org.apache.spark.sql.functions.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bootstrapbigdata.backend.config.AppConfig;

@Service
public class PredictionService {

    @Autowired
    private SparkSession spark;
    
    @Autowired
    private AppConfig appConfig;

    public Map<String, Object> predictSalesWithDiscount(String datasetPath, double discountRate) {
        try {
            // Read and prepare dataset
            Dataset<Row> df = readAndPrepareData(datasetPath);
            
            // Train model
            PipelineModel model = trainModel(df);
            
            // Create scenario with new discount rate
            Dataset<Row> scenarioData = createDiscountScenario(df, discountRate);
            
            // Make predictions
            Dataset<Row> predictions = model.transform(scenarioData);
            
            // Calculate results
            Map<String, Object> results = calculatePredictionResults(df, predictions, discountRate);
            
            // Save scenario results
            String csvPath = saveScenarioResults(predictions, discountRate);
            results.put("csv_path", csvPath);
            
            return results;
            
        } catch (Exception e) {
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    private Dataset<Row> readAndPrepareData(String path) {
        String fullPath = path.startsWith("hdfs://") ? path : appConfig.getHdfsUri() + path;
        
        Dataset<Row> df = spark.read()
                .option("header", "true")
                .option("inferSchema", "true")
                .option("sep", ";")  // CSV sử dụng dấu chấm phẩy
                .csv(fullPath);

        // Chuẩn hóa và tạo cột cần thiết
        return df.withColumnRenamed("product_type", "category")
                .withColumnRenamed("reviews", "review_count")
                .withColumn("product_id", monotonically_increasing_id())
                // Fake original_price: Tăng ngẫu nhiên 15-40%
                .withColumn("original_price", 
                    col("price").multiply(lit(1.15).plus(rand().multiply(lit(0.25)))))
                .withColumn("promo_price", col("price"))
                .withColumn("discount_rate", 
                    (col("original_price").minus(col("promo_price"))).divide(col("original_price")))
                // Encode categorical variables
                .withColumn("platform_encoded", 
                    when(col("platform").equalTo("Shopee"), 1.0).otherwise(0.0))
                .withColumn("season_encoded",
                    when(col("sale_time").isin("Tháng 12", "Tháng 1", "Tháng 2"), 1.0)
                    .when(col("sale_time").isin("Tháng 3", "Tháng 4", "Tháng 5"), 2.0)
                    .when(col("sale_time").isin("Tháng 6", "Tháng 7", "Tháng 8"), 3.0)
                    .otherwise(4.0))
                .filter(col("sales_volume").isNotNull()
                    .and(col("price").isNotNull())
                    .and(col("brand").isNotNull())
                    .and(col("category").isNotNull())
                    .and(col("rating").isNotNull())
                    .and(col("review_count").isNotNull()));
    }

    private PipelineModel trainModel(Dataset<Row> df) {
        // Create string indexers cho categorical variables
        StringIndexer brandIndexer = new StringIndexer()
                .setInputCol("brand")
                .setOutputCol("brand_indexed")
                .setHandleInvalid("keep");

        StringIndexer categoryIndexer = new StringIndexer()
                .setInputCol("category")
                .setOutputCol("category_indexed")
                .setHandleInvalid("keep");

        // Feature vector với các cột thực tế
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[]{
                    "original_price", "promo_price", "discount_rate", 
                    "rating", "review_count", "platform_encoded", 
                    "season_encoded", "brand_indexed", "category_indexed"
                })
                .setOutputCol("features")
                .setHandleInvalid("keep");

        LinearRegression lr = new LinearRegression()
                .setFeaturesCol("features")
                .setLabelCol("sales_volume")
                .setPredictionCol("predicted_sales")
                .setRegParam(0.01)  // Regularization
                .setElasticNetParam(0.1);

        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[]{brandIndexer, categoryIndexer, assembler, lr});

        return pipeline.fit(df);
    }

    private Dataset<Row> createDiscountScenario(Dataset<Row> originalData, double discountRate) {
        return originalData
                .withColumn("scenario_promo_price", 
                    col("original_price").multiply(1 - discountRate))
                .withColumn("promo_price", col("scenario_promo_price"))  // Cập nhật giá mới
                .withColumn("discount_rate", lit(discountRate))
                .withColumn("baseline_sales", col("sales_volume"));
    }

    private Map<String, Object> calculatePredictionResults(Dataset<Row> original, Dataset<Row> predictions, double discountRate) {
        // Calculate aggregated metrics
        Row originalMetrics = original.agg(
                avg("sales_volume").alias("baseline_avg_sales"),
                sum("sales_volume").alias("baseline_total_sales"),
                sum(col("promo_price").multiply(col("sales_volume"))).alias("baseline_revenue")
        ).first();

        Row predictedMetrics = predictions.agg(
                avg("predicted_sales").alias("predicted_avg_sales"),
                sum("predicted_sales").alias("predicted_total_sales"),
                sum(col("scenario_promo_price").multiply(col("predicted_sales"))).alias("predicted_revenue")
        ).first();

        double baselineAvgSales = originalMetrics.getDouble(0);
        double baselineTotalSales = originalMetrics.getDouble(1);
        double baselineRevenue = originalMetrics.getDouble(2);

        double predictedAvgSales = predictedMetrics.getDouble(0);
        double predictedTotalSales = predictedMetrics.getDouble(1);
        double predictedRevenue = predictedMetrics.getDouble(2);

        Map<String, Object> results = new HashMap<>();
        results.put("discount_rate", discountRate);
        results.put("baseline_avg_sales", baselineAvgSales);
        results.put("predicted_avg_sales", predictedAvgSales);
        results.put("sales_delta", predictedAvgSales - baselineAvgSales);
        results.put("sales_delta_percent", ((predictedAvgSales - baselineAvgSales) / baselineAvgSales) * 100);
        results.put("baseline_total_sales", baselineTotalSales);
        results.put("predicted_total_sales", predictedTotalSales);
        results.put("baseline_revenue", baselineRevenue);
        results.put("predicted_revenue", predictedRevenue);
        results.put("revenue_delta", predictedRevenue - baselineRevenue);
        results.put("revenue_delta_percent", ((predictedRevenue - baselineRevenue) / baselineRevenue) * 100);

        return results;
    }

    private String saveScenarioResults(Dataset<Row> predictions, double discountRate) {
        String fileName = String.format("discount_%.0f", discountRate * 100);
        String outputPath = appConfig.getHdfsUri() + appConfig.getPaths().getOutputDir() + "/predict/" + fileName;
        
        predictions.select("product_id", "brand", "category", "price", "scenario_promo_price", 
                          "baseline_sales", "predicted_sales", "scenario_discount_rate")
                .coalesce(1)
                .write()
                .mode(SaveMode.Overwrite)
                .option("header", "true")
                .csv(outputPath);
        
        return outputPath;
    }
}
