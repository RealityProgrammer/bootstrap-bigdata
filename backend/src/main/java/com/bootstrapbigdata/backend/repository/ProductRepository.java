package com.bootstrapbigdata.backend.repository;

import com.bootstrapbigdata.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByBrand(String brand);
    
    List<Product> findByProductType(String productType);
    
    List<Product> findByPlatform(String platform);
    
    @Query("SELECT p FROM Product p WHERE p.brand = :brand AND p.productType = :productType")
    List<Product> findByBrandAndProductType(@Param("brand") String brand, @Param("productType") String productType);
    
    @Query("SELECT DISTINCT p.brand FROM Product p ORDER BY p.brand")
    List<String> findDistinctBrands();
    
    @Query("SELECT DISTINCT p.productType FROM Product p ORDER BY p.productType")
    List<String> findDistinctProductTypes();
    
    @Query("SELECT DISTINCT p.platform FROM Product p ORDER BY p.platform")
    List<String> findDistinctPlatforms();
}
