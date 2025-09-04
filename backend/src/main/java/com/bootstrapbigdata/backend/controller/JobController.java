package com.bootstrapbigdata.backend.controller;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bootstrapbigdata.backend.service.AnalysisService;
import com.bootstrapbigdata.backend.service.HdfsService;
import com.bootstrapbigdata.backend.service.PredictionService;
import com.bootstrapbigdata.backend.service.RecommendationService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {

    @Autowired
    private HdfsService hdfsService;
    
    @Autowired
    private AnalysisService analysisService;
    
    @Autowired
    private PredictionService predictionService;
    
    @Autowired
    private RecommendationService recommendationService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String hdfsPath = "/data/" + System.currentTimeMillis() + "_" + fileName;
            
            String result = hdfsService.uploadFile(file, hdfsPath);
            
            Map<String, String> response = new HashMap<>();
            response.put("hdfsPath", result);
            response.put("message", "File uploaded successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> runAnalysis(@RequestParam String dataset) {
        try {
            Map<String, Object> results = analysisService.runFullAnalysis(dataset);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Analysis failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/predict")
    public ResponseEntity<Map<String, Object>> predictSales(
            @RequestParam String dataset,
            @RequestParam double discount) {
        try {
            if (discount < 0 || discount > 0.9) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Discount must be between 0 and 0.9");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> results = predictionService.predictSalesWithDiscount(dataset, discount);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Prediction failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/recommend")
    public ResponseEntity<Map<String, Object>> getRecommendations(@RequestParam String dataset) {
        try {
            Map<String, Object> results = recommendationService.recommendOptimalDiscount(dataset);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Recommendation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/results")
    public ResponseEntity<Object> getResults(@RequestParam String path) {
        try {
            // This would read CSV from HDFS and return as JSON
            // Implementation depends on specific requirements
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Results retrieved from: " + path);
            response.put("note", "Implementation depends on specific CSV structure");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve results: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam String path) {
        try {
            InputStream inputStream = hdfsService.downloadFile(path);
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(status);
    }
}
