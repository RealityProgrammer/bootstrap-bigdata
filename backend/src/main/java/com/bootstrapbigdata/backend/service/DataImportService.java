package com.bootstrapbigdata.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class DataImportService {
    
    @Autowired
    private SparkDataProcessingService sparkDataProcessingService;
    
    /**
     * Import dữ liệu từ Excel file sử dụng Spark cluster
     */
    public Map<String, Object> importExcelData(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only Excel files (.xlsx) are supported");
        }
        
        // Process with Spark cluster
        return sparkDataProcessingService.processExcelFileWithSpark(file);
    }
    
    /**
     * Lấy thống kê tổng quan từ Spark
     */
    public Map<String, Object> getDataStatistics() {
        return sparkDataProcessingService.getProcessingStatistics();
    }
}
