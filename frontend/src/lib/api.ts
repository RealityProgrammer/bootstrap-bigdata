// API configuration and service functions
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export interface SalesData {
    totalProducts: number;
    totalSales: number;
    totalRevenue: number;
    totalReviews: number;
    brands: string[];
    productTypes: string[];
    platforms: string[];
}

export interface PredictionResult {
    productId: number;
    productName: string;
    currentSalesVolume: number;
    predictedSalesVolume: number;
    increasePercentage: number;
    confidence: number;
    explanation: string;
}

export interface BrandPredictionResult {
    brand: string;
    productType: string;
    averageIncrease: number;
    totalProducts: number;
    explanation: string;
}

export interface PromotionComparisonData {
    category: string;
    sales: number;
    revenue: number;
    avgRating?: number;
}

export interface BrandPerformanceData {
    brand: string;
    sales: number;
    revenue: number;
    avgRating: number;
}

export interface DiscountImpactData {
    discountRange: string;
    avgRating: number;
    salesVolume: number;
}

// API Functions
export const apiService = {
    // Initialize analytics
    async initializeAnalytics(): Promise<{ message: string; status: string }> {
        const response = await fetch(`${API_BASE_URL}/analytics/init`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error('Failed to initialize analytics');
        }

        return response.json();
    },

    // Get data statistics
    async getDataStatistics(): Promise<SalesData> {
        const response = await fetch(`${API_BASE_URL}/data/statistics`);

        if (!response.ok) {
            throw new Error('Failed to fetch data statistics');
        }

        const data = await response.json();

        return {
            totalProducts: data.totalProducts || 0,
            totalSales: data.totalSales || 0,
            totalRevenue: data.totalRevenue || 0,
            totalReviews: data.totalReviews || 0,
            brands: data.brands || [],
            productTypes: data.productTypes || [],
            platforms: data.platforms || [],
        };
    },

    // Predict sales for a specific product
    async predictSalesIncrease(productId: number, discountPercentage: number): Promise<PredictionResult> {
        const response = await fetch(
            `${API_BASE_URL}/analytics/predict-sales/${productId}?discountPercentage=${discountPercentage}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
            }
        );

        if (!response.ok) {
            throw new Error('Failed to predict sales');
        }

        return response.json();
    },

    // Predict sales for multiple products
    async predictSalesForMultipleProducts(productIds: number[], discountPercentage: number): Promise<PredictionResult[]> {
        const response = await fetch(`${API_BASE_URL}/analytics/predict-sales/batch`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                productIds,
                discountPercentage,
            }),
        });

        if (!response.ok) {
            throw new Error('Failed to predict sales for multiple products');
        }

        return response.json();
    },

    // Predict sales by brand
    async predictSalesByBrand(brand: string, productType: string, discountPercentage: number): Promise<BrandPredictionResult> {
        const response = await fetch(
            `${API_BASE_URL}/analytics/predict-sales/brand?brand=${encodeURIComponent(brand)}&productType=${encodeURIComponent(productType)}&discountPercentage=${discountPercentage}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
            }
        );

        if (!response.ok) {
            throw new Error('Failed to predict sales by brand');
        }

        return response.json();
    },

    // Upload Excel file
    async uploadExcelFile(file: File): Promise<{
        success: boolean;
        processedRows: number;
        totalRows: number;
        errors: string[];
        message: string;
    }> {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`${API_BASE_URL}/data/import/excel`, {
            method: 'POST',
            body: formData,
        });

        if (!response.ok) {
            throw new Error('Failed to upload file');
        }

        return response.json();
    },

    // Export data to CSV
    async exportDataToCsv(): Promise<string> {
        const response = await fetch(`${API_BASE_URL}/data/export/csv`);

        if (!response.ok) {
            throw new Error('Failed to export data');
        }

        return response.text();
    },

    // Get Spark analytics data
    async getPromotionComparison(): Promise<PromotionComparisonData[]> {
        try {
            const response = await fetch(`${API_BASE_URL}/analytics/promotion-comparison`);
            if (!response.ok) {
                // Return mock data if API fails
                return [
                    { category: 'Không khuyến mãi', sales: 8420, revenue: 1247000 },
                    { category: 'Có khuyến mãi', sales: 12340, revenue: 1890000 }
                ];
            }
            return response.json();
        } catch (error) {
            console.warn('Using mock data for promotion comparison:', error);
            return [
                { category: 'Không khuyến mãi', sales: 8420, revenue: 1247000 },
                { category: 'Có khuyến mãi', sales: 12340, revenue: 1890000 }
            ];
        }
    },

    async getBrandPerformance(): Promise<BrandPerformanceData[]> {
        try {
            const response = await fetch(`${API_BASE_URL}/analytics/brand-performance`);
            if (!response.ok) {
                // Return mock data if API fails
                return [
                    { brand: 'Samsung', sales: 3200, revenue: 850000, avgRating: 4.2 },
                    { brand: 'Apple', sales: 2800, revenue: 1200000, avgRating: 4.5 },
                    { brand: 'Xiaomi', sales: 4100, revenue: 620000, avgRating: 4.1 },
                    { brand: 'Oppo', sales: 2900, revenue: 480000, avgRating: 4.0 },
                    { brand: 'Vivo', sales: 2400, revenue: 420000, avgRating: 3.9 }
                ];
            }
            return response.json();
        } catch (error) {
            console.warn('Using mock data for brand performance:', error);
            return [
                { brand: 'Samsung', sales: 3200, revenue: 850000, avgRating: 4.2 },
                { brand: 'Apple', sales: 2800, revenue: 1200000, avgRating: 4.5 },
                { brand: 'Xiaomi', sales: 4100, revenue: 620000, avgRating: 4.1 },
                { brand: 'Oppo', sales: 2900, revenue: 480000, avgRating: 4.0 },
                { brand: 'Vivo', sales: 2400, revenue: 420000, avgRating: 3.9 }
            ];
        }
    },

    async getDiscountImpact(): Promise<DiscountImpactData[]> {
        try {
            const response = await fetch(`${API_BASE_URL}/analytics/discount-impact`);
            if (!response.ok) {
                // Return mock data if API fails
                return [
                    { discountRange: '0%', avgRating: 4.1, salesVolume: 850 },
                    { discountRange: '5-10%', avgRating: 4.3, salesVolume: 1200 },
                    { discountRange: '11-20%', avgRating: 4.2, salesVolume: 1580 },
                    { discountRange: '21-30%', avgRating: 4.0, salesVolume: 1340 },
                    { discountRange: '30%+', avgRating: 3.8, salesVolume: 980 }
                ];
            }
            return response.json();
        } catch (error) {
            console.warn('Using mock data for discount impact:', error);
            return [
                { discountRange: '0%', avgRating: 4.1, salesVolume: 850 },
                { discountRange: '5-10%', avgRating: 4.3, salesVolume: 1200 },
                { discountRange: '11-20%', avgRating: 4.2, salesVolume: 1580 },
                { discountRange: '21-30%', avgRating: 4.0, salesVolume: 1340 },
                { discountRange: '30%+', avgRating: 3.8, salesVolume: 980 }
            ];
        }
    }
};
