"use client";

import { useEffect, useState } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { apiClient } from "@/lib/api-client";
import {
  Users,
  FileText,
  CreditCard,
  TrendingUp,
  Activity,
  Download,
  Eye,
  Calendar,
  Loader2,
  BarChart3,
  PieChart,
  ArrowUpRight,
  ArrowDownRight,
  RefreshCcw,
} from "lucide-react";

interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  totalSubscriptions: number;
  totalContent: number;
  totalTests: number;
  totalLearningMaterials: number;
  totalPdfFiles: number;
  recentActivity: any[];
  popularContent: any[];
}

interface ActivityItem {
  id: number;
  type: string;
  title: string;
  description: string;
  timestamp: string;
  user?: {
    name: string;
    email: string;
  };
  subscription?: {
    plan: string;
    amount: string;
  };
}

interface ContentItem {
  id: number;
  title: string;
  type: string;
  downloads: number;
  fileType: string;
}

export default function AnalyticsDashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await apiClient.get<{ data: DashboardStats }>(
        "/api/admin/analytics/dashboard"
      );
      setStats(data.data);
    } catch (err) {
      console.error("Failed to fetch analytics:", err);
      setError("Failed to load analytics data");
    } finally {
      setIsLoading(false);
    }
  };

  const formatNumber = (num: number) =>
    new Intl.NumberFormat().format(num);

  const formatDate = (dateString: string) =>
    new Date(dateString).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  const engagementRate =
    stats && stats.totalUsers > 0
      ? Math.round((stats.activeUsers / stats.totalUsers) * 100)
      : 0;

  const statCards = [
    {
      title: "Total Users",
      value: stats ? formatNumber(stats.totalUsers) : "0",
      description: `${stats ? formatNumber(stats.activeUsers) : "0"} active in last 30 days`,
      icon: Users,
      color: "text-blue-600",
      bgColor: "bg-blue-50",
      trend: engagementRate,
      trendLabel: "engagement",
    },
    {
      title: "Subscriptions",
      value: stats ? formatNumber(stats.totalSubscriptions) : "0",
      description: "Active subscriptions",
      icon: CreditCard,
      color: "text-green-600",
      bgColor: "bg-green-50",
    },
    {
      title: "Content Items",
      value: stats ? formatNumber(stats.totalContent) : "0",
      description: `${stats?.totalTests ?? 0} tests · ${stats?.totalLearningMaterials ?? 0} materials · ${stats?.totalPdfFiles ?? 0} PDFs`,
      icon: FileText,
      color: "text-purple-600",
      bgColor: "bg-purple-50",
    },
    {
      title: "Engagement Rate",
      value: `${engagementRate}%`,
      description: "Active users / total users",
      icon: TrendingUp,
      color: "text-orange-600",
      bgColor: "bg-orange-50",
    },
  ];

  if (error) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              Analytics Dashboard
            </h1>
            <p className="text-muted-foreground">
              Platform performance and engagement metrics
            </p>
          </div>
        </div>
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16">
            <div className="text-red-600 text-center">
              <Activity className="h-12 w-12 mx-auto mb-4" />
              <h3 className="text-lg font-semibold mb-2">
                Failed to load analytics
              </h3>
              <p className="text-muted-foreground">{error}</p>
              <Button onClick={fetchStats} variant="outline" className="mt-4">
                <RefreshCcw className="h-4 w-4 mr-2" />
                Try Again
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            Analytics Dashboard
          </h1>
          <p className="text-muted-foreground">
            Platform performance and engagement metrics
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={fetchStats}
          disabled={isLoading}
        >
          {isLoading ? (
            <Loader2 className="h-4 w-4 animate-spin mr-2" />
          ) : (
            <RefreshCcw className="h-4 w-4 mr-2" />
          )}
          Refresh
        </Button>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {statCards.map((stat) => (
          <Card key={stat.title} className="relative overflow-hidden">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                {stat.title}
              </CardTitle>
              <div className={`p-2 rounded-full ${stat.bgColor}`}>
                <stat.icon className={`h-4 w-4 ${stat.color}`} />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? "..." : stat.value}
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                {isLoading ? "Loading..." : stat.description}
              </p>
              {stat.trend !== undefined && (
                <div className="flex items-center mt-2">
                  {stat.trend > 0 ? (
                    <ArrowUpRight className="h-4 w-4 text-green-600 mr-1" />
                  ) : (
                    <ArrowDownRight className="h-4 w-4 text-red-600 mr-1" />
                  )}
                  <span
                    className={`text-xs font-medium ${stat.trend > 0 ? "text-green-600" : "text-red-600"}`}
                  >
                    {stat.trend}%
                  </span>
                  <span className="text-xs text-muted-foreground ml-1">
                    {stat.trendLabel}
                  </span>
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Content Breakdown */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Tests</CardTitle>
            <BarChart3 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">
              {isLoading ? "..." : stats?.totalTests ?? 0}
            </div>
            <p className="text-xs text-muted-foreground">
              Theory driving tests
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Learning Materials
            </CardTitle>
            <FileText className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">
              {isLoading ? "..." : stats?.totalLearningMaterials ?? 0}
            </div>
            <p className="text-xs text-muted-foreground">
              Study materials available
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">PDF Files</CardTitle>
            <PieChart className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">
              {isLoading ? "..." : stats?.totalPdfFiles ?? 0}
            </div>
            <p className="text-xs text-muted-foreground">
              Uploaded documents
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Recent Activity & Popular Content */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
            <CardDescription>
              Latest user registrations and subscriptions
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {isLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
              ) : stats?.recentActivity &&
                stats.recentActivity.length > 0 ? (
                stats.recentActivity
                  .slice(0, 8)
                  .map((activity: ActivityItem, idx: number) => (
                    <div
                      key={`${activity.id}-${activity.type}-${idx}`}
                      className="flex items-start space-x-3"
                    >
                      <div
                        className={`p-2 rounded-full mt-1 ${
                          activity.type === "user_registration"
                            ? "bg-blue-50"
                            : "bg-green-50"
                        }`}
                      >
                        {activity.type === "user_registration" ? (
                          <Users className="h-4 w-4 text-blue-600" />
                        ) : (
                          <CreditCard className="h-4 w-4 text-green-600" />
                        )}
                      </div>
                      <div className="flex-1 space-y-1">
                        <p className="text-sm font-medium">
                          {activity.title}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          {activity.description}
                        </p>
                        <div className="flex items-center text-xs text-muted-foreground">
                          <Calendar className="h-3 w-3 mr-1" />
                          {formatDate(activity.timestamp)}
                        </div>
                      </div>
                    </div>
                  ))
              ) : (
                <div className="text-center py-8 text-muted-foreground">
                  <Activity className="h-8 w-8 mx-auto mb-2 opacity-50" />
                  <p>No recent activity</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Popular Content</CardTitle>
            <CardDescription>Most downloaded learning materials</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {isLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
              ) : stats?.popularContent &&
                stats.popularContent.length > 0 ? (
                stats.popularContent
                  .slice(0, 8)
                  .map((content: ContentItem) => (
                    <div
                      key={content.id}
                      className="flex items-center justify-between p-3 border rounded-lg"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="p-2 rounded-full bg-purple-50">
                          <FileText className="h-4 w-4 text-purple-600" />
                        </div>
                        <div>
                          <p className="text-sm font-medium line-clamp-1">
                            {content.title}
                          </p>
                          <Badge
                            variant="outline"
                            className="text-xs mt-1"
                          >
                            {content.fileType.split("/")[1] ||
                              content.fileType}
                          </Badge>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="flex items-center text-sm font-medium">
                          <Download className="h-3 w-3 mr-1" />
                          {content.downloads}
                        </div>
                        <p className="text-xs text-muted-foreground">
                          downloads
                        </p>
                      </div>
                    </div>
                  ))
              ) : (
                <div className="text-center py-8 text-muted-foreground">
                  <FileText className="h-8 w-8 mx-auto mb-2 opacity-50" />
                  <p>No content data available</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
