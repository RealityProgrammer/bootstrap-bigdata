"use client";
import React, { useState } from "react";
import {
  TrendingUp,
  Zap,
  BarChart,
  Target,
  Calculator,
  Download,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { apiService } from "@/lib/api";

interface PredictionResult {
  scenario_summary?: {
    discount_rate: number;
    predicted_sales_increase: number;
    revenue_impact: number;
    efficiency_score: number;
  };
  model_performance?: {
    rmse: number;
    mae: number;
    r2_score: number;
  };
  csv_path?: string;
  business_insights?: string[];
}

export default function PredictPage() {
  const [loading, setLoading] = useState(false);
  const [datasetPath, setDatasetPath] = useState("/data/1.shope_lazada.csv");
  const [discountRate, setDiscountRate] = useState(0.1);
  const [result, setResult] = useState<PredictionResult | null>(null);
  const [error, setError] = useState<string>("");

  const runPredict = async () => {
    if (!datasetPath.trim()) {
      setError("Vui lòng nhập đường dẫn dataset");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const response = await apiService.predictSales(datasetPath, discountRate);
      if (response.ok) {
        setResult(response.data || response);
      } else {
        setError(response.error || "Có lỗi khi chạy dự đoán");
      }
    } catch (err) {
      setError(String(err));
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(amount);
  };

  const formatPercent = (value: number) => {
    return `${(value * 100).toFixed(1)}%`;
  };

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Dự đoán tác động khuyến mãi
        </h1>
        <p className="text-gray-600">
          Sử dụng Machine Learning để dự báo hiệu quả của chiến lược giảm giá
        </p>
      </div>

      {/* Control Panel */}
      <Card className="mb-8">
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Zap className="w-5 h-5 text-purple-600" />
            <span>Mô hình ML dự đoán</span>
          </CardTitle>
          <CardDescription>
            Linear Regression + Feature Engineering để dự báo sales tăng khi áp
            dụng chiến lược giảm giá
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <div>
              <label className="text-sm font-medium text-gray-700 mb-2 block">
                Dataset HDFS
              </label>
              <Input
                value={datasetPath}
                onChange={(e) => setDatasetPath(e.target.value)}
                placeholder="/data/your_dataset.csv"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700 mb-2 block">
                Mức giảm giá (%)
              </label>
              <Input
                type="number"
                min="0"
                max="50"
                step="1"
                value={discountRate * 100}
                onChange={(e) => setDiscountRate(Number(e.target.value) / 100)}
                placeholder="10"
              />
            </div>
            <div className="flex items-end">
              <Button
                onClick={runPredict}
                disabled={loading}
                className="w-full"
              >
                {loading ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                    Đang dự đoán...
                  </>
                ) : (
                  <>
                    <TrendingUp className="w-4 h-4 mr-2" />
                    Chạy dự đoán
                  </>
                )}
              </Button>
            </div>
          </div>

          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-red-700 text-sm">{error}</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Prediction Results */}
      {result && (
        <div className="space-y-6">
          {/* Scenario Summary */}
          {result.scenario_summary && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <Card className="bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-blue-700">
                        Mức giảm giá
                      </p>
                      <p className="text-2xl font-bold text-blue-900">
                        {formatPercent(result.scenario_summary.discount_rate)}
                      </p>
                    </div>
                    <Target className="w-8 h-8 text-blue-600" />
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-gradient-to-br from-green-50 to-green-100 border-green-200">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-green-700">
                        Dự báo tăng sales
                      </p>
                      <p className="text-2xl font-bold text-green-900">
                        +
                        {formatPercent(
                          result.scenario_summary.predicted_sales_increase
                        )}
                      </p>
                    </div>
                    <TrendingUp className="w-8 h-8 text-green-600" />
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-gradient-to-br from-purple-50 to-purple-100 border-purple-200">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-purple-700">
                        Tác động doanh thu
                      </p>
                      <p className="text-2xl font-bold text-purple-900">
                        {formatCurrency(result.scenario_summary.revenue_impact)}
                      </p>
                    </div>
                    <BarChart className="w-8 h-8 text-purple-600" />
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-gradient-to-br from-orange-50 to-orange-100 border-orange-200">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-orange-700">
                        Hiệu quả
                      </p>
                      <p className="text-2xl font-bold text-orange-900">
                        {result.scenario_summary.efficiency_score.toFixed(2)}
                      </p>
                    </div>
                    <Calculator className="w-8 h-8 text-orange-600" />
                  </div>
                </CardContent>
              </Card>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Model Performance */}
            {result.model_performance && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">
                    Hiệu suất mô hình ML
                  </CardTitle>
                  <CardDescription>
                    Chỉ số đánh giá độ chính xác của model
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                      <span className="font-medium">
                        R² Score (Độ chính xác)
                      </span>
                      <span className="font-bold text-blue-600">
                        {(result.model_performance.r2_score * 100).toFixed(1)}%
                      </span>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                      <span className="font-medium">
                        RMSE (Sai số căn bậc hai)
                      </span>
                      <span className="font-bold text-orange-600">
                        {result.model_performance.rmse.toFixed(2)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                      <span className="font-medium">
                        MAE (Sai số tuyệt đối)
                      </span>
                      <span className="font-bold text-red-600">
                        {result.model_performance.mae.toFixed(2)}
                      </span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Business Insights */}
            {result.business_insights && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Insights kinh doanh</CardTitle>
                  <CardDescription>
                    Gợi ý chiến lược dựa trên kết quả dự đoán
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {result.business_insights.map((insight, index) => (
                      <div
                        key={index}
                        className="flex items-start space-x-3 p-3 bg-blue-50 rounded"
                      >
                        <div className="flex-shrink-0 w-6 h-6 bg-blue-600 text-white rounded-full flex items-center justify-center text-xs font-bold">
                          {index + 1}
                        </div>
                        <p className="text-sm text-blue-900">{insight}</p>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}
          </div>

          {/* Download Results */}
          {result.csv_path && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center space-x-2">
                  <Download className="w-5 h-5" />
                  <span>Tải xuống kết quả</span>
                </CardTitle>
                <CardDescription>
                  File CSV chi tiết với kết quả dự đoán cho từng sản phẩm
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() =>
                    window.open(
                      `/api/download?path=${encodeURIComponent(
                        result.csv_path!
                      )}`,
                      "_blank"
                    )
                  }
                >
                  <Download className="w-4 h-4 mr-2" />
                  Tải về prediction_results_
                  {formatPercent(discountRate).replace("%", "percent")}.csv
                </Button>
              </CardContent>
            </Card>
          )}

          {/* Model Info */}
          <Card className="bg-gradient-to-r from-gray-50 to-gray-100">
            <CardContent className="p-6">
              <h3 className="font-semibold text-gray-900 mb-3">
                Thông tin mô hình ML
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                <div>
                  <span className="font-medium text-gray-700">Algorithm:</span>
                  <p className="text-gray-600">Linear Regression Pipeline</p>
                </div>
                <div>
                  <span className="font-medium text-gray-700">Features:</span>
                  <p className="text-gray-600">
                    Price, Category, Brand, Rating, Platform
                  </p>
                </div>
                <div>
                  <span className="font-medium text-gray-700">Target:</span>
                  <p className="text-gray-600">Sales Volume Prediction</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
