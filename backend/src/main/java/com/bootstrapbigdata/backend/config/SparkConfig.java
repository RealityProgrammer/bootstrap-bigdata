package com.bootstrapbigdata.backend.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {
    
    @Bean
    public SparkSession sparkSession() {
        // Get Spark master URL from environment (defaults to local for development)
        String sparkMaster = System.getProperty("spark.master", 
                             System.getenv("SPARK_MASTER"));
        if (sparkMaster == null || sparkMaster.isBlank()) {
            sparkMaster = "local[*]";
        }

        // Configure Spark builder with sensible settings:
        SparkSession.Builder builder = SparkSession.builder()
                .appName(System.getProperty("spark.app.name", System.getenv().getOrDefault("SPARK_APP_NAME", "ECommerceAnalytics")))
                .master(sparkMaster)
                .config("spark.sql.warehouse.dir", 
                        System.getProperty("spark.sql.warehouse.dir", 
                        System.getenv().getOrDefault("SPARK_SQL_WAREHOUSE_DIR", "./spark-warehouse")))
                .config("spark.local.dir", System.getProperty("java.io.tmpdir"))
                .config("spark.driver.bindAddress", "0.0.0.0")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .config("spark.sql.adaptive.enabled", "true")
                .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .config("spark.sql.adaptive.skewJoin.enabled", "true")
                .config("spark.driver.memory", System.getenv().getOrDefault("SPARK_DRIVER_MEMORY", "2g"))
                .config("spark.executor.memory", System.getenv().getOrDefault("SPARK_EXECUTOR_MEMORY", "2g"))
                // Always disable UI in backend - UI runs on spark-master container
                .config("spark.ui.enabled", "false");

        // Add Java options for compatibility
        builder.config("spark.driver.extraJavaOptions",
                        "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
                .config("spark.executor.extraJavaOptions",
                        "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED");

        return builder.getOrCreate();
    }
}
