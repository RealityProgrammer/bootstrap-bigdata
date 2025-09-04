import Link from "next/link";
import {
  Upload,
  BarChart3,
  TrendingUp,
  Lightbulb,
  FileText,
  Activity,
  ArrowRight,
  Database,
  Zap,
  Target,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function Home() {
  const features = [
    {
      icon: Upload,
      title: "Tải dữ liệu",
      description: "Upload file CSV/Excel vào HDFS để bắt đầu phân tích",
      href: "/upload",
      color: "blue",
    },
    {
      icon: BarChart3,
      title: "Phân tích khuyến mãi",
      description: "Đánh giá hiệu quả chiến lược giá với Spark SQL + UDF",
      href: "/analyze",
      color: "green",
    },
    {
      icon: TrendingUp,
      title: "Dự đoán ML",
      description: "Mô hình Machine Learning dự báo tác động giảm giá",
      href: "/predict",
      color: "purple",
    },
    {
      icon: Lightbulb,
      title: "Gợi ý chiến lược",
      description: "Tối ưu hóa chính sách giá dựa trên insights",
      href: "/recommend",
      color: "orange",
    },
    {
      icon: FileText,
      title: "Báo cáo CSV",
      description: "Xuất kết quả chi tiết cho bộ phận kinh doanh",
      href: "/results",
      color: "indigo",
    },
    {
      icon: Activity,
      title: "Giám sát hệ thống",
      description: "Theo dõi trạng thái Spark Master & HDFS",
      href: "/health",
      color: "red",
    },
  ];

  const stats = [
    { label: "Công nghệ", value: "Spark SQL + ML", icon: Database },
    { label: "Hiệu suất", value: "Real-time Analytics", icon: Zap },
    { label: "Mục tiêu", value: "Tối ưu doanh thu", icon: Target },
  ];

  return (
    <div className="p-6 max-w-7xl mx-auto">
      {/* Hero Section */}
      <div className="text-center mb-12">
        <div className="inline-flex items-center space-x-2 bg-blue-100 text-blue-800 px-4 py-2 rounded-full text-sm font-medium mb-4">
          <Database className="w-4 h-4" />
          <span>Big Data Analytics Platform</span>
        </div>
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          Đánh giá hiệu quả{" "}
          <span className="text-blue-600">chiến lược khuyến mãi</span>
        </h1>
        <p className="text-xl text-gray-600 max-w-3xl mx-auto mb-8">
          Hệ thống phân tích dữ liệu lớn với Apache Spark, Machine Learning và
          Business Intelligence để tối ưu hóa chính sách giá và tăng doanh thu
        </p>

        <div className="flex justify-center space-x-4">
          <Button asChild size="lg" className="bg-blue-600 hover:bg-blue-700">
            <Link href="/upload">
              <Upload className="w-5 h-5 mr-2" />
              Bắt đầu phân tích
            </Link>
          </Button>
          <Button asChild variant="outline" size="lg">
            <Link href="/health">
              <Activity className="w-5 h-5 mr-2" />
              Kiểm tra hệ thống
            </Link>
          </Button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
        {stats.map((stat, index) => (
          <Card key={index} className="text-center">
            <CardContent className="p-6">
              <div className="inline-flex items-center justify-center w-12 h-12 bg-blue-100 rounded-full mb-4">
                <stat.icon className="w-6 h-6 text-blue-600" />
              </div>
              <div className="font-semibold text-gray-900">{stat.value}</div>
              <div className="text-sm text-gray-600">{stat.label}</div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Features Grid */}
      <div className="mb-12">
        <h2 className="text-2xl font-bold text-gray-900 text-center mb-8">
          Quy trình phân tích toàn diện
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <Card
              key={index}
              className="group hover:shadow-lg transition-all duration-200 cursor-pointer"
            >
              <Link href={feature.href}>
                <CardHeader>
                  <div
                    className={`inline-flex items-center justify-center w-12 h-12 bg-${feature.color}-100 rounded-full mb-3 group-hover:scale-110 transition-transform`}
                  >
                    <feature.icon
                      className={`w-6 h-6 text-${feature.color}-600`}
                    />
                  </div>
                  <CardTitle className="text-lg">{feature.title}</CardTitle>
                  <CardDescription>{feature.description}</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center text-blue-600 text-sm font-medium group-hover:text-blue-700">
                    Xem chi tiết
                    <ArrowRight className="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
                  </div>
                </CardContent>
              </Link>
            </Card>
          ))}
        </div>
      </div>

      {/* Process Flow */}
      <Card className="bg-gradient-to-r from-blue-50 to-indigo-50">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">Quy trình 10 bước đánh giá</CardTitle>
          <CardDescription>
            Phân tích toàn diện hiệu quả khuyến mãi với Spark SQL + UDF
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            {[
              "1. UDF phân loại khuyến mãi",
              "2. So sánh sales có/không giảm giá",
              "3. Ảnh hưởng giảm giá đến rating",
              "4. Hệ số chuyển đổi review/sales",
              "5. Ngưỡng giá tối ưu",
              "6. Tính toán KPI bằng Spark SQL",
              "7. Phân tích theo brand/category",
              "8. Mô hình ML dự đoán sales",
              "9. Gợi ý chính sách giá",
              "10. Xuất báo cáo CSV",
            ].map((step, index) => (
              <div key={index} className="text-center">
                <div className="w-8 h-8 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm font-bold mx-auto mb-2">
                  {index + 1}
                </div>
                <p className="text-sm text-gray-700">{step.split(". ")[1]}</p>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
