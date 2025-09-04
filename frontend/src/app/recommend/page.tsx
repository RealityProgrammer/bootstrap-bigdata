"use client";

import { useState } from "react";
import {
  Lightbulb,
  TrendingUp,
  Target,
  DollarSign,
  CheckCircle,
  Download,
  Sparkles,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";

interface RecommendationResult {
  optimalDiscountRate: number;
  expectedRevenue: number;
  confidenceScore: number;
  riskLevel: string;
  strategies: Array<{
    strategy: string;
    description: string;
    expectedImpact: number;
    implementation: string;
  }>;
  marketingActions: Array<{
    action: string;
    timeline: string;
    budget: number;
    expectedROI: number;
  }>;
}

export default function RecommendPage() {
  const [category, setCategory] = useState("");
  const [brand, setBrand] = useState("");
  const [currentPrice, setCurrentPrice] = useState("");
  const [targetRevenue, setTargetRevenue] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<RecommendationResult | null>(null);

  const handleGenerateRecommendations = async () => {
    if (!category || !brand || !currentPrice) {
      alert("Vui lòng điền đầy đủ thông tin");
      return;
    }

    setLoading(true);
    try {
      // Simulate API call to get recommendations
      await new Promise((resolve) => setTimeout(resolve, 2000));

      const mockResult: RecommendationResult = {
        optimalDiscountRate: 15,
        expectedRevenue: 2450000,
        confidenceScore: 87,
        riskLevel: "Thấp",
        strategies: [
          {
            strategy: "Khuyến mãi theo mùa",
            description: "Tăng giảm giá 15-20% trong giai đoạn cao điểm",
            expectedImpact: 25,
            implementation: "Áp dụng từ ngày 15 đến 30 hàng tháng",
          },
          {
            strategy: "Bundle deals",
            description:
              "Kết hợp sản phẩm với phụ kiện để tăng giá trị đơn hàng",
            expectedImpact: 18,
            implementation: "Tạo combo 2-3 sản phẩm liên quan",
          },
          {
            strategy: "Dynamic pricing",
            description: "Điều chỉnh giá theo thời gian thực dựa trên demand",
            expectedImpact: 32,
            implementation: "Sử dụng AI để tự động điều chỉnh giá",
          },
        ],
        marketingActions: [
          {
            action: "Chạy quảng cáo Facebook/Google",
            timeline: "2 tuần",
            budget: 500000,
            expectedROI: 3.2,
          },
          {
            action: "Email marketing campaign",
            timeline: "1 tháng",
            budget: 200000,
            expectedROI: 4.5,
          },
          {
            action: "Influencer collaboration",
            timeline: "3 tuần",
            budget: 800000,
            expectedROI: 2.8,
          },
        ],
      };

      setResult(mockResult);
    } catch (error) {
      console.error("Error generating recommendations:", error);
      alert("Lỗi khi tạo gợi ý. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  const downloadReport = () => {
    if (!result) return;

    const csvContent = `Loại gợi ý,Giá trị,Mô tả
Tỷ lệ giảm giá tối ưu,${
      result.optimalDiscountRate
    }%,Mức giảm giá mang lại doanh thu cao nhất
Doanh thu dự kiến,${result.expectedRevenue.toLocaleString()} VND,Doanh thu ước tính với chiến lược được đề xuất
Độ tin cậy,${result.confidenceScore}%,Mức độ tin cậy của mô hình dự đoán
Mức độ rủi ro,${result.riskLevel},Đánh giá rủi ro khi áp dụng chiến lược

Chiến lược khuyến mãi:
${result.strategies
  .map((s) => `${s.strategy},${s.expectedImpact}%,"${s.description}"`)
  .join("\n")}

Hành động marketing:
${result.marketingActions
  .map(
    (a) =>
      `${a.action},${a.expectedROI}x ROI,"Timeline: ${
        a.timeline
      }, Budget: ${a.budget.toLocaleString()} VND"`
  )
  .join("\n")}`;

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = `marketing_recommendations_${
      new Date().toISOString().split("T")[0]
    }.csv`;
    link.click();
  };

  const getRiskColor = (risk: string) => {
    switch (risk) {
      case "Thấp":
        return "text-green-600 bg-green-100";
      case "Trung bình":
        return "text-yellow-600 bg-yellow-100";
      case "Cao":
        return "text-red-600 bg-red-100";
      default:
        return "text-gray-600 bg-gray-100";
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="mb-8">
        <div className="flex items-center space-x-3 mb-4">
          <div className="flex items-center justify-center w-10 h-10 bg-orange-100 rounded-full">
            <Lightbulb className="w-5 h-5 text-orange-600" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              Gợi ý chiến lược marketing
            </h1>
            <p className="text-gray-600">
              Tối ưu hóa chính sách giá và chiến lược bán hàng
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Input Form */}
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Target className="w-5 h-5" />
              <span>Thông tin sản phẩm</span>
            </CardTitle>
            <CardDescription>
              Nhập thông tin để nhận gợi ý tối ưu
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Danh mục sản phẩm
              </label>
              <Input
                placeholder="Ví dụ: Electronics, Fashion, Home..."
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Thương hiệu
              </label>
              <Input
                placeholder="Tên thương hiệu sản phẩm"
                value={brand}
                onChange={(e) => setBrand(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Giá hiện tại (VND)
              </label>
              <Input
                type="number"
                placeholder="1000000"
                value={currentPrice}
                onChange={(e) => setCurrentPrice(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Mục tiêu doanh thu (VND)
              </label>
              <Input
                type="number"
                placeholder="2000000"
                value={targetRevenue}
                onChange={(e) => setTargetRevenue(e.target.value)}
              />
            </div>

            <Button
              onClick={handleGenerateRecommendations}
              disabled={loading}
              className="w-full bg-orange-600 hover:bg-orange-700"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  Đang phân tích...
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 mr-2" />
                  Tạo gợi ý chiến lược
                </>
              )}
            </Button>
          </CardContent>
        </Card>

        {/* Results */}
        <div className="lg:col-span-2 space-y-6">
          {result ? (
            <>
              {/* Key Metrics */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <Card>
                  <CardContent className="p-4 text-center">
                    <div className="text-2xl font-bold text-orange-600">
                      {result.optimalDiscountRate}%
                    </div>
                    <div className="text-sm text-gray-600">
                      Tỷ lệ giảm giá tối ưu
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-4 text-center">
                    <div className="text-2xl font-bold text-green-600">
                      {(result.expectedRevenue / 1000000).toFixed(1)}M
                    </div>
                    <div className="text-sm text-gray-600">
                      Doanh thu dự kiến
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-4 text-center">
                    <div className="text-2xl font-bold text-blue-600">
                      {result.confidenceScore}%
                    </div>
                    <div className="text-sm text-gray-600">Độ tin cậy</div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-4 text-center">
                    <div
                      className={`inline-flex px-2 py-1 rounded-full text-sm font-medium ${getRiskColor(
                        result.riskLevel
                      )}`}
                    >
                      {result.riskLevel}
                    </div>
                    <div className="text-sm text-gray-600 mt-1">
                      Mức độ rủi ro
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Strategies */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <TrendingUp className="w-5 h-5" />
                    <span>Chiến lược khuyến mãi đề xuất</span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {result.strategies.map((strategy, index) => (
                      <div key={index} className="border rounded-lg p-4">
                        <div className="flex items-start justify-between mb-2">
                          <h4 className="font-semibold text-gray-900">
                            {strategy.strategy}
                          </h4>
                          <div className="flex items-center space-x-2">
                            <TrendingUp className="w-4 h-4 text-green-600" />
                            <span className="text-green-600 font-medium">
                              +{strategy.expectedImpact}%
                            </span>
                          </div>
                        </div>
                        <p className="text-gray-600 mb-2">
                          {strategy.description}
                        </p>
                        <div className="flex items-center space-x-2 text-sm">
                          <CheckCircle className="w-4 h-4 text-blue-600" />
                          <span className="text-blue-600">
                            {strategy.implementation}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* Marketing Actions */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <DollarSign className="w-5 h-5" />
                    <span>Hành động marketing</span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {result.marketingActions.map((action, index) => (
                      <div key={index} className="border rounded-lg p-4">
                        <div className="flex items-start justify-between mb-2">
                          <h4 className="font-semibold text-gray-900">
                            {action.action}
                          </h4>
                          <div className="text-right">
                            <div className="text-green-600 font-bold">
                              {action.expectedROI}x ROI
                            </div>
                            <div className="text-sm text-gray-500">
                              {action.timeline}
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center justify-between text-sm">
                          <span className="text-gray-600">
                            Ngân sách: {action.budget.toLocaleString()} VND
                          </span>
                          <Progress
                            value={(action.expectedROI / 5) * 100}
                            className="w-20"
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* Export */}
              <Card>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="font-semibold text-gray-900">
                        Xuất báo cáo gợi ý
                      </h4>
                      <p className="text-sm text-gray-600">
                        Tải về file CSV với đầy đủ gợi ý và số liệu
                      </p>
                    </div>
                    <Button onClick={downloadReport} variant="outline">
                      <Download className="w-4 h-4 mr-2" />
                      Tải về CSV
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </>
          ) : (
            <Card className="lg:col-span-2">
              <CardContent className="p-8 text-center">
                <div className="flex flex-col items-center space-y-4">
                  <div className="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center">
                    <Lightbulb className="w-8 h-8 text-orange-600" />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      Sẵn sàng tạo gợi ý chiến lược
                    </h3>
                    <p className="text-gray-600">
                      Nhập thông tin sản phẩm và nhấn &quot;Tạo gợi ý chiến
                      lược&quot; để nhận được
                      <br />
                      các đề xuất tối ưu hóa doanh thu và chiến lược marketing
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
