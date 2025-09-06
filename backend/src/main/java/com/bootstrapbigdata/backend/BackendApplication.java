package com.bootstrapbigdata.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        // Docker environment configurations
        String sparkMaster = System.getenv().getOrDefault("SPARK_MASTER", "local[*]");
        String sparkAppName = System.getenv().getOrDefault("SPARK_APP_NAME", "ECommerceAnalytics");
        
        // Set system properties for Spark in Docker
        System.setProperty("spark.master", sparkMaster);
        System.setProperty("spark.app.name", sparkAppName);
        System.setProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        System.setProperty("spark.sql.adaptive.enabled", "true");
        System.setProperty("spark.sql.adaptive.coalescePartitions.enabled", "true");
        System.setProperty("spark.driver.memory", System.getenv().getOrDefault("SPARK_DRIVER_MEMORY", "2g"));
        System.setProperty("spark.executor.memory", System.getenv().getOrDefault("SPARK_EXECUTOR_MEMORY", "2g"));
        
        // Fix Hadoop issues in Docker
        System.setProperty("hadoop.home.dir", System.getenv().getOrDefault("HADOOP_HOME", "/opt/hadoop"));
        System.setProperty("spark.hadoop.fs.defaultFS", "file:///");
        System.setProperty("spark.sql.warehouse.dir", System.getenv().getOrDefault("SPARK_SQL_WAREHOUSE_DIR", "/app/spark-warehouse"));
        
        // Disable native Hadoop library warnings
        System.setProperty("java.library.path", "");
        
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("http://localhost:*", "http://frontend:*", "http://127.0.0.1:*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}