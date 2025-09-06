"use client";

import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Upload,
  FileSpreadsheet,
  CheckCircle,
  XCircle,
  AlertCircle,
} from "lucide-react";
import { apiService } from "@/lib/api";

interface UploadResult {
  success: boolean;
  processedRows: number;
  totalRows: number;
  errors: string[];
  message: string;
}

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<UploadResult | null>(null);
  const [dragActive, setDragActive] = useState(false);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const files = e.dataTransfer.files;
    if (files && files[0]) {
      setFile(files[0]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files[0]) {
      setFile(files[0]);
    }
  };

  const uploadFile = async () => {
    if (!file) return;

    setUploading(true);
    setResult(null);

    try {
      console.log("Uploading file:", file.name);
      const data = await apiService.uploadExcelFile(file);
      console.log("Upload result:", data);
      setResult(data);

      // Initialize analytics after successful upload
      if (data.success) {
        try {
          console.log("Initializing analytics after upload...");
          await apiService.initializeAnalytics();
          console.log("Analytics initialized successfully");
        } catch (error) {
          console.error("Failed to initialize analytics:", error);
        }
      }
    } catch (error) {
      console.error("Upload error:", error);
      setResult({
        success: false,
        processedRows: 0,
        totalRows: 0,
        errors: [error instanceof Error ? error.message : "Upload failed"],
        message: "Upload failed",
      });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Upload Data</h1>
        <p className="text-muted-foreground">
          Upload your Excel file containing sales data for analysis
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Upload Section */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Upload className="h-5 w-5" />
              File Upload
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Drag and Drop Zone */}
            <div
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                dragActive
                  ? "border-blue-500 bg-blue-50"
                  : "border-gray-300 hover:border-gray-400"
              }`}
              onDragEnter={handleDrag}
              onDragLeave={handleDrag}
              onDragOver={handleDrag}
              onDrop={handleDrop}
            >
              <FileSpreadsheet className="mx-auto h-12 w-12 text-gray-400 mb-4" />
              <div className="space-y-2">
                <p className="text-lg font-medium">
                  {file ? file.name : "Drop your Excel file here"}
                </p>
                <p className="text-sm text-muted-foreground">
                  or click to browse
                </p>
                <input
                  type="file"
                  accept=".xlsx,.xls"
                  onChange={handleFileSelect}
                  className="hidden"
                  id="file-upload"
                />
                <label
                  htmlFor="file-upload"
                  className="inline-block px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 cursor-pointer"
                >
                  Browse Files
                </label>
              </div>
            </div>

            {/* Upload Button */}
            <Button
              onClick={uploadFile}
              disabled={!file || uploading}
              className="w-full"
              size="lg"
            >
              {uploading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  Uploading...
                </>
              ) : (
                <>
                  <Upload className="h-4 w-4 mr-2" />
                  Upload File
                </>
              )}
            </Button>

            {/* File Info */}
            {file && (
              <div className="bg-gray-50 p-3 rounded text-sm">
                <p>
                  <strong>File:</strong> {file.name}
                </p>
                <p>
                  <strong>Size:</strong> {(file.size / 1024 / 1024).toFixed(2)}{" "}
                  MB
                </p>
                <p>
                  <strong>Type:</strong> {file.type}
                </p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Results Section */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AlertCircle className="h-5 w-5" />
              Upload Results
            </CardTitle>
          </CardHeader>
          <CardContent>
            {result ? (
              <div className="space-y-4">
                {/* Status Banner */}
                <div
                  className={`flex items-center gap-2 p-3 rounded ${
                    result.success
                      ? "bg-green-50 text-green-800 border border-green-200"
                      : "bg-red-50 text-red-800 border border-red-200"
                  }`}
                >
                  {result.success ? (
                    <CheckCircle className="h-5 w-5" />
                  ) : (
                    <XCircle className="h-5 w-5" />
                  )}
                  <span className="font-medium">{result.message}</span>
                </div>

                {/* Statistics */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-blue-50 p-3 rounded">
                    <p className="text-sm text-blue-600 font-medium">
                      Total Rows
                    </p>
                    <p className="text-2xl font-bold text-blue-800">
                      {result.totalRows}
                    </p>
                  </div>
                  <div className="bg-green-50 p-3 rounded">
                    <p className="text-sm text-green-600 font-medium">
                      Processed
                    </p>
                    <p className="text-2xl font-bold text-green-800">
                      {result.processedRows}
                    </p>
                  </div>
                </div>

                {/* Errors */}
                {result.errors && result.errors.length > 0 && (
                  <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                    <p className="font-medium text-yellow-800 mb-2">
                      Warnings ({result.errors.length}):
                    </p>
                    <ul className="text-sm text-yellow-700 space-y-1">
                      {result.errors.slice(0, 5).map((error, index) => (
                        <li key={index} className="flex items-start gap-2">
                          <span className="text-yellow-500">•</span>
                          <span>{error}</span>
                        </li>
                      ))}
                      {result.errors.length > 5 && (
                        <li className="text-yellow-600 italic">
                          ...and {result.errors.length - 5} more warnings
                        </li>
                      )}
                    </ul>
                  </div>
                )}

                {/* Success message */}
                {result.success && (
                  <div className="bg-blue-50 border border-blue-200 rounded p-3">
                    <p className="text-blue-800">
                      🎉 Data uploaded successfully! You can now view analytics
                      in the dashboard.
                    </p>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-8 text-muted-foreground">
                <FileSpreadsheet className="mx-auto h-12 w-12 mb-4 opacity-50" />
                <p>Upload results will appear here</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Data Format Help */}
      <Card>
        <CardHeader>
          <CardTitle>Expected Data Format</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h4 className="font-semibold mb-3 text-gray-900">
                Required Columns:
              </h4>
              <ul className="space-y-1 text-sm text-gray-600">
                <li>
                  <strong>product_name:</strong> Tên sản phẩm
                </li>
                <li>
                  <strong>product_type:</strong> Loại sản phẩm
                </li>
                <li>
                  <strong>brand:</strong> Thương hiệu
                </li>
                <li>
                  <strong>price:</strong> Giá bán (VNĐ)
                </li>
                <li>
                  <strong>sales_volume:</strong> Số lượng bán
                </li>
                <li>
                  <strong>sale_time:</strong> Thời gian bán
                </li>
                <li>
                  <strong>platform:</strong> Nền tảng (Shopee/Lazada/Tiki)
                </li>
                <li>
                  <strong>rating:</strong> Đánh giá (1-5)
                </li>
                <li>
                  <strong>reviews:</strong> Số review
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold mb-3 text-gray-900">
                Data Examples:
              </h4>
              <div className="bg-gray-50 p-3 rounded text-xs">
                <pre>
                  {`product_name: "iPhone 14 Pro"
product_type: "Smartphone"
brand: "Apple"
price: "28,990,000"
sales_volume: 150
sale_time: "10:30"
date: "15/03/2024"
platform: "Shopee"
rating: 4.5
reviews: 1250`}
                </pre>
              </div>
            </div>
          </div>

          <div className="mt-6 p-4 bg-blue-50 rounded-lg">
            <h4 className="font-semibold text-blue-900 mb-2">💡 Tips:</h4>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>• File Excel cần có header row với tên cột chính xác</li>
              <li>
                • Giá có thể có dấu phẩy phân cách hàng nghìn (28,990,000)
              </li>
              <li>• Thời gian có thể ở format: dd/MM/yyyy hoặc yyyy-MM-dd</li>
              <li>• Rating nên ở dạng số thập phân (4.5)</li>
              <li>• Kích thước file tối đa: 100MB</li>
            </ul>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
