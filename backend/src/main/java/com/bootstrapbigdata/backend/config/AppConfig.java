package com.bootstrapbigdata.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String hdfsUri;
    private String sparkMaster;
    private Spark spark = new Spark();
    private Paths paths = new Paths();

    // Getters and setters
    public String getHdfsUri() { return hdfsUri; }
    public void setHdfsUri(String hdfsUri) { this.hdfsUri = hdfsUri; }

    public String getSparkMaster() { return sparkMaster; }
    public void setSparkMaster(String sparkMaster) { this.sparkMaster = sparkMaster; }

    public Spark getSpark() { return spark; }
    public void setSpark(Spark spark) { this.spark = spark; }

    public Paths getPaths() { return paths; }
    public void setPaths(Paths paths) { this.paths = paths; }

    public static class Spark {
        private String driverHost;
        private String driverMemory;
        private String executorMemory;
        private int executorInstances;

        // Getters and setters
        public String getDriverHost() { return driverHost; }
        public void setDriverHost(String driverHost) { this.driverHost = driverHost; }

        public String getDriverMemory() { return driverMemory; }
        public void setDriverMemory(String driverMemory) { this.driverMemory = driverMemory; }

        public String getExecutorMemory() { return executorMemory; }
        public void setExecutorMemory(String executorMemory) { this.executorMemory = executorMemory; }

        public int getExecutorInstances() { return executorInstances; }
        public void setExecutorInstances(int executorInstances) { this.executorInstances = executorInstances; }
    }

    public static class Paths {
        private String inputDir;
        private String outputDir;

        // Getters and setters
        public String getInputDir() { return inputDir; }
        public void setInputDir(String inputDir) { this.inputDir = inputDir; }

        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
    }
}
