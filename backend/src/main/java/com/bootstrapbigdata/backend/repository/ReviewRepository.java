package com.bootstrapbigdata.backend.repository;

import com.bootstrapbigdata.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByProductId(Long productId);
    
    List<Review> findByRatingGreaterThanEqual(BigDecimal rating);
    
    List<Review> findByReviewDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT r FROM Review r WHERE r.product.brand = :brand")
    List<Review> findByProductBrand(@Param("brand") String brand);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    BigDecimal findAverageRatingByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long countReviewsByProductId(@Param("productId") Long productId);
    
    @Query("""
        SELECT p.brand, p.productType,
               COUNT(r.id) as reviewCount,
               AVG(r.rating) as avgRating,
               MIN(r.rating) as minRating,
               MAX(r.rating) as maxRating
        FROM Review r JOIN r.product p
        GROUP BY p.brand, p.productType
        ORDER BY AVG(r.rating) DESC
        """)
    List<Object[]> findReviewStatsByBrandAndType();
}
