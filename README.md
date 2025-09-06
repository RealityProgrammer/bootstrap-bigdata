# Hệ thống Phân tích Hiệu quả Kinh doanh E-commerce

## Tổng quan

Hệ thống phân tích hiệu quả kinh doanh cho các sản phẩm thương mại điện tử sử dụng **Apache Spark**, **Spring Boot**, **Next.js** và **MySQL** để xử lý và phân tích dữ liệu lớn.

## Tính chất BigData và Phần mềm hướng dịch vụ

### 🔢 Đặc điểm BigData (5V)

1. **Volume**: Xử lý hàng triệu giao dịch từ các nền tảng e-commerce
2. **Velocity**: Streaming data real-time từ Shopee, Lazada, Tiki
3. **Variety**: Dữ liệu structured (sales, products) và unstructured (reviews, comments)
4. **Veracity**: Data validation và cleaning pipeline với Spark
5. **Value**: Business insights cho pricing strategy và growth optimization

### 🏗️ Kiến trúc Microservices

- **Analytics Service**: Spark SQL processing với UDF tùy chỉnh
- **ML Service**: Machine Learning models cho prediction và recommendations
- **Data Service**: ETL pipeline với Apache Spark
- **API Gateway**: Spring Boot REST APIs với rate limiting
- **Frontend Service**: Next.js SPA với real-time analytics dashboard

## Công nghệ sử dụng

### Backend

- **Spring Boot 3.5.5** - REST API và business logic
- **Apache Spark 3.5.0** - Big data processing và analytics
- **Spark SQL** - SQL queries trên large datasets
- **Spark MLlib** - Machine learning models
- **MySQL 8.0** - Relational database
- **Maven** - Dependency management

### Frontend

- **Next.js 15** - React framework với SSR
- **ShadCN UI** - Modern UI components
- **Recharts** - Data visualization
- **Tailwind CSS** - Utility-first CSS
- **TypeScript** - Type safety

### Infrastructure

- **Docker & Docker Compose** - Containerization
- **Apache Spark Cluster** - Distributed computing

## Tính năng chính

### 1. Phân tích dữ liệu với Spark SQL + UDF

- **UDF phân loại sản phẩm khuyến mãi** dựa trên giá ban đầu
- **So sánh sales_volume** giữa hàng giảm giá và không giảm giá
- **Phân tích ảnh hưởng giảm giá** đến rating khách hàng
- **Tính hệ số chuyển đổi** review/doanh số khi có khuyến mãi

### 2. Tìm kiếm insights kinh doanh

- **Ngưỡng giá tối ưu** để đạt doanh số cao
- **Spark SQL tính toán** các chỉ số hiệu quả bán hàng
- **So sánh hiệu quả** theo brand và loại sản phẩm
- **Phân tích cross-platform** performance

### 3. Machine Learning Models

- **Mô hình dự đoán sales** tăng bao nhiêu khi giảm 10%
- **Predictive analytics** cho multiple discount scenarios
- **Recommendation engine** cho pricing strategy
- **Confidence scoring** dựa trên historical data

### 4. Gợi ý chính sách giá thông minh

- **Optimal pricing recommendations** với ROI analysis
- **Risk assessment** cho different discount levels
- **Dynamic pricing strategies** based on demand patterns
- **A/B testing suggestions** cho campaign optimization

### 5. Export & Reporting

- **CSV export** cho bộ phận kinh doanh
- **Real-time dashboards** với interactive charts
- **Automated reports** với scheduling
- **Data visualization** với Recharts

## Cài đặt và Chạy

### Yêu cầu hệ thống

- **Java 17+**
- **Node.js 18+**
- **Docker & Docker Compose**
- **Maven 3.6+**
- **RAM: 8GB+** (recommended for Spark)
- **Disk: 10GB+** free space

### 1. Clone repository

```bash
git clone https://github.com/your-repo/bootstrap-bigdata.git
cd bootstrap-bigdata
```

### 2. Chạy với Docker Compose (Recommended)

```bash
cd docker
docker-compose up -d
```

Services sẽ chạy trên:

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **MySQL**: localhost:3306
- **Spark Master UI**: http://localhost:8090

### 3. Import dữ liệu mẫu

Sử dụng API endpoint để import Excel data:

```bash
curl -X POST \
  -F "file=@data/1.shope_lazada.xlsx" \
  http://localhost:8080/api/data/import/excel
```

## API Endpoints

### Analytics APIs

```bash
# Dự đoán sales increase
POST /api/analytics/predict-sales/{productId}?discountPercentage=10

# Dự đoán batch cho nhiều sản phẩm
POST /api/analytics/predict-sales/batch

# Dự đoán theo brand
POST /api/analytics/predict-sales/brand?brand=Samsung&productType=Smartphone&discountPercentage=20

# Gợi ý pricing policy
GET /api/analytics/pricing-policy/{productId}
```

## Machine Learning Models

### Sales Prediction Algorithm

```java
// Tính hệ số tăng trưởng dựa trên historical data
double salesIncreaseRatio = (avgPromotionSales - avgRegularSales) / avgRegularSales;
double discountImpactFactor = salesIncreaseRatio / avgPromotionDiscount;

// Dự đoán cho discount mới
double predictedIncreaseRatio = discountImpactFactor * discountPercentage;
double predictedSalesVolume = avgRegularSales * (1 + predictedIncreaseRatio);
```

## Performance Optimization

### Spark Configuration

```properties
spark.master=local[*]
spark.sql.adaptive.enabled=true
spark.sql.adaptive.coalescePartitions.enabled=true
spark.driver.memory=4g
spark.executor.memory=4g
```

## Troubleshooting

### Common Issues

1. **OutOfMemory errors**: Increase JVM heap size
2. **Spark connection issues**: Check Spark master URL
3. **Database connection**: Verify MySQL is running
4. **Port conflicts**: Change ports in docker-compose.yml

### Logs Location

```bash
# Application logs
docker logs ecommerce_backend

# Spark logs
docker logs spark-master
docker logs spark-worker

# MySQL logs
docker logs ecommerce_mysql
```

## License

MIT License - see LICENSE file for details
