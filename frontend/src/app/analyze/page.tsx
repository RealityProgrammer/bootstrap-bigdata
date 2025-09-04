"use client";
import React, { useState } from "react";
import {
  BarChart3,
  TrendingUp,
  DollarSign,
  Users,
  ShoppingCart,
  Star,
  Play,
  FileText,
  RefreshCw,
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
import { Progress } from "@/components/ui/progress";
import { apiService } from "@/lib/api";
import { useDataset } from "@/lib/dataset-context";

interface AnalysisResult {
  promotion_classification?: any;
  sales_comparison?: any;
  rating_impact?: any;
  review_conversion?: any;
  optimal_price?: any;
  overall_kpis?: any;
  brand_category_analysis?: any;
  platform_comparison?: any;
  seasonal_analysis?: any;
  hdfs_outputs?: any;
}

export default function AnalyzePage() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [error, setError] = useState<string>("");
  const { currentDataset, uploadedFiles, setCurrentDataset } = useDataset();

  const runAnalysis = async () => {
    if (!currentDataset?.trim()) {
      setError("Vui lòng chọn dataset để phân tích");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const response = await apiService.runAnalysis(currentDataset);
      if (response.ok) {
        setResult(response.data || response);
      } else {
        setError(response.error || "Có lỗi khi chạy phân tích");
      }
    } catch (err) {
      setError(String(err));
    } finally {
      setLoading(false);
    }
  };

  const renderKPICard = (
    title: string,
    value: string | number,
    icon: React.ReactNode,
    trend?: string
  ) => (
    <Card>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">{title}</p>
            <p className="text-2xl font-bold text-gray-900">{value}</p>
            {trend && <p className="text-sm text-green-600">{trend}</p>}
          </div>
          <div className="text-blue-600">{icon}</div>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Phân tích hiệu quả khuyến mãi
        </h1>
        <p className="text-gray-600">
          Đánh giá toàn diện tác động của chiến lược giá đến doanh số và hành vi
          khách hàng
        </p>
      </div>

      {/* Control Panel */}
      <Card className="mb-8">
        <CardHeader>
          <CardTitle className="flex items-center space-x-2 text-gray-900">
            <BarChart3 className="w-5 h-5 text-blue-600" />
            <span>Bảng điều khiển phân tích</span>
          </CardTitle>
          <CardDescription className="text-gray-600">
            Chạy phân tích Spark SQL + UDF trên dữ liệu bán hàng để đánh giá
            hiệu quả khuyến mãi
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {/* Dataset Selector */}
            <div>
              <label className="text-sm font-medium text-gray-700 mb-2 block">
                Dataset hiện tại
              </label>
              <div className="flex space-x-2">
                <Input
                  value={currentDataset || ""}
                  onChange={(e) => setCurrentDataset(e.target.value)}
                  placeholder="/data/your_dataset.csv"
                  className="flex-1"
                />
                <Button
                  onClick={runAnalysis}
                  disabled={loading}
                  className="px-6"
                >
                  {loading ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                      Đang phân tích...
                    </>
                  ) : (
                    <>
                      <Play className="w-4 h-4 mr-2" />
                      Chạy phân tích
                    </>
                  )}
                </Button>
              </div>
            </div>

            {/* Quick Select from Uploaded Files */}
            {uploadedFiles.length > 0 && (
              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">
                  Hoặc chọn từ file đã upload
                </label>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                  {uploadedFiles.map((file, index) => (
                    <Button
                      key={index}
                      variant={
                        currentDataset === file.path ? "default" : "outline"
                      }
                      className="justify-start text-left h-auto p-3"
                      onClick={() => setCurrentDataset(file.path)}
                    >
                      <div className="flex items-center space-x-2 w-full">
                        <FileText className="w-4 h-4 text-blue-600" />
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-medium text-gray-900 truncate">
                            {file.name}
                          </div>
                          <div className="text-xs text-gray-500">
                            {file.uploadedAt.toLocaleDateString()}
                          </div>
                        </div>
                      </div>
                    </Button>
                  ))}
                </div>
              </div>
            )}

            {loading && (
              <div className="mt-4">
                <div className="flex justify-between text-sm text-gray-600 mb-2">
                  <span>Tiến độ phân tích</span>
                  <span>Đang xử lý...</span>
                </div>
                <Progress value={65} className="w-full" />
              </div>
            )}

            {error && (
              <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-md">
                <p className="text-red-700 text-sm">{error}</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Results */}
      {result && (
        <div className="space-y-8">
          {/* KPI Overview */}
          {result.overall_kpis?.data?.[0] && (
            <div>
              <h2 className="text-2xl font-semibold mb-6 flex items-center space-x-2">
                <TrendingUp className="w-6 h-6 text-green-600" />
                <span>Tổng quan hiệu suất</span>
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {renderKPICard(
                  "Tổng sản phẩm",
                  result.overall_kpis.data[0].total_products?.toLocaleString() ||
                    "0",
                  <ShoppingCart className="w-6 h-6" />
                )}
                {renderKPICard(
                  "Doanh thu",
                  new Intl.NumberFormat("vi-VN", {
                    style: "currency",
                    currency: "VND",
                  }).format(result.overall_kpis.data[0].total_revenue || 0),
                  <DollarSign className="w-6 h-6" />
                )}
                {renderKPICard(
                  "Tỷ lệ khuyến mãi",
                  `${(
                    result.overall_kpis.data[0].discount_penetration_pct || 0
                  ).toFixed(1)}%`,
                  <Users className="w-6 h-6" />
                )}
                {renderKPICard(
                  "Rating trung bình",
                  (result.overall_kpis.data[0].avg_rating || 0).toFixed(2),
                  <Star className="w-6 h-6" />
                )}
              </div>
            </div>
          )}

          {/* Analysis Sections */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Sales Comparison */}
            {result.sales_comparison && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">
                    So sánh doanh số khuyến mãi
                  </CardTitle>
                  <CardDescription>
                    Hiệu quả bán hàng có/không giảm giá
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {result.sales_comparison.data?.map(
                      (item: any, index: number) => (
                        <div
                          key={index}
                          className="flex justify-between items-center p-3 bg-gray-50 rounded"
                        >
                          <span className="font-medium">
                            {item.is_discount
                              ? "Có khuyến mãi"
                              : "Không khuyến mãi"}
                          </span>
                          <div className="text-right">
                            <div className="font-bold text-blue-600">
                              {item.avg_sales_volume?.toFixed(0)} sản phẩm/SP
                            </div>
                            <div className="text-sm text-gray-600">
                              {new Intl.NumberFormat("vi-VN", {
                                style: "currency",
                                currency: "VND",
                              }).format(item.total_revenue || 0)}
                            </div>
                          </div>
                        </div>
                      )
                    )}
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Promotion Classification */}
            {result.promotion_classification && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">
                    Phân loại khuyến mãi
                  </CardTitle>
                  <CardDescription>
                    Hiệu quả theo mức độ giảm giá
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {result.promotion_classification.data?.map(
                      (item: any, index: number) => (
                        <div
                          key={index}
                          className="flex justify-between items-center p-3 bg-gray-50 rounded"
                        >
                          <span className="font-medium">
                            {item.promo_class}
                          </span>
                          <div className="text-right">
                            <div className="font-bold text-green-600">
                              {item.avg_sales_volume?.toFixed(0)} SP/sản phẩm
                            </div>
                            <div className="text-sm text-gray-600">
                              {(item.avg_discount_rate * 100)?.toFixed(1)}% giảm
                              giá
                            </div>
                          </div>
                        </div>
                      )
                    )}
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Rating Impact */}
            {result.rating_impact && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">
                    Ảnh hưởng đến đánh giá
                  </CardTitle>
                  <CardDescription>Rating theo mức giảm giá</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {result.rating_impact.data?.map(
                      (item: any, index: number) => (
                        <div
                          key={index}
                          className="flex justify-between items-center p-3 bg-gray-50 rounded"
                        >
                          <span className="font-medium">
                            {item.discount_bucket}
                          </span>
                          <div className="text-right">
                            <div className="font-bold text-yellow-600">
                              {item.avg_rating?.toFixed(2)} ⭐
                            </div>
                            <div className="text-sm text-gray-600">
                              {item.count} sản phẩm
                            </div>
                          </div>
                        </div>
                      )
                    )}
                  </div>
                </CardContent>
              </Card>
            )}

            {/* CSV Downloads */}
            {result.hdfs_outputs && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg flex items-center space-x-2">
                    <FileText className="w-5 h-5" />
                    <span>Tải xuống báo cáo</span>
                  </CardTitle>
                  <CardDescription>
                    File CSV chi tiết cho bộ phận kinh doanh
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    {Object.entries(result.hdfs_outputs).map(([key, path]) => (
                      <Button
                        key={key}
                        variant="outline"
                        className="w-full justify-start"
                        onClick={() =>
                          window.open(
                            `/api/download?path=${encodeURIComponent(
                              path as string
                            )}`,
                            "_blank"
                          )
                        }
                      >
                        <FileText className="w-4 h-4 mr-2" />
                        {key
                          .replace(/_/g, " ")
                          .replace(/\b\w/g, (l) => l.toUpperCase())}
                        .csv
                      </Button>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
