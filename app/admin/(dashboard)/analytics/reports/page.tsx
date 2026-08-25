"use client";

import { useEffect, useState, useCallback } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { apiClient } from "@/lib/api-client";
import {
  Loader2,
  Download,
  FileText,
  Users,
  CreditCard,
  BookOpen,
  Calendar,
  Search,
  RefreshCcw,
} from "lucide-react";
import { toast } from "sonner";

type ReportType = "users" | "subscriptions" | "requests" | "tests" | "pdfs";

interface ReportResult {
  reportType: string;
  dateRange: { startDate: string | null; endDate: string | null };
  results: any[];
  count: number;
}

const reportTypes: { value: ReportType; label: string; icon: any }[] = [
  { value: "users", label: "Users", icon: Users },
  { value: "subscriptions", label: "Subscriptions", icon: CreditCard },
  { value: "requests", label: "User Requests", icon: BookOpen },
  { value: "tests", label: "Test Results", icon: BookOpen },
  { value: "pdfs", label: "PDF Files", icon: FileText },
];

export default function AnalyticsReportsPage() {
  const [reportType, setReportType] = useState<ReportType>("users");
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 90);
    return d.toISOString().split("T")[0];
  });
  const [endDate, setEndDate] = useState(
    () => new Date().toISOString().split("T")[0]
  );
  const [report, setReport] = useState<ReportResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchReport = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const params = new URLSearchParams({
        type: reportType,
        startDate,
        endDate,
      });
      const data = await apiClient.get<{ data: ReportResult }>(
        `/api/admin/analytics/reports?${params.toString()}`
      );
      setReport(data.data);
    } catch (err) {
      console.error("Failed to fetch report:", err);
      setError("Failed to load report data");
      toast.error("Failed to load report");
    } finally {
      setIsLoading(false);
    }
  }, [reportType, startDate, endDate]);

  useEffect(() => {
    fetchReport();
  }, [fetchReport]);

  const formatDate = (dateString: string) => {
    if (!dateString) return "—";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const exportCSV = () => {
    if (!report || report.results.length === 0) return;

    const headers = Object.keys(report.results[0]).filter(
      (k) => !k.startsWith("_")
    );
    const csvContent = [
      headers.join(","),
      ...report.results.map((row) =>
        headers
          .map((h) => {
            const val = row[h];
            if (val === null || val === undefined) return "";
            if (typeof val === "object") return `"${JSON.stringify(val)}"`;
            return `"${String(val).replace(/"/g, '""')}"`;
          })
          .join(",")
      ),
    ].join("\n");

    const blob = new Blob([csvContent], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${reportType}-report-${startDate}-to-${endDate}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success("Report exported successfully");
  };

  const renderReportTable = () => {
    if (!report || report.results.length === 0) {
      return (
        <div className="text-center py-12 text-muted-foreground">
          <FileText className="h-12 w-12 mx-auto mb-4 opacity-30" />
          <p>No data found for the selected criteria</p>
        </div>
      );
    }

    switch (reportType) {
      case "users":
        return (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Phone</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Role</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Tests</TableHead>
                <TableHead>Sessions</TableHead>
                <TableHead>Joined</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {report.results.map((user: any) => (
                <TableRow key={user.id}>
                  <TableCell className="font-medium">
                    {user.firstName} {user.lastName}
                  </TableCell>
                  <TableCell>{user.phoneNumber}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {user.email || "—"}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">
                      {user.role?.roleName || "—"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={user.isActive ? "default" : "secondary"}
                    >
                      {user.isActive ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell>{user._count?.testAttempts ?? 0}</TableCell>
                  <TableCell>
                    {user._count?.readingSessions ?? 0}
                  </TableCell>
                  <TableCell>{formatDate(user.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        );

      case "subscriptions":
        return (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>User</TableHead>
                <TableHead>Plan</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Date</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {report.results.map((tx: any) => (
                <TableRow key={tx.id}>
                  <TableCell className="font-medium">
                    {tx.user?.firstName} {tx.user?.lastName}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">
                      {tx.subscription?.planName || "—"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {tx.subscription?.amount
                      ? `RWF ${tx.subscription.amount.toLocaleString()}`
                      : "—"}
                  </TableCell>
                  <TableCell>{formatDate(tx.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        );

      case "requests":
        return (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>User</TableHead>
                <TableHead>Phone</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Tests</TableHead>
                <TableHead>Days</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Requested</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {report.results.map((req: any) => (
                <TableRow key={req.id}>
                  <TableCell className="font-medium">
                    {req.user?.firstName} {req.user?.lastName}
                  </TableCell>
                  <TableCell>{req.user?.phoneNumber || "—"}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {req.user?.email || "—"}
                  </TableCell>
                  <TableCell>{req.requestedTests}</TableCell>
                  <TableCell>{req.requestedDays}</TableCell>
                  <TableCell>
                    <Badge
                      variant={
                        req.status === "ACCEPTED"
                          ? "default"
                          : req.status === "REJECTED"
                            ? "destructive"
                            : "secondary"
                      }
                    >
                      {req.status}
                    </Badge>
                  </TableCell>
                  <TableCell>{formatDate(req.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        );

      case "tests":
        return (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>User</TableHead>
                <TableHead>Test</TableHead>
                <TableHead>Marks</TableHead>
                <TableHead>Pass Marks</TableHead>
                <TableHead>Date</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {report.results.map((result: any) => (
                <TableRow key={result.id}>
                  <TableCell className="font-medium">
                    {result.user?.firstName} {result.user?.lastName}
                  </TableCell>
                  <TableCell>{result.test?.title || "—"}</TableCell>
                  <TableCell>{result.marks ?? "—"}</TableCell>
                  <TableCell>{result.test?.passMarks ?? "—"}</TableCell>
                  <TableCell>{formatDate(result.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        );

      case "pdfs":
        return (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Title</TableHead>
                <TableHead>Author</TableHead>
                <TableHead>Public</TableHead>
                <TableHead>Views</TableHead>
                <TableHead>Bookmarks</TableHead>
                <TableHead>Ratings</TableHead>
                <TableHead>Uploaded</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {report.results.map((pdf: any) => (
                <TableRow key={pdf.id}>
                  <TableCell className="font-medium">
                    {pdf.title}
                  </TableCell>
                  <TableCell>{pdf.author || "—"}</TableCell>
                  <TableCell>
                    <Badge
                      variant={pdf.isPublic ? "default" : "secondary"}
                    >
                      {pdf.isPublic ? "Public" : "Private"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {pdf._count?.readingSessions ?? 0}
                  </TableCell>
                  <TableCell>{pdf._count?.bookmarks ?? 0}</TableCell>
                  <TableCell>{pdf._count?.ratings ?? 0}</TableCell>
                  <TableCell>{formatDate(pdf.uploadedAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        );

      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Reports</h1>
          <p className="text-muted-foreground">
            Generate and export detailed analytics reports
          </p>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Report Filters</CardTitle>
          <CardDescription>
            Select report type and date range
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            <div className="space-y-2">
              <Label>Report Type</Label>
              <Select
                value={reportType}
                onValueChange={(v) => setReportType(v as ReportType)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {reportTypes.map((rt) => (
                    <SelectItem key={rt.value} value={rt.value}>
                      <div className="flex items-center gap-2">
                        <rt.icon className="h-4 w-4" />
                        {rt.label}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Start Date</Label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>End Date</Label>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
            <div className="space-y-2 flex items-end gap-2">
              <Button onClick={fetchReport} disabled={isLoading} className="flex-1">
                {isLoading ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : (
                  <RefreshCcw className="h-4 w-4 mr-2" />
                )}
                Generate
              </Button>
              <Button
                variant="outline"
                onClick={exportCSV}
                disabled={!report || report.results.length === 0}
              >
                <Download className="h-4 w-4 mr-2" />
                CSV
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Report Results */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="capitalize">
                {reportType} Report
              </CardTitle>
              <CardDescription>
                {report
                  ? `${report.count} records found`
                  : "Loading..."}
              </CardDescription>
            </div>
            {report && report.count > 0 && (
              <Badge variant="secondary">
                {report.count} records
              </Badge>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {error ? (
            <div className="text-center py-12 text-red-600">
              <p>{error}</p>
              <Button
                variant="outline"
                onClick={fetchReport}
                className="mt-4"
              >
                Retry
              </Button>
            </div>
          ) : isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <div className="overflow-x-auto">{renderReportTable()}</div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
