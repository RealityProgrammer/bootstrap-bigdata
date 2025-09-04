"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Upload,
  BarChart3,
  TrendingUp,
  Lightbulb,
  FileText,
  Activity,
  Database,
} from "lucide-react";
import { cn } from "@/lib/utils";

const Sidebar: React.FC = () => {
  const pathname = usePathname();

  const items = [
    {
      href: "/upload",
      label: "Tải dữ liệu",
      icon: Upload,
      description: "Upload CSV/Excel vào HDFS",
    },
    {
      href: "/analyze",
      label: "Phân tích",
      icon: BarChart3,
      description: "Đánh giá hiệu quả khuyến mãi",
    },
    {
      href: "/predict",
      label: "Dự đoán",
      icon: TrendingUp,
      description: "Mô hình ML dự báo sales",
    },
    {
      href: "/recommend",
      label: "Gợi ý",
      icon: Lightbulb,
      description: "Chiến lược giá tối ưu",
    },
    {
      href: "/results",
      label: "Kết quả",
      icon: FileText,
      description: "Download báo cáo CSV",
    },
    {
      href: "/health",
      label: "Hệ thống",
      icon: Activity,
      description: "Trạng thái Spark/HDFS",
    },
  ];

  return (
    <aside className="w-72 bg-gradient-to-b from-blue-50 to-indigo-100 dark:from-slate-900 dark:to-slate-800 border-r border-blue-200 dark:border-slate-700 h-screen sticky top-0 flex flex-col">
      <div className="p-6 border-b border-blue-200 dark:border-slate-700">
        <div className="flex items-center space-x-3">
          <Database className="w-8 h-8 text-blue-600 dark:text-blue-400" />
          <div>
            <h1 className="font-bold text-lg text-gray-900 dark:text-white">
              BigData Analytics
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-300">
              Đánh giá hiệu quả khuyến mãi
            </p>
          </div>
        </div>
      </div>

      <nav className="flex-1 px-4 py-6 space-y-2">
        {items.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-start space-x-3 px-3 py-3 rounded-lg transition-all duration-200 group",
                isActive
                  ? "bg-blue-600 text-white shadow-lg shadow-blue-600/25"
                  : "text-gray-700 dark:text-gray-200 hover:bg-white/70 dark:hover:bg-slate-700/50 hover:shadow-md"
              )}
            >
              <Icon
                className={cn(
                  "w-5 h-5 mt-0.5 flex-shrink-0",
                  isActive
                    ? "text-white"
                    : "text-gray-500 dark:text-gray-400 group-hover:text-blue-600 dark:group-hover:text-blue-400"
                )}
              />
              <div className="flex-1 min-w-0">
                <div
                  className={cn(
                    "font-medium text-sm",
                    isActive ? "text-white" : "text-gray-900 dark:text-white"
                  )}
                >
                  {item.label}
                </div>
                <div
                  className={cn(
                    "text-xs mt-1",
                    isActive
                      ? "text-blue-100"
                      : "text-gray-500 dark:text-gray-400"
                  )}
                >
                  {item.description}
                </div>
              </div>
            </Link>
          );
        })}
      </nav>

      <div className="p-4 border-t border-blue-200 dark:border-slate-700">
        <div className="text-xs text-gray-500 dark:text-gray-400 text-center">
          <div className="font-medium">Spark SQL + ML Engine</div>
          <div className="mt-1">v1.0 — Business Intelligence</div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
