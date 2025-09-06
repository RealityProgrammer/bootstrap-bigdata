package com.bootstrapbigdata.backend.repository;

import com.bootstrapbigdata.backend.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    List<Sale> findByProductId(Long productId);
    
    List<Sale> findByIsPromotion(Boolean isPromotion);
    
    List<Sale> findBySaleTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT s FROM Sale s WHERE s.product.brand = :brand")
    List<Sale> findByProductBrand(@Param("brand") String brand);
    
    @Query("SELECT s FROM Sale s WHERE s.product.productType = :productType")
    List<Sale> findByProductType(@Param("productType") String productType);
    
    @Query("""
        SELECT s.id as sale_id, p.id as product_id, p.productName as product_name, 
               p.brand, p.productType as product_type, p.platform,
               s.salesVolume as sales_volume, s.salePrice as sale_price,
               s.isPromotion as is_promotion, s.discountPercentage as discount_percentage,
               s.saleTime as sale_time, r.id as review_id, r.rating, r.reviewDate as review_date
        FROM Sale s 
        LEFT JOIN s.product p 
        LEFT JOIN Review r ON r.product.id = p.id
        """)
    List<Object[]> findCombinedSalesData();
    
    @Query("""
        SELECT p.brand, p.productType, 
               SUM(s.salesVolume) as totalSales,
               SUM(s.salePrice * s.salesVolume) as totalRevenue,
               AVG(s.salePrice) as avgPrice,
               COUNT(DISTINCT s.product.id) as productCount
        FROM Sale s JOIN s.product p
        GROUP BY p.brand, p.productType
        ORDER BY SUM(s.salePrice * s.salesVolume) DESC
        """)
    List<Object[]> findSalesSummaryByBrandAndType();
    
    @Query("""
        SELECT s.isPromotion,
               COUNT(s.id) as transactionCount,
               SUM(s.salesVolume) as totalVolume,
               AVG(s.salesVolume) as avgVolume,
               SUM(s.salePrice * s.salesVolume) as totalRevenue
        FROM Sale s
        GROUP BY s.isPromotion
        """)
    List<Object[]> findPromotionVsNonPromotionStats();
}
