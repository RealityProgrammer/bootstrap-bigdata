package com.bootstrapbigdata.backend.controller;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bootstrapbigdata.backend.service.MachineLearningService;
import com.bootstrapbigdata.backend.service.MachineLearningService.BrandPredictionResult;
import com.bootstrapbigdata.backend.service.MachineLearningService.PricingPolicyRecommendation;
import com.bootstrapbigdata.backend.service.MachineLearningService.SalesPredictionResult;
import com.bootstrapbigdata.backend.service.SparkAnalyticsService;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class AnalyticsController {
    
    @Autowired
    private MachineLearningService mlService;

    @Autowired
    private SparkAnalyticsService sparkAnalyticsService;
    
    /**
     * Helper method to convert Dataset<Row> to List<Map<String, Object>>
     */
    private List<Map<String, Object>> rowsToList(Dataset<Row> dataset) {
        if (dataset == null) return List.of();
        
        List<Row> rows = dataset.collectAsList();
        String[] columns = dataset.columns();
        
        return rows.stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String column : columns) {
                Object value = row.getAs(column);
                map.put(column, value);
            }
            return map;
        }).collect(Collectors.toList());
    }
    
    /**
     * Initialize Spark UDFs - call this before using analytics
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> initializeAnalytics() {
        try {
            sparkAnalyticsService.initializeUDFs();
            return ResponseEntity.ok(Map.of("status", "success", "message", "Analytics initialized"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Failed to initialize: " + e.getMessage()));
        }
    }
    
    /**
     * 8. Dự đoán sales increase cho một sản phẩm
     */
    @PostMapping("/predict-sales/{productId}")
    public ResponseEntity<SalesPredictionResult> predictSalesIncrease(
            @PathVariable Long productId,
            @RequestParam BigDecimal discountPercentage) {
        try {
            SalesPredictionResult result = mlService.predictSalesIncrease(productId, discountPercentage);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Dự đoán sales increase cho nhiều sản phẩm
     */
    @PostMapping("/predict-sales/batch")
    public ResponseEntity<List<SalesPredictionResult>> predictSalesIncreaseForMultipleProducts(
            @RequestBody PredictionRequest request) {
        try {
            List<SalesPredictionResult> results = mlService.predictSalesIncreaseForMultipleProducts(
                    request.getProductIds(), request.getDiscountPercentage());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Dự đoán theo brand
     */
    @PostMapping("/predict-sales/brand")
    public ResponseEntity<BrandPredictionResult> predictSalesByBrand(
            @RequestParam String brand,
            @RequestParam String productType,
            @RequestParam BigDecimal discountPercentage) {
        try {
            BrandPredictionResult result = mlService.predictSalesIncreaseByBrand(brand, productType, discountPercentage);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 9. Gợi ý chính sách giá
     */
    @GetMapping("/pricing-policy/{productId}")
    public ResponseEntity<PricingPolicyRecommendation> getPricingPolicy(@PathVariable Long productId) {
        try {
            PricingPolicyRecommendation policy = mlService.generatePricingPolicy(productId);
            return ResponseEntity.ok(policy);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 2. So sánh sales giữa hàng khuyến mãi và không khuyến mãi
     */
    @GetMapping("/spark/compare-promotion")
    public ResponseEntity<List<Map<String, Object>>> comparePromotionVsNonPromotion() {
        try {
            List<Map<String, Object>> result = sparkAnalyticsService.getPromotionEffectivenessAnalysis();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Failed to analyze promotion effectiveness: " + e.getMessage())));
        }
    }
    
    /**
     * 4. Tỷ lệ chuyển đổi review->sales
     */
    @GetMapping("/spark/review-conversion")
    public ResponseEntity<List<Map<String, Object>>> getReviewConversionRate() {
        try {
            Dataset<Row> result = sparkAnalyticsService.calculateReviewConversionRate();
            List<Map<String, Object>> jsonResult = rowsToList(result);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Failed to calculate review conversion rate: " + e.getMessage())));
        }
    }
    
    /**
     * 5. Ngưỡng giá tối ưu
     */
    @GetMapping("/spark/optimal-price")
    public ResponseEntity<List<Map<String, Object>>> getOptimalPriceThreshold() {
        try {
            Dataset<Row> result = sparkAnalyticsService.findOptimalPriceThreshold();
            List<Map<String, Object>> jsonResult = rowsToList(result);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Failed to find optimal price threshold: " + e.getMessage())));
        }
    }
    
    /**
     * 6. Các chỉ số hiệu quả bán hàng
     */
    @GetMapping("/spark/sales-efficiency")
    public ResponseEntity<List<Map<String, Object>>> getSalesEfficiencyMetrics() {
        try {
            Dataset<Row> result = sparkAnalyticsService.calculateSalesEfficiencyMetrics();
            List<Map<String, Object>> jsonResult = rowsToList(result);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Failed to calculate sales efficiency metrics: " + e.getMessage())));
        }
    }
    
    /**
     * 7. So sánh theo brand và loại sản phẩm
     */
    @GetMapping("/spark/brand-comparison")
    public ResponseEntity<List<Map<String, Object>>> getBrandComparison() {
        try {
            List<Map<String, Object>> result = sparkAnalyticsService.getBrandComparisonAnalysis();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Failed to analyze brand comparison: " + e.getMessage())));
        }
    }
    
    /**
     * 3. Phân tích ảnh hưởng giảm giá đến rating
     */
    @GetMapping("/spark/discount-rating-impact")
    public ResponseEntity<List<Map<String, Object>>> getDiscountRatingImpact() {
        try {
            Dataset<Row> result = sparkAnalyticsService.analyzePromotionImpactOnRating();
            List<Map<String, Object>> jsonResult = rowsToList(result);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Failed to analyze discount rating impact: " + e.getMessage())));
        }
    }
    
    /**
     * Top performing products
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<Map<String, Object>>> getTopPerformingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> result = sparkAnalyticsService.getTopPerformingProducts(limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(List.of(Map.of("error", "Failed to get top products: " + e.getMessage())));
        }
    }

    // Additional endpoints for frontend compatibility
    
    /**
     * Get promotion comparison data for charts
     */
    @GetMapping("/promotion-comparison")
    public ResponseEntity<List<Map<String, Object>>> getPromotionComparison() {
        return comparePromotionVsNonPromotion();
    }
    
    /**
     * Get brand performance data for charts
     */
    @GetMapping("/brand-performance")
    public ResponseEntity<List<Map<String, Object>>> getBrandPerformance() {
        return getBrandComparison();
    }
    
    /**
     * Get discount impact data for charts
     */
    @GetMapping("/discount-impact")
    public ResponseEntity<List<Map<String, Object>>> getDiscountImpact() {
        return getDiscountRatingImpact();
    }
    
    /**
     * Dashboard overview data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            // Combine multiple analyses for dashboard
            List<Map<String, Object>> promotionAnalysis = sparkAnalyticsService.getPromotionEffectivenessAnalysis();
            List<Map<String, Object>> brandAnalysis = sparkAnalyticsService.getBrandComparisonAnalysis();
            List<Map<String, Object>> topProducts = sparkAnalyticsService.getTopPerformingProducts(5);
            
            Map<String, Object> dashboard = Map.of(
                    "promotionEffectiveness", promotionAnalysis,
                    "brandComparison", brandAnalysis,
                    "topProducts", topProducts,
                    "lastUpdated", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to load dashboard: " + e.getMessage()));
        }
    }
    
    // Data classes for request/response
    public static class PredictionRequest {
        private List<Long> productIds;
        private BigDecimal discountPercentage;
        
        public PredictionRequest() {}
        
        public PredictionRequest(List<Long> productIds, BigDecimal discountPercentage) {
            this.productIds = productIds;
            this.discountPercentage = discountPercentage;
        }
        
        public List<Long> getProductIds() { return productIds; }
        public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
        
        public BigDecimal getDiscountPercentage() { return discountPercentage; }
        public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }
    }
}
