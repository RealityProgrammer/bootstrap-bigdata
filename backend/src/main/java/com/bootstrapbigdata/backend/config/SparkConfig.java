package com.bootstrapbigdata.backend.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {

    @Autowired
    private AppConfig appConfig;

    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName("BigDataAnalysis")
                .master(appConfig.getSparkMaster())
                .config("spark.driver.host", appConfig.getSpark().getDriverHost())
                .config("spark.driver.bindAddress", appConfig.getSpark().getDriverHost())
                .config("spark.driver.memory", appConfig.getSpark().getDriverMemory())
                .config("spark.executor.memory", appConfig.getSpark().getExecutorMemory())
                .config("spark.executor.instances", String.valueOf(appConfig.getSpark().getExecutorInstances()))
                .config("spark.sql.adaptive.enabled", "true")
                .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                // tắt UI khi chạy embedded để tránh xung đột servlet
                .config("spark.ui.enabled", "false")
                // nếu vẫn cần UI bên ngoài, chạy Spark master/worker riêng (Docker/cluster)
                .getOrCreate();
    }
}