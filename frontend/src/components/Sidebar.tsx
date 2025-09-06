"use client";

import { useState } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import {
  Home,
  Upload,
  BarChart3,
  TrendingUp,
  Settings,
  Menu,
  X,
  Database,
  Brain,
  PieChart,
} from "lucide-react";
import { cn } from "@/lib/utils";

interface SidebarProps {
  className?: string;
}

const navigation = [
  { name: "Dashboard", href: "/dashboard", icon: Home },
  { name: "Upload Data", href: "/dashboard/upload", icon: Upload },
  { name: "Analytics", href: "/dashboard/analytics", icon: BarChart3 },
  { name: "Predictions", href: "/dashboard/predictions", icon: Brain },
  { name: "Reports", href: "/dashboard/reports", icon: PieChart },
  { name: "Pricing Policy", href: "/dashboard/pricing", icon: TrendingUp },
  { name: "Data Management", href: "/dashboard/data", icon: Database },
  { name: "Settings", href: "/dashboard/settings", icon: Settings },
];

export default function Sidebar({ className }: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const pathname = usePathname();

  return (
    <div
      className={cn(
        "relative flex h-full flex-col border-r bg-background transition-all duration-300",
        isCollapsed ? "w-16" : "w-64",
        className
      )}
    >
      {/* Header */}
      <div className="flex h-16 items-center justify-between px-4 border-b">
        {!isCollapsed && (
          <div className="flex items-center space-x-2">
            <BarChart3 className="h-6 w-6 text-primary" />
            <span className="text-lg font-semibold">BigData Analytics</span>
          </div>
        )}
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="flex h-8 w-8 items-center justify-center rounded-md hover:bg-accent hover:text-accent-foreground"
        >
          {isCollapsed ? (
            <Menu className="h-4 w-4" />
          ) : (
            <X className="h-4 w-4" />
          )}
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 p-2">
        {navigation.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center space-x-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                isCollapsed && "justify-center space-x-0"
              )}
              title={isCollapsed ? item.name : undefined}
            >
              <item.icon
                className={cn("h-5 w-5", isCollapsed ? "h-6 w-6" : "")}
              />
              {!isCollapsed && <span>{item.name}</span>}
            </Link>
          );
        })}
      </nav>

      {/* Footer */}
      {!isCollapsed && (
        <div className="border-t p-4">
          <div className="text-xs text-muted-foreground">
            <p>BigData Analytics Platform</p>
            <p className="mt-1">© 2025 All rights reserved</p>
          </div>
        </div>
      )}
    </div>
  );
}
