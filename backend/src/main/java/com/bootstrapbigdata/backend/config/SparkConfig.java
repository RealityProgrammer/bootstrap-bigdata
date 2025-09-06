package com.bootstrapbigdata.backend.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {
    
    @Bean
    public SparkSession sparkSession() {
        // Kết nối đến Spark cluster trong Docker Compose
        String sparkMaster = System.getenv().getOrDefault("SPARK_MASTER", "spark://spark-master:7077");
        String appName = System.getenv().getOrDefault("SPARK_APP_NAME", "ECommerceAnalytics");
        
        return SparkSession.builder()
                .appName(appName)
                .master(sparkMaster)
                
                // Client mode - không khởi tạo Spark UI trong backend
                .config("spark.ui.enabled", "false") // Tắt Spark UI trong backend
                .config("spark.driver.host", "backend")
                .config("spark.driver.bindAddress", "0.0.0.0") 
                .config("spark.driver.port", "7001")
                .config("spark.blockManager.port", "7002")

                // Fix ANTLR and dependency conflicts
                .config("spark.driver.userClassPathFirst", "false")
                .config("spark.executor.userClassPathFirst", "false")
                .config("spark.sql.parser.ansi.enabled", "false")
                
                // Disable web UI conflicts
                .config("spark.ui.showConsoleProgress", "false")
                .config("spark.sql.warehouse.dir", "/app/spark-warehouse")
                
                // Performance optimization
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .config("spark.sql.adaptive.enabled", "true")
                .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .config("spark.sql.adaptive.skewJoin.enabled", "true")
                
                // Memory settings cho cluster mode
                .config("spark.driver.memory", "512m")
                .config("spark.driver.maxResultSize", "512m")
                .config("spark.executor.memory", "1g")
                .config("spark.executor.cores", "1")
                
                // JDBC configuration
                .config("spark.sql.streaming.checkpointLocation", "/app/spark-checkpoints")
                
                // Java compatibility - critical for Java 17
                .config("spark.driver.extraJavaOptions", 
                    "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED " +
                    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED " +
                    "--add-opens=java.base/java.lang=ALL-UNNAMED " +
                    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED " +
                    "--add-opens=java.base/java.io=ALL-UNNAMED " +
                    "--add-opens=java.base/java.net=ALL-UNNAMED " +
                    "--add-opens=java.base/java.nio=ALL-UNNAMED " +
                    "--add-opens=java.base/java.util=ALL-UNNAMED " +
                    "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED " +
                    "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED " +
                    "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED " +
                    "--add-opens=java.base/sun.security.action=ALL-UNNAMED " +
                    "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED " +
                    "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED " +
                    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
                .config("spark.executor.extraJavaOptions",
                    "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED " +
                    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED " +
                    "--add-opens=java.base/java.lang=ALL-UNNAMED " +
                    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED " +
                    "--add-opens=java.base/java.io=ALL-UNNAMED " +
                    "--add-opens=java.base/java.net=ALL-UNNAMED " +
                    "--add-opens=java.base/java.nio=ALL-UNNAMED " +
                    "--add-opens=java.base/java.util=ALL-UNNAMED " +
                    "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED " +
                    "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED " +
                    "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED " +
                    "--add-opens=java.base/sun.security.action=ALL-UNNAMED " +
                    "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED " +
                    "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED " +
                    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
                
                // Submit dependency conflicts resolution
                .config("spark.driver.userClassPathFirst", "true")
                .config("spark.executor.userClassPathFirst", "true")
                
                .getOrCreate();
    }
}
