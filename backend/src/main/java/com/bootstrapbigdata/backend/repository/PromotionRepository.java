package com.bootstrapbigdata.backend.repository;

import com.bootstrapbigdata.backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    List<Promotion> findByProductId(Long productId);
    
    List<Promotion> findByIsActive(Boolean isActive);
    
    @Query("SELECT p FROM Promotion p WHERE :currentDate BETWEEN p.startDate AND p.endDate AND p.isActive = true")
    List<Promotion> findActivePromotions(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT p FROM Promotion p WHERE p.product.id = :productId AND :currentDate BETWEEN p.startDate AND p.endDate AND p.isActive = true")
    List<Promotion> findActivePromotionsByProductId(@Param("productId") Long productId, @Param("currentDate") LocalDateTime currentDate);
    
    @Query("""
        SELECT p.product.brand, p.product.productType,
               COUNT(p.id) as promotionCount,
               AVG(p.discountPercentage) as avgDiscount,
               MIN(p.discountPercentage) as minDiscount,
               MAX(p.discountPercentage) as maxDiscount
        FROM Promotion p
        WHERE p.isActive = true
        GROUP BY p.product.brand, p.product.productType
        ORDER BY AVG(p.discountPercentage) DESC
        """)
    List<Object[]> findPromotionStatsByBrandAndType();
}
