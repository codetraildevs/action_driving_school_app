"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { Users, FileText, CreditCard, TrendingUp, Activity, Download, Eye, User, Calendar, Clock, Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";

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

interface SubscriptionRequest {
  id: number;
  userId: number;
  requestedTests: number;
  requestedDays: number;
  status: string;
  createdAt: string;
  user: {
    firstName: string;
    lastName: string;
    phoneNumber: string;
    email: string;
  };
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

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requests, setRequests] = useState<SubscriptionRequest[]>([]);
  const [requestsLoading, setRequestsLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
    fetchRequests();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setError(null);
      const data = await apiClient.get<{ data: DashboardStats }>("/api/admin/analytics/dashboard");
      setStats(data.data);
    } catch (error) {
      console.error("Failed to fetch dashboard data:", error);
      setError("Failed to load dashboard data");
    } finally {
      setIsLoading(false);
    }
  };

  const fetchRequests = async () => {
    try {
      setRequestsLoading(true);
      const data = await apiClient.get<{ data: SubscriptionRequest[]; total: number }>(
        "/api/subscriptions/userRequests?pageSize=20"
      );
      setRequests(data.data || []);
    } catch (error) {
      console.error("Failed to fetch requests:", error);
    } finally {
      setRequestsLoading(false);
    }
  };

  const formatNumber = (num: number) => {
    return new Intl.NumberFormat().format(num);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'user_registration':
        return <User className="h-4 w-4" />;
      case 'subscription':
        return <CreditCard className="h-4 w-4" />;
      default:
        return <Activity className="h-4 w-4" />;
    }
  };

  const getFileTypeIcon = (fileType: string) => {
    if (fileType.includes('pdf')) return <FileText className="h-4 w-4" />;
    if (fileType.includes('video')) return <Eye className="h-4 w-4" />;
    if (fileType.includes('image')) return <Eye className="h-4 w-4" />;
    return <Download className="h-4 w-4" />;
  };

  const statCards = [
    {
      title: "Total Users",
      value: stats ? formatNumber(stats.totalUsers) : "0",
      description: `${stats ? formatNumber(stats.activeUsers) : "0"} active users`,
      icon: Users,
      color: "text-blue-600",
      bgColor: "bg-blue-50",
    },
    {
      title: "Active Subscriptions",
      value: stats ? formatNumber(stats.totalSubscriptions) : "0",
      description: "Current subscribers",
      icon: CreditCard,
      color: "text-green-600",
      bgColor: "bg-green-50",
    },
    {
      title: "Content Items",
      value: stats ? formatNumber(stats.totalContent) : "0",
      description: `${stats ? `${stats.totalTests} tests, ${stats.totalLearningMaterials} materials, ${stats.totalPdfFiles} PDFs` : "Loading..."}`,
      icon: FileText,
      color: "text-purple-600",
      bgColor: "bg-purple-50",
    },
    {
      title: "Engagement Rate",
      value: stats && stats.totalUsers > 0 ? `${Math.round((stats.activeUsers / stats.totalUsers) * 100)}%` : "0%",
      description: "Active user percentage",
      icon: TrendingUp,
      color: "text-orange-600",
      bgColor: "bg-orange-50",
    },
  ];

  if (error) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground">Welcome back! Here's an overview of your platform.</p>
        </div>
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16">
            <div className="text-red-600 text-center">
              <Activity className="h-12 w-12 mx-auto mb-4" />
              <h3 className="text-lg font-semibold mb-2">Failed to load dashboard</h3>
              <p className="text-muted-foreground">{error}</p>
              <Button 
                onClick={fetchDashboardData} 
                variant="outline" 
                className="mt-4"
              >
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
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Welcome back! Here's an overview of your platform.
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {statCards.map((stat) => (
          <Card key={stat.title} className="relative overflow-hidden">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
              <div className={`p-2 rounded-full ${stat.bgColor}`}>
                <stat.icon className={`h-4 w-4 ${stat.color}`} />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{isLoading ? "..." : stat.value}</div>
              <p className="text-xs text-muted-foreground mt-1">
                {isLoading ? "Loading..." : stat.description}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Recent Activity & Popular Content */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
            <CardDescription>Latest user registrations and activities</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {isLoading ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
                  <p className="text-sm text-muted-foreground mt-2">Loading activity...</p>
                </div>
              ) : stats?.recentActivity && stats.recentActivity.length > 0 ? (
                stats.recentActivity.slice(0, 5).map((activity: ActivityItem) => (
                  <div key={activity.id + activity.title} className="flex items-start space-x-3">
                    <div className={`p-2 rounded-full mt-1 ${
                      activity.type === 'user_registration' ? 'bg-blue-50' : 'bg-green-50'
                    }`}>
                      {getActivityIcon(activity.type)}
                    </div>
                    <div className="flex-1 space-y-1">
                      <p className="text-sm font-medium">{activity.title}</p>
                      <p className="text-sm text-muted-foreground">{activity.description}</p>
                      <div className="flex items-center text-xs text-muted-foreground">
                        <Calendar className="h-3 w-3 mr-1" />
                        {formatDate(activity.timestamp)}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-8">
                  <Activity className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
                  <p className="text-sm text-muted-foreground">No recent activity</p>
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
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600 mx-auto"></div>
                  <p className="text-sm text-muted-foreground mt-2">Loading content...</p>
                </div>
              ) : stats?.popularContent && stats.popularContent.length > 0 ? (
                stats.popularContent.slice(0, 5).map((content: ContentItem) => (
                  <div key={content.id} className="flex items-center justify-between p-3 border rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="p-2 rounded-full bg-purple-50">
                        {getFileTypeIcon(content.fileType)}
                      </div>
                      <div>
                        <p className="text-sm font-medium line-clamp-1">{content.title}</p>
                        <Badge variant="outline" className="text-xs mt-1">
                          {content.fileType.split('/')[1] || content.fileType}
                        </Badge>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="flex items-center text-sm font-medium">
                        <Download className="h-3 w-3 mr-1" />
                        {content.downloads}
                      </div>
                      <p className="text-xs text-muted-foreground">downloads</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-8">
                  <FileText className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
                  <p className="text-sm text-muted-foreground">No content data available</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Subscription Requests */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <CreditCard className="h-5 w-5" />
                Subscription Requests
              </CardTitle>
              <CardDescription>Recent user subscription requests</CardDescription>
            </div>
            <a href="/admin/user-requests">
              <Button variant="outline" size="sm">View All</Button>
            </a>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {requestsLoading ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : requests.length > 0 ? (
              requests.slice(0, 10).map((req) => (
                <div key={req.id} className="flex items-center justify-between p-3 border rounded-lg">
                  <div className="flex items-center space-x-3">
                    <div className={`p-2 rounded-full ${
                      req.status === "ACCEPTED" ? "bg-green-50" :
                      req.status === "REJECTED" ? "bg-red-50" : "bg-yellow-50"
                    }`}>
                      {req.status === "ACCEPTED" ? (
                        <Check className="h-4 w-4 text-green-600" />
                      ) : req.status === "REJECTED" ? (
                        <X className="h-4 w-4 text-red-600" />
                      ) : (
                        <Clock className="h-4 w-4 text-yellow-600" />
                      )}
                    </div>
                    <div>
                      <p className="text-sm font-medium">
                        {req.user.firstName} {req.user.lastName}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {req.user.phoneNumber} • {req.requestedTests} tests • {req.requestedDays} days
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Badge
                      variant={
                        req.status === "ACCEPTED" ? "default" :
                        req.status === "REJECTED" ? "destructive" : "secondary"
                      }
                    >
                      {req.status}
                    </Badge>
                    <span className="text-xs text-muted-foreground">
                      {formatDate(req.createdAt)}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-8">
                <CreditCard className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
                <p className="text-sm text-muted-foreground">No subscription requests</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* System Health */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            System Status
          </CardTitle>
          <CardDescription>Current system health and performance metrics</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-full bg-green-50">
                  <Activity className="h-4 w-4 text-green-600" />
                </div>
                <div>
                  <p className="text-sm font-medium">API Status</p>
                  <p className="text-xs text-muted-foreground">Backend service health</p>
                </div>
              </div>
              <Badge variant="outline" className="bg-green-50 text-green-700">
                Operational
              </Badge>
            </div>
            
            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-full bg-green-50">
                  <Users className="h-4 w-4 text-green-600" />
                </div>
                <div>
                  <p className="text-sm font-medium">Database</p>
                  <p className="text-xs text-muted-foreground">Data storage system</p>
                </div>
              </div>
              <Badge variant="outline" className="bg-green-50 text-green-700">
                Healthy
              </Badge>
            </div>
            
            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-full bg-blue-50">
                  <FileText className="h-4 w-4 text-blue-600" />
                </div>
                <div>
                  <p className="text-sm font-medium">Content Delivery</p>
                  <p className="text-xs text-muted-foreground">File serving performance</p>
                </div>
              </div>
              <Badge variant="outline" className="bg-blue-50 text-blue-700">
                Optimal
              </Badge>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

 