import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import Sidebar from "../components/Sidebar";
import { DatasetProvider } from "../lib/dataset-context";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "BigData Analytics - Đánh giá hiệu quả khuyến mãi",
  description:
    "Hệ thống phân tích Big Data với Spark SQL để đánh giá hiệu quả chiến lược giá khuyến mãi",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-gray-50`}
      >
        <DatasetProvider>
          <div className="flex min-h-screen bg-gray-50">
            <Sidebar />
            <main className="flex-1 bg-gray-50 overflow-auto">{children}</main>
          </div>
        </DatasetProvider>
      </body>
    </html>
  );
}
