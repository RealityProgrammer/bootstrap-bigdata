package com.bootstrapbigdata.backend.service;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.spark.sql.functions.*;

@Service
public class SparkDataProcessingService {
    
    @Autowired
    private SparkSession sparkSession;
    
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    
    @Value("${spring.datasource.username}")
    private String databaseUsername;
    
    @Value("${spring.datasource.password}")
    private String databasePassword;
    
    private static final String SHARED_DATA_PATH = "/data/uploads/";
    
    /**
     * Process Excel file using Spark cluster
     */
    public Map<String, Object> processExcelFileWithSpark(MultipartFile file) throws IOException {
        // 1. Upload file to shared volume accessible by Spark cluster
        String fileName = uploadFileToSharedVolume(file);
        String filePath = SHARED_DATA_PATH + fileName;
        
        try {
            // 2. Read Excel file using Spark
            Dataset<Row> rawData = readExcelWithSpark(filePath);
            
            // 3. Process and transform data in Spark cluster
            Dataset<Row> processedData = transformRawData(rawData);
            
            // 4. Write directly to MySQL using Spark JDBC
            writeDataToMySQL(processedData);
            
            // 5. Get processing statistics
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalRows", rawData.count());
            result.put("processedRows", processedData.count());
            result.put("message", "Data processed successfully by Spark cluster");
            
            return result;
            
        } finally {
            // Clean up uploaded file
            cleanupFile(filePath);
        }
    }
    
    /**
     * Upload file to shared volume accessible by Spark cluster
     */
    private String uploadFileToSharedVolume(MultipartFile file) throws IOException {
        // Create shared directory if not exists
        Path sharedDir = Path.of(SHARED_DATA_PATH);
        Files.createDirectories(sharedDir);
        
        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path targetPath = sharedDir.resolve(fileName);
        
        // Copy file to shared volume
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        return fileName;
    }
    
    /**
     * Read Excel file using Spark with custom schema
     */
    private Dataset<Row> readExcelWithSpark(String filePath) {
        // Define schema for Excel data
        StructType schema = DataTypes.createStructType(new org.apache.spark.sql.types.StructField[]{
            DataTypes.createStructField("product_name", DataTypes.StringType, false),
            DataTypes.createStructField("product_type", DataTypes.StringType, false), 
            DataTypes.createStructField("sales_volume", DataTypes.IntegerType, true),
            DataTypes.createStructField("price", DataTypes.DoubleType, false),
            DataTypes.createStructField("sale_time", DataTypes.StringType, true),
            DataTypes.createStructField("date", DataTypes.StringType, true),
            DataTypes.createStructField("platform", DataTypes.StringType, true),
            DataTypes.createStructField("brand", DataTypes.StringType, true),
            DataTypes.createStructField("reviews", DataTypes.IntegerType, true),
            DataTypes.createStructField("rating", DataTypes.DoubleType, true)
        });
        
        // Read Excel file - using com.crealytics.spark.excel format
        return sparkSession.read()
                .format("com.crealytics.spark.excel")
                .option("header", "true")
                .option("inferSchema", "true")
                .option("addColorColumns", "false")
                .option("timestampFormat", "dd/MM/yyyy")
                .schema(schema)
                .load(filePath);
    }
    
    /**
     * Transform raw data using Spark SQL and functions
     */
    private Dataset<Row> transformRawData(Dataset<Row> rawData) {
        // Register temporary view for SQL operations
        rawData.createOrReplaceTempView("raw_data");
        
        // Clean and transform data using Spark SQL
        Dataset<Row> cleanedData = sparkSession.sql("""
            SELECT 
                TRIM(product_name) as product_name,
                TRIM(COALESCE(product_type, 'Unknown')) as product_type,
                COALESCE(sales_volume, 1) as sales_volume,
                CASE 
                    WHEN price IS NULL OR price <= 0 THEN 100.0
                    ELSE price 
                END as price,
                TRIM(COALESCE(platform, 'Unknown')) as platform,
                TRIM(COALESCE(brand, 'Unknown')) as brand,
                COALESCE(reviews, 0) as reviews,
                CASE 
                    WHEN rating IS NULL OR rating < 1 OR rating > 5 THEN 3.5
                    ELSE rating 
                END as rating,
                COALESCE(sale_time, 'Unknown') as sale_time,
                COALESCE(date, CURRENT_DATE()) as sale_date,
                -- Generate original price (5-25% higher than current price)
                price * (1.05 + RAND() * 0.20) as original_price,
                -- Current timestamp
                CURRENT_TIMESTAMP() as processed_at
            FROM raw_data
            WHERE product_name IS NOT NULL 
                AND TRIM(product_name) != ''
                AND price > 0
        """);
        
        // Add calculated columns
        Dataset<Row> enrichedData = cleanedData
                .withColumn("is_promotion", 
                    when(col("price").lt(col("original_price")), true)
                    .otherwise(false))
                .withColumn("discount_percentage",
                    when(col("price").lt(col("original_price")),
                        ((col("original_price").minus(col("price")))
                         .divide(col("original_price"))).multiply(100))
                    .otherwise(0.0))
                .withColumn("revenue", 
                    col("price").multiply(col("sales_volume")));
        
        return enrichedData;
    }
    
