package com.bootstrapbigdata.backend.controller;

import com.bootstrapbigdata.backend.service.DataImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/data")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class DataController {
    
    @Autowired
    private DataImportService dataImportService;
    
    /**
     * Import dữ liệu từ Excel file
     */
    @PostMapping("/import/excel")
    public ResponseEntity<Map<String, Object>> importExcelData(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = dataImportService.importExcelData(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to import data: " + e.getMessage()));
        }
    }
    
    /**
     * Export dữ liệu ra CSV
     */
    // @GetMapping("/export/csv")
    // public ResponseEntity<String> exportDataToCsv() {
    //     try {
    //         String csvData = dataImportService.exportDataToCsv();
    //         return ResponseEntity.ok()
    //                 .header("Content-Type", "text/csv")
    //                 .header("Content-Disposition", "attachment; filename=sales_analysis.csv")
    //                 .body(csvData);
    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body("Failed to export data: " + e.getMessage());
    //     }
    // }
    
    /**
     * Lấy thống kê tổng quan
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDataStatistics() {
        Map<String, Object> stats = dataImportService.getDataStatistics();
        return ResponseEntity.ok(stats);
    }
}
