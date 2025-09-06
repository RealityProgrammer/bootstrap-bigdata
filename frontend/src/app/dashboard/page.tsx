'use client'

import { useState, useEffect } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, LineChart, Line, PieChart, Pie, Cell, ResponsiveContainer } from 'recharts'
import { TrendingUp, Package, DollarSign, Star, AlertCircle, RefreshCw } from 'lucide-react'
import { apiService, type SalesData, type PredictionResult } from '@/lib/api'

interface ChartData {
  category: string
  sales: number
  revenue: number
  avgRating?: number
}

interface BrandData {
  brand: string
  sales: number
  revenue: number
  avgRating: number
}

interface DiscountData {
  discount: string
  avgRating: number
  salesVolume: number
}

export default function Dashboard() {
  const [salesData, setSalesData] = useState<SalesData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedDiscount, setSelectedDiscount] = useState(10)
  const [predictionResult, setPredictionResult] = useState<PredictionResult | null>(null)
  const [predicting, setPredicting] = useState(false)

  // Chart data states
  const [chartPromotionData, setChartPromotionData] = useState<ChartData[]>([])
  const [chartBrandData, setChartBrandData] = useState<BrandData[]>([])
  const [chartDiscountData, setChartDiscountData] = useState<DiscountData[]>([])

  const loadData = async () => {
    setLoading(true)
    setError(null)
    try {
      // Initialize analytics first
      console.log('Initializing analytics...')
      await apiService.initializeAnalytics()
      
      // Load all data in parallel
      console.log('Loading data...')
      const [
        statistics,
        promotionComparison,
        brandPerformance,
        discountImpact
      ] = await Promise.all([
        apiService.getDataStatistics(),
        apiService.getPromotionComparison(),
        apiService.getBrandPerformance(),
        apiService.getDiscountImpact()
      ])

      console.log('Data loaded:', { statistics, promotionComparison, brandPerformance, discountImpact })

      setSalesData(statistics)
      setChartPromotionData(promotionComparison)
      setChartBrandData(brandPerformance)
      setChartDiscountData(discountImpact.map(item => ({
        discount: item.discountRange,
        avgRating: item.avgRating,
        salesVolume: item.salesVolume
      })))

    } catch (err) {
      console.error('Failed to load data:', err)
      setError(err instanceof Error ? err.message : 'Failed to load data')
      
      // Fallback to mock data
      setSalesData({
        totalProducts: 1250,
        totalSales: 15420,
        totalRevenue: 2847500,
        totalReviews: 8930,
        brands: ['Samsung', 'Apple', 'Xiaomi', 'Oppo', 'Vivo'],
        productTypes: ['Smartphone', 'Laptop', 'Tablet', 'Headphone', 'Watch'],
        platforms: ['Shopee', 'Lazada', 'Tiki']
      })
      
      setChartPromotionData([
        { category: 'Không khuyến mãi', sales: 8420, revenue: 1247000 },
        { category: 'Có khuyến mãi', sales: 12340, revenue: 1890000 }
      ])
      
      setChartBrandData([
        { brand: 'Samsung', sales: 3200, revenue: 850000, avgRating: 4.2 },
        { brand: 'Apple', sales: 2800, revenue: 1200000, avgRating: 4.5 },
        { brand: 'Xiaomi', sales: 4100, revenue: 620000, avgRating: 4.1 },
        { brand: 'Oppo', sales: 2900, revenue: 480000, avgRating: 4.0 },
        { brand: 'Vivo', sales: 2400, revenue: 420000, avgRating: 3.9 }
      ])
      
      setChartDiscountData([
        { discount: '0%', avgRating: 4.1, salesVolume: 850 },
        { discount: '5-10%', avgRating: 4.3, salesVolume: 1200 },
        { discount: '11-20%', avgRating: 4.2, salesVolume: 1580 },
        { discount: '21-30%', avgRating: 4.0, salesVolume: 1340 },
        { discount: '30%+', avgRating: 3.8, salesVolume: 980 }
      ])
    } finally {
      setLoading(false)
    }
  }

  const handlePredictSales = async () => {
    if (!salesData?.totalProducts) return
    
    setPredicting(true)
    try {
      // Use first product ID for demo, you might want to make this configurable
      const result = await apiService.predictSalesIncrease(1, selectedDiscount)
      setPredictionResult(result)
    } catch (err) {
      console.error('Prediction failed:', err)
      // Fallback to mock prediction
      setPredictionResult({
        productId: 1,
        productName: 'Sample Product',
        currentSalesVolume: 1250,
        predictedSalesVolume: 1250 + (1250 * selectedDiscount * 0.02),
        increasePercentage: selectedDiscount * 2,
        confidence: 85,
        explanation: `Dự đoán dựa trên mô hình học máy với độ tin cậy 85%. API connection failed, using mock data.`
      })
    } finally {
      setPredicting(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  // Platform distribution data
  const platformDistribution = [
    { name: 'Shopee', value: 45, color: '#FF6B35' },
    { name: 'Lazada', value: 35, color: '#004AAD' },
    { name: 'Tiki', value: 20, color: '#1BA3E0' }
  ]

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <RefreshCw className="animate-spin h-8 w-8 mx-auto mb-4" />
          <div className="text-lg">Đang tải dữ liệu...</div>
          <div className="text-sm text-gray-500 mt-2">Đang kết nối với Spark Analytics...</div>
        </div>
      </div>
    )
  }

  return (
    <div className="p-6 space-y-6 bg-gray-50 min-h-screen">
      <div className="mb-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              Hệ thống Phân tích Hiệu quả Kinh doanh E-commerce
            </h1>
            <p className="text-gray-600 mt-2">
              Phân tích dữ liệu lớn với Spark SQL và Machine Learning để tối ưu hóa chiến lược kinh doanh
            </p>
          </div>
          <Button onClick={loadData} variant="outline">
            <RefreshCw className="h-4 w-4 mr-2" />
            Làm mới
          </Button>
        </div>
        
        {error && (
          <div className="bg-yellow-50 border border-yellow-200 rounded-md p-4 mt-4">
            <div className="flex">
              <AlertCircle className="h-5 w-5 text-yellow-400 mr-2" />
              <div>
                <h3 className="text-sm font-medium text-yellow-800">Cảnh báo kết nối</h3>
                <p className="text-sm text-yellow-700 mt-1">
                  {error}. Hiện đang sử dụng dữ liệu mẫu.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Tổng sản phẩm</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{salesData?.totalProducts.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">
              +12% so với tháng trước
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Tổng doanh thu</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {salesData?.totalRevenue.toLocaleString()} VNĐ
            </div>
            <p className="text-xs text-muted-foreground">
              +18% so với tháng trước
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Tổng đơn hàng</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{salesData?.totalSales.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">
              +8% so với tháng trước
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Tổng đánh giá</CardTitle>
            <Star className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{salesData?.totalReviews.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">
              Rating trung bình: 4.2/5
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Promotion vs Non-Promotion Comparison */}
        <Card>
          <CardHeader>
            <CardTitle>So sánh Hiệu quả Khuyến mãi</CardTitle>
            <CardDescription>
              Phân tích doanh số giữa sản phẩm có và không có khuyến mãi
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chartPromotionData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="category" />
                <YAxis />
                <Tooltip formatter={(value, name) => [
                  name === 'sales' ? value.toLocaleString() : value.toLocaleString() + ' VNĐ',
                  name === 'sales' ? 'Doanh số' : 'Doanh thu'
                ]} />
                <Legend />
                <Bar dataKey="sales" fill="#8884d8" name="Doanh số" />
                <Bar dataKey="revenue" fill="#82ca9d" name="Doanh thu" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Brand Performance */}
        <Card>
          <CardHeader>
            <CardTitle>Hiệu quả theo Thương hiệu</CardTitle>
            <CardDescription>
              So sánh doanh số và rating trung bình theo thương hiệu
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chartBrandData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="brand" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="sales" fill="#8884d8" name="Doanh số" />
                <Bar dataKey="avgRating" fill="#ffc658" name="Rating TB" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Discount Impact on Rating */}
        <Card>
          <CardHeader>
            <CardTitle>Ảnh hưởng Giảm giá đến Rating</CardTitle>
            <CardDescription>
              Phân tích mối quan hệ giữa mức giảm giá và đánh giá khách hàng
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={chartDiscountData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="discount" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="avgRating" stroke="#8884d8" name="Rating TB" />
                <Line type="monotone" dataKey="salesVolume" stroke="#82ca9d" name="Doanh số" />
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Platform Distribution */}
        <Card>
          <CardHeader>
            <CardTitle>Phân bố theo Nền tảng</CardTitle>
            <CardDescription>
              Tỷ lệ doanh số trên các nền tảng thương mại điện tử
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={platformDistribution}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, value }) => `${name}: ${value}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {platformDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      {/* Machine Learning Prediction Section */}
      <Card>
        <CardHeader>
          <CardTitle>Dự đoán Tăng trưởng Doanh số với Machine Learning</CardTitle>
          <CardDescription>
            Sử dụng mô hình học máy để dự đoán ảnh hưởng của chiến lược giảm giá
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center space-x-4">
              <label className="text-sm font-medium">Mức giảm giá (%):</label>
              <select 
                value={selectedDiscount}
                onChange={(e) => setSelectedDiscount(Number(e.target.value))}
                className="border rounded px-3 py-1"
              >
                {[5, 10, 15, 20, 25, 30].map(discount => (
                  <option key={discount} value={discount}>{discount}%</option>
                ))}
              </select>
              <Button onClick={handlePredictSales} disabled={predicting}>
                {predicting ? (
                  <>
                    <RefreshCw className="animate-spin h-4 w-4 mr-2" />
                    Đang dự đoán...
                  </>
                ) : (
                  'Dự đoán Doanh số'
                )}
              </Button>
            </div>

            {predictionResult && (
              <div className="bg-blue-50 p-4 rounded-lg">
                <h4 className="font-semibold text-blue-900 mb-2">Kết quả Dự đoán:</h4>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <p className="text-sm text-blue-700">Doanh số dự kiến:</p>
                    <p className="text-lg font-bold text-blue-900">
                      {Math.round(predictionResult.predictedSalesVolume).toLocaleString()} sản phẩm
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-blue-700">Tăng trưởng:</p>
                    <p className="text-lg font-bold text-green-600">
                      +{predictionResult.increasePercentage.toFixed(1)}%
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-blue-700">Độ tin cậy:</p>
                    <p className="text-lg font-bold text-blue-900">{predictionResult.confidence}%</p>
                  </div>
                </div>
                <p className="text-sm text-blue-700 mt-2">{predictionResult.explanation}</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Recommendations Section */}
      <Card>
        <CardHeader>
          <CardTitle>Gợi ý Chính sách Giá thông minh</CardTitle>
          <CardDescription>
            Dựa trên phân tích dữ liệu lớn và Machine Learning
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="bg-green-50 p-4 rounded-lg">
              <h4 className="font-semibold text-green-900 mb-2">🎯 Tối ưu</h4>
              <p className="text-sm text-green-700">
                Giảm giá 10-15% cho sản phẩm có rating {'>'} 4.0 để tối đa hóa doanh thu
              </p>
            </div>
            <div className="bg-yellow-50 p-4 rounded-lg">
              <h4 className="font-semibold text-yellow-900 mb-2">⚡ Nhanh</h4>
              <p className="text-sm text-yellow-700">
                Áp dụng flash sale 20-25% trong 24h cho sản phẩm tồn kho cao
              </p>
            </div>
            <div className="bg-purple-50 p-4 rounded-lg">
              <h4 className="font-semibold text-purple-900 mb-2">🔥 Đột phá</h4>
              <p className="text-sm text-purple-700">
                Bundle products với giảm giá 30% để tăng review conversion rate
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* BigData & Service-Oriented Architecture Info */}
      <Card>
        <CardHeader>
          <CardTitle>Kiến trúc Hệ thống</CardTitle>
          <CardDescription>
            Tính chất BigData và Phần mềm hướng dịch vụ
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h4 className="font-semibold mb-3 text-blue-900">🔢 Đặc điểm BigData (5V):</h4>
              <ul className="space-y-2 text-sm">
                <li><strong>Volume:</strong> Xử lý hàng triệu giao dịch e-commerce</li>
                <li><strong>Velocity:</strong> Streaming data từ Shopee, Lazada, Tiki</li>
                <li><strong>Variety:</strong> Dữ liệu structured (sales) & unstructured (reviews)</li>
                <li><strong>Veracity:</strong> Data validation và cleaning pipeline</li>
                <li><strong>Value:</strong> Insights cho pricing strategy và business growth</li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold mb-3 text-green-900">🏗️ Kiến trúc Microservices:</h4>
              <ul className="space-y-2 text-sm">
                <li><strong>Analytics Service:</strong> Spark SQL processing với UDF</li>
                <li><strong>ML Service:</strong> Prediction models và recommendations</li>
                <li><strong>Data Service:</strong> ETL pipeline với Apache Spark</li>
                <li><strong>API Gateway:</strong> Spring Boot REST APIs</li>
                <li><strong>Frontend Service:</strong> Next.js SPA với real-time updates</li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

