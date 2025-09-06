# Multi-stage build for Spring Boot backend with Spark support
FROM maven:3.8.3-openjdk-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Install dependencies for Spark/Hadoop on Linux
RUN apt-get update && \
    apt-get install -y curl procps && \
    rm -rf /var/lib/apt/lists/*

# Create directories
RUN mkdir -p /app/data /app/spark-warehouse

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Set environment variables for Spark
ENV SPARK_HOME=/opt/spark
ENV HADOOP_CONF_DIR=/app/hadoop-conf
ENV SPARK_DIST_CLASSPATH=""

# JVM options to handle Spark module access issues
ENV JAVA_OPTS="-Xms512m -Xmx2g --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
