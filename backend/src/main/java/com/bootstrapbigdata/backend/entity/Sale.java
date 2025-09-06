package com.bootstrapbigdata.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "sales_volume")
    private Integer salesVolume;
    
    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice;
    
    @Column(name = "sale_time")
    private LocalDateTime saleTime;
    
    @Column(name = "platform")
    private String platform;
    
    @Column(name = "is_promotion")
    private Boolean isPromotion = false;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public Sale() {}
    
    public Sale(Product product, Integer salesVolume, BigDecimal salePrice, LocalDateTime saleTime, String platform) {
        this.product = product;
        this.salesVolume = salesVolume;
        this.salePrice = salePrice;
        this.saleTime = saleTime;
        this.platform = platform;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public Integer getSalesVolume() { return salesVolume; }
    public void setSalesVolume(Integer salesVolume) { this.salesVolume = salesVolume; }
    
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    
    public LocalDateTime getSaleTime() { return saleTime; }
    public void setSaleTime(LocalDateTime saleTime) { this.saleTime = saleTime; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public Boolean getIsPromotion() { return isPromotion; }
    public void setIsPromotion(Boolean isPromotion) { this.isPromotion = isPromotion; }
    
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Helper method to calculate revenue
    public BigDecimal getRevenue() {
        return salePrice.multiply(BigDecimal.valueOf(salesVolume));
    }
}