    /**
     * Write processed data directly to MySQL using Spark JDBC
     */
    private void writeDataToMySQL(Dataset<Row> processedData) {
        // JDBC properties
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", databaseUsername);
        connectionProperties.setProperty("password", databasePassword);
        connectionProperties.setProperty("driver", "com.mysql.cj.jdbc.Driver");
        connectionProperties.setProperty("useSSL", "false");
        connectionProperties.setProperty("allowPublicKeyRetrieval", "true");
        
        // Extract JDBC URL without jdbc: prefix for Spark
        String jdbcUrl = databaseUrl;
        
        try {
            // Write products data
            Dataset<Row> productsData = processedData
                    .select("product_name", "product_type", "brand", "platform", "price", "original_price")
                    .dropDuplicates("product_name", "brand", "product_type");
            
            productsData.write()
                    .mode(SaveMode.Append)
                    .option("createTableOptions", "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
                    .jdbc(jdbcUrl, "products", connectionProperties);
            
            // Write sales data - this requires joining with products to get IDs
            // For simplicity, we'll use a stored procedure or handle ID mapping
            writeSalesData(processedData, jdbcUrl, connectionProperties);
            
            // Write reviews data
            writeReviewsData(processedData, jdbcUrl, connectionProperties);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to write data to MySQL via Spark: " + e.getMessage(), e);
        }
    }
    
    /**
     * Write sales data with proper product ID mapping
     */
    private void writeSalesData(Dataset<Row> processedData, String jdbcUrl, Properties props) {
        // Create a temporary view with sales data
        processedData.createOrReplaceTempView("processed_sales");
        
        // Read existing products to get ID mapping
        Dataset<Row> existingProducts = sparkSession.read()
                .jdbc(jdbcUrl, "products", props)
                .select("id", "product_name", "brand", "product_type");
        
        existingProducts.createOrReplaceTempView("existing_products");
        
        // Join and prepare sales data
        Dataset<Row> salesData = sparkSession.sql("""
            SELECT 
                p.id as product_id,
                s.sales_volume,
                s.price as sale_price,
                s.platform,
                s.is_promotion,
                s.discount_percentage,
                CURRENT_TIMESTAMP() as sale_time
            FROM processed_sales s
            INNER JOIN existing_products p 
                ON s.product_name = p.product_name 
                AND s.brand = p.brand 
                AND s.product_type = p.product_type
        """);
        
        // Write to sales table
        salesData.write()
                .mode(SaveMode.Append)
                .jdbc(jdbcUrl, "sales", props);
    }
    
    /**
     * Write reviews data with product ID mapping
     */
    private void writeReviewsData(Dataset<Row> processedData, String jdbcUrl, Properties props) {
        // Similar to sales, but for reviews
        Dataset<Row> existingProducts = sparkSession.read()
                .jdbc(jdbcUrl, "products", props)
                .select("id", "product_name", "brand", "product_type");
        
        existingProducts.createOrReplaceTempView("products_for_reviews");
        
        Dataset<Row> reviewsData = sparkSession.sql("""
            SELECT 
                p.id as product_id,
                s.rating,
                CASE 
                    WHEN s.rating >= 4.0 THEN 'Sản phẩm tuyệt vời, rất hài lòng!'
                    WHEN s.rating >= 3.0 THEN 'Sản phẩm bình thường'  
                    ELSE 'Sản phẩm không như mong đợi'
                END as comment,
                s.platform,
                CURRENT_TIMESTAMP() as review_date,
                CONCAT('CUSTOMER_', SUBSTR(MD5(RAND()), 1, 8)) as customer_id
            FROM processed_sales s
            INNER JOIN products_for_reviews p 
                ON s.product_name = p.product_name 
                AND s.brand = p.brand 
                AND s.product_type = p.product_type
            WHERE s.reviews > 0 AND s.rating IS NOT NULL
        """);
        
        reviewsData.write()
                .mode(SaveMode.Append)
                .jdbc(jdbcUrl, "reviews", props);
    }
    
    /**
     * Clean up temporary files
     */
    private void cleanupFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            // Log warning but don't fail the operation
            System.err.println("Warning: Failed to cleanup file " + filePath + ": " + e.getMessage());
        }
    }
    
    /**
     * Get processing statistics from Spark cluster
     */
    public Map<String, Object> getProcessingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Read data statistics directly from MySQL using Spark
            Properties props = new Properties();
            props.setProperty("user", databaseUsername);
            props.setProperty("password", databasePassword);
            props.setProperty("driver", "com.mysql.cj.jdbc.Driver");
            
            Dataset<Row> productsCount = sparkSession.read()
                    .jdbc(databaseUrl, "products", props)
                    .agg(count("*").alias("total_products"));
            
            Dataset<Row> salesCount = sparkSession.read()
                    .jdbc(databaseUrl, "sales", props)
                    .agg(count("*").alias("total_sales"),
                         sum("sales_volume").alias("total_volume"),
                         sum(col("sale_price").multiply(col("sales_volume"))).alias("total_revenue"));
            
            Dataset<Row> reviewsCount = sparkSession.read()
                    .jdbc(databaseUrl, "reviews", props)
                    .agg(count("*").alias("total_reviews"),
                         avg("rating").alias("avg_rating"));
            
            // Collect results
            stats.put("totalProducts", productsCount.collectAsList().get(0).getLong(0));
            Row salesRow = salesCount.collectAsList().get(0);
            stats.put("totalSales", salesRow.getLong(0));
            stats.put("totalVolume", salesRow.get(1));
            stats.put("totalRevenue", salesRow.get(2));
            
            Row reviewsRow = reviewsCount.collectAsList().get(0);
            stats.put("totalReviews", reviewsRow.getLong(0));
            stats.put("averageRating", reviewsRow.get(1));
            
        } catch (Exception e) {
            stats.put("error", "Failed to get statistics: " + e.getMessage());
        }
        
        return stats;
    }
}
