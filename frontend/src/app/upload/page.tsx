"use client";
import React, { useState } from "react";
import {
  Upload,
  FileText,
  CheckCircle,
  AlertCircle,
  Database,
  ArrowRight,
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
import { useDataset } from "@/lib/dataset-context";
import Link from "next/link";

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string>("");
  const { addUploadedFile, uploadedFiles } = useDataset();

  const onFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      setFile(selectedFile);
      setError("");
      setResult(null);
    }
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setError("Vui lòng chọn file CSV hoặc Excel");
      return;
    }

    setUploading(true);
    setError("");

    try {
      const response = await apiService.uploadFile(file);
      if (response.ok) {
        setResult(response);

        // Add to context
        addUploadedFile({
          name: file.name,
          path: response.hdfsPath || `/data/${file.name}`,
          uploadedAt: new Date(),
          size: file.size,
        });

        setFile(null);
        // Reset file input
        const fileInput = document.getElementById(
          "file-input"
        ) as HTMLInputElement;
        if (fileInput) fileInput.value = "";
      } else {
        setError(response.error || "Có lỗi khi tải file");
      }
    } catch (err) {
      setError(String(err));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Tải dữ liệu lên HDFS
        </h1>
        <p className="text-gray-600">
          Upload file CSV/Excel chứa dữ liệu bán hàng để phân tích hiệu quả
          khuyến mãi
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload Form */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2 text-gray-900">
                <Upload className="w-5 h-5 text-blue-600" />
                <span>Chọn file dữ liệu</span>
              </CardTitle>
              <CardDescription className="text-gray-600">
                Định dạng hỗ trợ: CSV (phân cách bằng dấu ;), Excel (.xlsx)
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={onSubmit} className="space-y-4">
                <div className="space-y-2">
                  <label
                    htmlFor="file-input"
                    className="text-sm font-medium text-gray-700"
                  >
                    File dữ liệu
                  </label>
                  <Input
                    id="file-input"
                    type="file"
                    accept=".csv,.xlsx,.xls"
                    onChange={onFileSelect}
                    className="cursor-pointer"
                  />
                  {file && (
                    <div className="flex items-center space-x-2 text-sm text-gray-600">
                      <FileText className="w-4 h-4" />
                      <span className="text-gray-700">
                        {file.name} ({(file.size / 1024 / 1024).toFixed(2)} MB)
                      </span>
                    </div>
                  )}
                </div>

                <Button
                  type="submit"
                  disabled={!file || uploading}
                  className="w-full"
                >
                  {uploading ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                      Đang tải lên...
                    </>
                  ) : (
                    <>
                      <Upload className="w-4 h-4 mr-2" />
                      Tải lên HDFS
                    </>
                  )}
                </Button>
              </form>

              {error && (
                <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-md flex items-start space-x-2">
                  <AlertCircle className="w-5 h-5 text-red-500 mt-0.5 flex-shrink-0" />
                  <div className="text-red-700 text-sm">{error}</div>
                </div>
              )}

              {result && result.ok && (
                <div className="mt-4 space-y-4">
                  <div className="p-3 bg-green-50 border border-green-200 rounded-md">
                    <div className="flex items-start space-x-2">
                      <CheckCircle className="w-5 h-5 text-green-500 mt-0.5 flex-shrink-0" />
                      <div className="flex-1">
                        <div className="text-green-800 font-medium text-sm">
                          Upload thành công!
                        </div>
                        <div className="text-green-700 text-sm mt-1">
                          Đường dẫn HDFS:{" "}
                          <code className="bg-green-100 px-1 rounded text-gray-800">
                            {result.hdfsPath}
                          </code>
                        </div>
                        {result.message && (
                          <div className="text-green-600 text-sm mt-2">
                            {result.message}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Next Steps */}
                  <div className="p-4 bg-blue-50 border border-blue-200 rounded-md">
                    <h4 className="text-blue-800 font-medium mb-2">
                      Bước tiếp theo
                    </h4>
                    <div className="space-y-2">
                      <Link href="/analyze">
                        <Button
                          variant="outline"
                          className="w-full justify-start"
                        >
                          <ArrowRight className="w-4 h-4 mr-2" />
                          <span className="text-gray-800">
                            Phân tích dữ liệu ngay
                          </span>
                        </Button>
                      </Link>
                      <Link href="/predict">
                        <Button
                          variant="outline"
                          className="w-full justify-start"
                        >
                          <ArrowRight className="w-4 h-4 mr-2" />
                          <span className="text-gray-800">
                            Dự đoán tác động giảm giá
                          </span>
                        </Button>
                      </Link>
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Uploaded Files History */}
          {uploadedFiles.length > 0 && (
            <Card className="mt-6">
              <CardHeader>
                <CardTitle className="text-lg text-gray-900">
                  File đã tải lên
                </CardTitle>
                <CardDescription className="text-gray-600">
                  Lịch sử các file đã upload vào hệ thống
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {uploadedFiles.map((file, index) => (
                    <div
                      key={index}
                      className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                    >
                      <div className="flex items-center space-x-3">
                        <FileText className="w-4 h-4 text-blue-600" />
                        <div>
                          <div className="font-medium text-gray-900">
                            {file.name}
                          </div>
                          <div className="text-sm text-gray-600">
                            {file.uploadedAt.toLocaleString()} •{" "}
                            {(file.size / 1024 / 1024).toFixed(2)} MB
                          </div>
                        </div>
                      </div>
                      <div className="text-sm text-gray-500">
                        <code className="bg-gray-200 px-2 py-1 rounded text-gray-800">
                          {file.path}
                        </code>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Instructions */}
        <div>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2 text-gray-900">
                <Database className="w-5 h-5 text-green-600" />
                <span>Yêu cầu dữ liệu</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h4 className="font-medium text-sm mb-2 text-gray-900">
                  Cột bắt buộc:
                </h4>
                <ul className="text-sm text-gray-700 space-y-1">
                  <li>• product_name</li>
                  <li>• product_type</li>
                  <li>• sales_volume</li>
                  <li>• price (dùng dấu phẩy thập phân)</li>
                  <li>• sale_time</li>
                  <li>• platform</li>
                  <li>• brand</li>
                  <li>• reviews</li>
                  <li>• rating</li>
                </ul>
              </div>

              <div>
                <h4 className="font-medium text-sm mb-2 text-gray-900">
                  Định dạng:
                </h4>
                <ul className="text-sm text-gray-700 space-y-1">
                  <li>• CSV: phân cách bằng dấu ;</li>
                  <li>• Giá: dùng dấu phẩy (vd: 350,89)</li>
                  <li>• Encoding: UTF-8</li>
                </ul>
              </div>

              <div className="pt-2 border-t">
                <p className="text-xs text-gray-600">
                  Hệ thống sẽ tự động tạo các trường phụ như original_price,
                  promo_price, discount_rate để phân tích.
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
