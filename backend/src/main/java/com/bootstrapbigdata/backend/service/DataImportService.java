package com.bootstrapbigdata.backend.service;

import com.bootstrapbigdata.backend.entity.*;
import com.bootstrapbigdata.backend.repository.*;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataImportService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    private final DateTimeFormatter[] dateFormatters = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };
    
    private final Random random = new Random();
    
    /**
     * Import dữ liệu từ Excel file
     */
    public Map<String, Object> importExcelData(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            int processedRows = 0;
            int totalRows = sheet.getLastRowNum();
            List<String> errors = new ArrayList<>();
            
            // Skip header row (row 0)
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    try {
                        processExcelRow(row);
                        processedRows++;
                    } catch (Exception e) {
                        errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    }
                }
            }
            
            result.put("success", true);
            result.put("processedRows", processedRows);
            result.put("totalRows", totalRows);
            result.put("errors", errors);
            result.put("message", "Successfully imported " + processedRows + " rows out of " + totalRows);
        }
        
        return result;
    }
    
    /**
     * Process một row từ Excel
     * Expected columns: product_name, product_type, sales_volume, price, sale_time, date, platform, brand, reviews, rating
     */
    private void processExcelRow(Row row) {
        // Extract data from row
        String productName = getCellValueAsString(row.getCell(0));
        String productType = getCellValueAsString(row.getCell(1));
        Integer salesVolume = getCellValueAsInteger(row.getCell(2));
        BigDecimal price = getCellValueAsBigDecimal(row.getCell(3));
        String saleTimeStr = getCellValueAsString(row.getCell(4));
        String dateStr = getCellValueAsString(row.getCell(5));
        String platform = getCellValueAsString(row.getCell(6));
        String brand = getCellValueAsString(row.getCell(7));
        Integer reviews = getCellValueAsInteger(row.getCell(8));
        BigDecimal rating = getCellValueAsBigDecimal(row.getCell(9));
        
        if (productName == null || productType == null || price == null) {
            throw new RuntimeException("Missing required fields: product_name, product_type, or price");
        }
        
        // Find or create product
        Product product = findOrCreateProduct(productName, productType, brand, price, platform);
        
        // Generate original price if not exists (simulate pre-discount price)
        if (product.getOriginalPrice() == null || product.getOriginalPrice().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal factor = BigDecimal.valueOf(1.0 + (0.05 + random.nextDouble() * 0.20)); // 5-25% higher
            product.setOriginalPrice(price.multiply(factor).setScale(2, RoundingMode.HALF_UP));
            productRepository.save(product);
        }
        
        // Parse sale time
        LocalDateTime saleTime = parseDateTime(saleTimeStr, dateStr);
        
        // Create sale record
        Sale sale = createSaleRecord(product, salesVolume, price, saleTime, platform);
        
        // Determine if this is a promotion based on original vs current price
        if (price != null && product.getOriginalPrice() != null && 
            price.compareTo(product.getOriginalPrice()) < 0) {
            BigDecimal discountPercent = product.getOriginalPrice().subtract(price)
                    .divide(product.getOriginalPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            sale.setIsPromotion(true);
            sale.setDiscountPercentage(discountPercent);
            saleRepository.save(sale);
            
            // Create promotion record
            createPromotionRecord(product, discountPercent, saleTime);
        }
        
        // Create review records (generate multiple reviews based on reviews count)
        if (reviews != null && reviews > 0 && rating != null) {
            createMultipleReviews(product, reviews, rating, saleTime, platform);
        }
    }
    
    /**
     * Parse price string with comma decimal separator and thousand separators
     */
    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String raw = cell.getStringCellValue().trim()
                                   .replaceAll("\\s+", "") // remove all spaces
                                   .replaceAll("\\.", "") // remove thousand separators (dots)
                                   .replaceAll(",", ".") // replace comma with dot for decimal
                                   .replaceAll("[^0-9.\\-]", ""); // keep only numbers, dots and minus
                    if (raw.isEmpty()) return null;
                    return new BigDecimal(raw);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String raw = cell.getStringCellValue().trim().replaceAll("[^0-9]", "");
                    if (raw.isEmpty()) return null;
                    return Integer.parseInt(raw);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
    
    /**
     * Parse date time from sale_time and date columns
     */
    private LocalDateTime parseDateTime(String saleTimeStr, String dateStr) {
        LocalDateTime saleTime = LocalDateTime.now();
        
        // Try to parse date string first
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            for (DateTimeFormatter formatter : dateFormatters) {
                try {
                    saleTime = LocalDateTime.of(
                        java.time.LocalDate.parse(dateStr.trim(), formatter), 
                        java.time.LocalTime.of(12, 0) // Default to noon
                    );
                    break;
                } catch (DateTimeParseException ignored) {}
            }
        }
        
        // Adjust based on sale_time (month info)
        if (saleTimeStr != null && !saleTimeStr.trim().isEmpty()) {
            String monthStr = saleTimeStr.toLowerCase().trim();
            if (monthStr.contains("1") || monthStr.contains("một")) {
                saleTime = saleTime.withMonth(1);
            } else if (monthStr.contains("2") || monthStr.contains("hai")) {
                saleTime = saleTime.withMonth(2);
            } else if (monthStr.contains("3") || monthStr.contains("ba")) {
                saleTime = saleTime.withMonth(3);
            } else if (monthStr.contains("4") || monthStr.contains("tư")) {
                saleTime = saleTime.withMonth(4);
            } else if (monthStr.contains("5") || monthStr.contains("năm")) {
                saleTime = saleTime.withMonth(5);
            } else if (monthStr.contains("6") || monthStr.contains("sáu")) {
                saleTime = saleTime.withMonth(6);
            } else if (monthStr.contains("7") || monthStr.contains("bảy")) {
                saleTime = saleTime.withMonth(7);
            } else if (monthStr.contains("8") || monthStr.contains("tám")) {
                saleTime = saleTime.withMonth(8);
            } else if (monthStr.contains("9") || monthStr.contains("chín")) {
                saleTime = saleTime.withMonth(9);
            } else if (monthStr.contains("10") || monthStr.contains("mười")) {
                saleTime = saleTime.withMonth(10);
            } else if (monthStr.contains("11")) {
                saleTime = saleTime.withMonth(11);
            } else if (monthStr.contains("12")) {
                saleTime = saleTime.withMonth(12);
            }
        }
        
        return saleTime;
    }
    
    /**
     * Find existing product or create new one
     */
    private Product findOrCreateProduct(String productName, String productType, String brand, BigDecimal price, String platform) {
        // Try to find existing product by name and brand
        List<Product> existingProducts = productRepository.findByBrandAndProductType(
            brand != null ? brand : "Unknown", 
            productType != null ? productType : "Unknown"
        );
        
        Optional<Product> existing = existingProducts.stream()
                .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                .findFirst();
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new product
        Product product = new Product();
        product.setProductName(productName);
        product.setProductType(productType != null ? productType : "Unknown");
        product.setBrand(brand != null ? brand : "Unknown");
        product.setPrice(price);
        product.setPlatform(platform != null ? platform : "Unknown");
        
        return productRepository.save(product);
    }
    
    /**
     * Create sale record
     */
    private Sale createSaleRecord(Product product, Integer salesVolume, BigDecimal price, LocalDateTime saleTime, String platform) {
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setSalesVolume(salesVolume != null ? salesVolume : 1);
        sale.setSalePrice(price);
        sale.setSaleTime(saleTime);
        sale.setPlatform(platform != null ? platform : product.getPlatform());
        sale.setIsPromotion(false);
        sale.setDiscountPercentage(BigDecimal.ZERO);
        
        return saleRepository.save(sale);
    }
    
    /**
     * Create multiple review records based on review count
     */
    private void createMultipleReviews(Product product, Integer reviewCount, BigDecimal avgRating, LocalDateTime baseTime, String platform) {
        for (int i = 0; i < Math.min(reviewCount, 10); i++) { // Limit to 10 reviews per import
            Review review = new Review();
            review.setProduct(product);
            
            // Generate rating around the average with some variance
            double variance = 0.5 + random.nextDouble() * 1.0; // ±0.5 to ±1.5 rating variance
            double newRating = avgRating.doubleValue() + (random.nextBoolean() ? variance : -variance);
            newRating = Math.max(1.0, Math.min(5.0, newRating)); // Clamp between 1 and 5
            
            review.setRating(BigDecimal.valueOf(newRating).setScale(1, RoundingMode.HALF_UP));
            review.setComment(generateRandomComment(newRating));
            review.setReviewDate(baseTime.minusDays(random.nextInt(30))); // Random date within last 30 days
            review.setPlatform(platform);
            review.setCustomerId("CUSTOMER_" + UUID.randomUUID().toString().substring(0, 8));
            
            reviewRepository.save(review);
        }
    }
    
    /**
     * Generate random comment based on rating
     */
    private String generateRandomComment(double rating) {
        String[] goodComments = {
            "Sản phẩm tuyệt vời, rất hài lòng!",
            "Chất lượng tốt, đáng tiền",
            "Giao hàng nhanh, sản phẩm như mô tả",
            "Recommend cho mọi người",
            "Sẽ mua lại lần sau"
        };
        
        String[] averageComments = {
            "Sản phẩm bình thường",
            "Tạm được, có thể cải thiện",
            "Giá hơi cao so với chất lượng",
            "Không quá ấn tượng",
            "Ổn, không có gì đặc biệt"
        };
        
        String[] badComments = {
            "Sản phẩm không như mong đợi",
            "Chất lượng kém",
            "Giao hàng chậm",
            "Không đáng tiền",
            "Sẽ không mua lại"
        };
        
        if (rating >= 4.0) {
            return goodComments[random.nextInt(goodComments.length)];
        } else if (rating >= 3.0) {
            return averageComments[random.nextInt(averageComments.length)];
        } else {
            return badComments[random.nextInt(badComments.length)];
        }
    }
    
    /**
     * Create promotion record
     */
    private Promotion createPromotionRecord(Product product, BigDecimal discountPercent, LocalDateTime startDate) {
        Promotion promotion = new Promotion();
        promotion.setProduct(product);
        promotion.setDiscountPercentage(discountPercent);
        promotion.setStartDate(startDate.minusDays(random.nextInt(7))); // Started up to 7 days ago
        promotion.setEndDate(startDate.plusDays(7 + random.nextInt(23))); // Ends 7-30 days later
        promotion.setPromotionType("DISCOUNT");
        promotion.setIsActive(true);
        
        return promotionRepository.save(promotion);
    }
    
    /**
     * Export dữ liệu ra CSV cho bộ phận kinh doanh
     */
    public String exportDataToCsv() throws IOException {
        List<Object[]> combinedData = saleRepository.findCombinedSalesData();
        
        StringWriter stringWriter = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(stringWriter);
        
        // Write header
        String[] header = {
            "Sale ID", "Product ID", "Product Name", "Brand", "Product Type", "Platform",
            "Sales Volume", "Sale Price", "Is Promotion", "Discount Percentage", "Sale Time",
            "Review ID", "Rating", "Review Date"
        };
        csvWriter.writeNext(header);
        
        // Write data
        for (Object[] row : combinedData) {
            String[] csvRow = new String[header.length];
            for (int i = 0; i < row.length && i < csvRow.length; i++) {
                csvRow[i] = row[i] != null ? row[i].toString() : "";
            }
            csvWriter.writeNext(csvRow);
        }
        
        csvWriter.close();
        return stringWriter.toString();
    }
    
    /**
     * Lấy thống kê tổng quan
     */
    public Map<String, Object> getDataStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        long totalProducts = productRepository.count();
        long totalSales = saleRepository.count();
        long totalReviews = reviewRepository.count();
        long totalPromotions = promotionRepository.count();
        
        // Revenue calculation
        List<Sale> allSales = saleRepository.findAll();
        BigDecimal totalRevenue = allSales.stream()
                .map(sale -> sale.getSalePrice().multiply(BigDecimal.valueOf(sale.getSalesVolume())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Average rating
        List<Review> allReviews = reviewRepository.findAll();
        BigDecimal avgRating = allReviews.isEmpty() ? BigDecimal.ZERO :
                allReviews.stream()
                        .map(Review::getRating)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(allReviews.size()), 2, RoundingMode.HALF_UP);
        
        // Brand, product types, platforms
        List<String> brands = productRepository.findDistinctBrands();
        List<String> productTypes = productRepository.findDistinctProductTypes();
        List<String> platforms = productRepository.findDistinctPlatforms();
        
        stats.put("totalProducts", totalProducts);
        stats.put("totalSales", totalSales);
        stats.put("totalReviews", totalReviews);
        stats.put("totalPromotions", totalPromotions);
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageRating", avgRating);
        stats.put("brands", brands);
        stats.put("productTypes", productTypes);
        stats.put("platforms", platforms);
        
        return stats;
    }
}
