const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
const API_BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080/api';

interface ApiResponse<T = any> {
    ok: boolean;
    data?: T;
    error?: string;
    message?: string;
}

class ApiService {
    private baseUrl: string;

    constructor() {
        this.baseUrl = API_BACKEND_URL;
    }

    async uploadFile(file: File): Promise<ApiResponse> {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch(`${this.baseUrl}/upload`, {
                method: 'POST',
                body: formData,
            });
            return await response.json();
        } catch (error) {
            return { ok: false, error: String(error) };
        }
    }

    async runAnalysis(dataset: string): Promise<ApiResponse> {
        try {
            const response = await fetch(`${this.baseUrl}/analyze`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `dataset=${encodeURIComponent(dataset)}`,
            });
            return await response.json();
        } catch (error) {
            return { ok: false, error: String(error) };
        }
    }

    async predictSales(dataset: string, discount: number): Promise<ApiResponse> {
        try {
            const response = await fetch(
                `${this.baseUrl}/predict?dataset=${encodeURIComponent(dataset)}&discount=${discount}`
            );
            return await response.json();
        } catch (error) {
            return { ok: false, error: String(error) };
        }
    }

    async getRecommendations(dataset: string): Promise<ApiResponse> {
        try {
            const response = await fetch(
                `${this.baseUrl}/recommend?dataset=${encodeURIComponent(dataset)}`
            );
            return await response.json();
        } catch (error) {
            return { ok: false, error: String(error) };
        }
    }

    async getResults(path: string): Promise<ApiResponse> {
        try {
            const response = await fetch(
                `${this.baseUrl}/results?path=${encodeURIComponent(path)}`
            );
            return await response.json();
        } catch (error) {
            return { ok: false, error: String(error) };
        }
    }

    async downloadFile(path: string): Promise<Response> {
        return fetch(`${this.baseUrl}/download?path=${encodeURIComponent(path)}`);
    }

    async checkHealth(): Promise<ApiResponse> {
        try {
            const response = await fetch(`${this.baseUrl}/health`);
            return await response.json();
        } catch (error) {
            return { ok: false, error: String(error) };
        }
    }
}

export const apiService = new ApiService();
export type { ApiResponse };
