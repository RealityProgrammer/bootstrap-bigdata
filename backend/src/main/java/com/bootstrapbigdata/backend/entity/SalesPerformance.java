package com.bootstrapbigdata.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_performance")
public class SalesPerformance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue;
    
    @Column(name = "total_sales_volume")
    private Integer totalSalesVolume;
    
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;
    
    @Column(name = "review_conversion_rate", precision = 5, scale = 4)
    private BigDecimal reviewConversionRate;
    
    @Column(name = "optimal_price", precision = 15, scale = 2)
    private BigDecimal optimalPrice;
    
    @Column(name = "promotion_impact_score", precision = 5, scale = 2)
    private BigDecimal promotionImpactScore;
    
    @Column(name = "analysis_period_start")
    private LocalDateTime analysisPeriodStart;
    
    @Column(name = "analysis_period_end")
    private LocalDateTime analysisPeriodEnd;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public SalesPerformance() {}
    
    public SalesPerformance(Product product, LocalDateTime analysisPeriodStart, LocalDateTime analysisPeriodEnd) {
        this.product = product;
        this.analysisPeriodStart = analysisPeriodStart;
        this.analysisPeriodEnd = analysisPeriodEnd;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public Integer getTotalSalesVolume() { return totalSalesVolume; }
    public void setTotalSalesVolume(Integer totalSalesVolume) { this.totalSalesVolume = totalSalesVolume; }
    
    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }
    
    public BigDecimal getReviewConversionRate() { return reviewConversionRate; }
    public void setReviewConversionRate(BigDecimal reviewConversionRate) { this.reviewConversionRate = reviewConversionRate; }
    
    public BigDecimal getOptimalPrice() { return optimalPrice; }
    public void setOptimalPrice(BigDecimal optimalPrice) { this.optimalPrice = optimalPrice; }
    
    public BigDecimal getPromotionImpactScore() { return promotionImpactScore; }
    public void setPromotionImpactScore(BigDecimal promotionImpactScore) { this.promotionImpactScore = promotionImpactScore; }
    
    public LocalDateTime getAnalysisPeriodStart() { return analysisPeriodStart; }
    public void setAnalysisPeriodStart(LocalDateTime analysisPeriodStart) { this.analysisPeriodStart = analysisPeriodStart; }
    
    public LocalDateTime getAnalysisPeriodEnd() { return analysisPeriodEnd; }
    public void setAnalysisPeriodEnd(LocalDateTime analysisPeriodEnd) { this.analysisPeriodEnd = analysisPeriodEnd; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
