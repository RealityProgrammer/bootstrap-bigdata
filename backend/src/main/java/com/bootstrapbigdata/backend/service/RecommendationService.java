package com.bootstrapbigdata.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    @Autowired
    private PredictionService predictionService;

    public Map<String, Object> recommendOptimalDiscount(String datasetPath) {
        List<Double> discountRates = List.of(0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40);
        List<Map<String, Object>> scenarios = new ArrayList<>();
        
        double bestEfficiency = -Double.MAX_VALUE;
        double bestDiscount = 0.05;
        Map<String, Object> bestScenario = null;

        for (double discount : discountRates) {
            try {
                Map<String, Object> prediction = predictionService.predictSalesWithDiscount(datasetPath, discount);
                
                double salesDelta = (Double) prediction.get("sales_delta");
                double revenueDelta = (Double) prediction.get("revenue_delta");
                
                // Calculate efficiency: marginal sales improvement per discount point
                double efficiency = salesDelta / discount;
                
                Map<String, Object> scenario = new HashMap<>();
                scenario.put("discount_rate", discount);
                scenario.put("sales_delta", salesDelta);
                scenario.put("revenue_delta", revenueDelta);
                scenario.put("efficiency", efficiency);
                scenario.put("predicted_avg_sales", prediction.get("predicted_avg_sales"));
                scenario.put("predicted_revenue", prediction.get("predicted_revenue"));
                
                scenarios.add(scenario);
                
                // Find best efficiency (considering revenue growth as well)
                double revenueWeight = 0.7; // Weight for revenue vs pure sales volume
                double combinedScore = efficiency * (1 - revenueWeight) + (revenueDelta / discount) * revenueWeight;
                
                if (combinedScore > bestEfficiency && revenueDelta > 0) {
                    bestEfficiency = combinedScore;
                    bestDiscount = discount;
                    bestScenario = scenario;
                }
                
            } catch (Exception e) {
                // Continue with other discount rates if one fails
                System.err.println("Failed to analyze discount " + discount + ": " + e.getMessage());
            }
        }

        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("recommended_discount", bestDiscount);
        recommendation.put("recommended_discount_percent", bestDiscount * 100);
        recommendation.put("best_scenario", bestScenario);
        recommendation.put("all_scenarios", scenarios);
        recommendation.put("analysis_summary", createAnalysisSummary(scenarios, bestDiscount));

        return recommendation;
    }

    private Map<String, Object> createAnalysisSummary(List<Map<String, Object>> scenarios, double bestDiscount) {
        Map<String, Object> summary = new HashMap<>();
        
        // Find scenarios with positive revenue impact
        long positiveRevenueCount = scenarios.stream()
                .mapToLong(s -> (Double) s.get("revenue_delta") > 0 ? 1 : 0)
                .sum();
        
        // Calculate average efficiency
        double avgEfficiency = scenarios.stream()
                .mapToDouble(s -> (Double) s.get("efficiency"))
                .average()
                .orElse(0.0);

        summary.put("total_scenarios_analyzed", scenarios.size());
        summary.put("positive_revenue_scenarios", positiveRevenueCount);
        summary.put("average_efficiency", avgEfficiency);
        summary.put("optimal_discount_explanation", 
                String.format("%.0f%% discount offers the best balance of sales increase and revenue growth", 
                        bestDiscount * 100));

        return summary;
    }
}
