"use client";

import { Fragment, useEffect, useState } from "react";
import { apiClient } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
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
import {
  Activity,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Download,
  FilterX,
  Inbox,
  Loader2,
  RefreshCcw,
  ScrollText,
  Search,
  ShieldAlert,
  Users,
} from "lucide-react";

interface AuditUser {
  id: number;
  firstName: string;
  lastName: string | null;
  email: string | null;
  role: { roleName: string } | null;
}

interface AuditEntry {
  id: number;
  activityType: string;
  description: string;
  createdAt: string;
  updatedAt: string;
  user: AuditUser;
}

interface Pagination {
  page: number;
  pageSize: number;
  total: number;
  pages: number;
}

interface Meta {
  types: { activityType: string; count: number }[];
  stats: {
    total: number;
    today: number;
    uniqueUsers: number;
    topAction: string | null;
  };
}

const EMPTY_PAGINATION: Pagination = {
  page: 1,
  pageSize: 25,
  total: 0,
  pages: 1,
};

/** Bucket an activity type into a display category with a colour. */
function categoryFor(activityType: string): {
  label: string;
  className: string;
} {
  const t = activityType.toLowerCase();
  if (/(password|login|logout|session|otp|account)/.test(t)) {
    return { label: "Access", className: "bg-amber-50 text-amber-700 border-amber-200" };
  }
  if (/(role|permission|user_)/.test(t) || t.startsWith("user")) {
    return { label: "Users", className: "bg-sky-50 text-sky-700 border-sky-200" };
  }
  if (/subscription/.test(t)) {
    return { label: "Subscription", className: "bg-blue-50 text-blue-700 border-blue-200" };
  }
  if (/(material|pdf|test|question|content|file|folder)/.test(t)) {
    return { label: "Content", className: "bg-purple-50 text-purple-700 border-purple-200" };
  }
  if (/notification/.test(t)) {
    return { label: "Notification", className: "bg-cyan-50 text-cyan-700 border-cyan-200" };
  }
  if (/(system|setting|data|export|deletion)/.test(t)) {
    return { label: "System", className: "bg-green-50 text-green-700 border-green-200" };
  }
  return { label: "General", className: "bg-gray-50 text-gray-700 border-gray-200" };
}

function formatTimestamp(dateString: string) {
  return new Date(dateString).toLocaleString("en-US", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

export default function AuditLogPage() {
  const [entries, setEntries] = useState<AuditEntry[]>([]);
  const [pagination, setPagination] = useState<Pagination>(EMPTY_PAGINATION);
  const [meta, setMeta] = useState<Meta | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  // Filters
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [activityType, setActivityType] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  // Debounce the free-text search so typing doesn't fire a query per keystroke.
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(1);
    }, 400);
    return () => clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setIsLoading(true);
      setError(null);
      try {
        const params = new URLSearchParams();
        params.set("page", String(page));
        params.set("pageSize", String(pageSize));
        if (search) params.set("search", search);
        if (activityType) params.set("activityType", activityType);
        if (from) params.set("from", from);
        if (to) params.set("to", to);

        const response = await apiClient.get<{
          data: AuditEntry[];
          pagination: Pagination;
          meta: Meta;
        }>(`/api/admin/audit-log?${params.toString()}`);

        if (cancelled) return;
        setEntries(response.data || []);
        setPagination(response.pagination || EMPTY_PAGINATION);
        setMeta(response.meta || null);
      } catch (err) {
        if (cancelled) return;
        setError(
          err instanceof Error && err.message
            ? err.message
            : "Failed to load the audit log",
        );
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [page, pageSize, search, activityType, from, to, refreshKey]);

  const hasFilters = Boolean(search || activityType || from || to);

  const clearFilters = () => {
    setSearchInput("");
    setSearch("");
    setActivityType("");
    setFrom("");
    setTo("");
    setPage(1);
  };

  const exportCsv = () => {
    const escape = (value: unknown) =>
      `"${String(value ?? "").replace(/"/g, '""')}"`;
    const header = [
      "ID",
      "Timestamp",
      "User",
      "Email",
      "Role",
      "Action",
      "Description",
    ];
    const lines = [header.map(escape).join(",")];
    for (const entry of entries) {
      lines.push(
        [
          entry.id,
          new Date(entry.createdAt).toISOString(),
          `${entry.user.firstName} ${entry.user.lastName ?? ""}`.trim(),
          entry.user.email ?? "",
          entry.user.role?.roleName ?? "",
          entry.activityType,
          entry.description,
        ]
          .map(escape)
          .join(","),
      );
    }
    const blob = new Blob([lines.join("\n")], {
      type: "text/csv;charset=utf-8",
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `audit-log-page${pagination.page}-${new Date()
      .toISOString()
      .slice(0, 10)}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  /** Compact page-number window around the current page. */
  const pageWindow = (): number[] => {
    const { page: current, pages: totalPages } = pagination;
    const start = Math.max(1, Math.min(current - 2, totalPages - 4));
    const end = Math.min(totalPages, start + 4);
    const numbers: number[] = [];
    for (let i = start; i <= end; i++) numbers.push(i);
    return numbers;
  };

  const rangeStart =
    pagination.total === 0
      ? 0
      : (pagination.page - 1) * pagination.pageSize + 1;
  const rangeEnd = Math.min(
    pagination.page * pagination.pageSize,
    pagination.total,
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Audit Log</h1>
          <p className="text-muted-foreground">
            Every recorded action across the admin console — searchable, filterable and exportable.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={exportCsv}
            disabled={entries.length === 0}
          >
            <Download className="mr-2 h-4 w-4" />
            Export CSV
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setRefreshKey((key) => key + 1)}
            disabled={isLoading}
          >
            <RefreshCcw className={cn("mr-2 h-4 w-4", isLoading && "animate-spin")} />
            Refresh
          </Button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Matching Events</CardTitle>
            <Activity className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {isLoading ? "…" : pagination.total.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              under the current filters
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Events Today</CardTitle>
            <RefreshCcw className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {isLoading || !meta ? "…" : meta.stats.today.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              recorded so far today
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Users</CardTitle>
            <Users className="h-4 w-4 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {isLoading || !meta ? "…" : meta.stats.uniqueUsers.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              unique actors in this view
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Top Action</CardTitle>
            <ScrollText className="h-4 w-4 text-orange-600" />
          </CardHeader>
          <CardContent>
            <div className="text-lg font-bold truncate">
              {isLoading || !meta ? "…" : meta.stats.topAction || "—"}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              most frequent event type
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col gap-3 md:flex-row md:items-end">
            <div className="flex-1 space-y-1">
              <label className="text-sm font-medium">Search</label>
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Description, action, user name or email…"
                  className="pl-8"
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                />
              </div>
            </div>
            <div className="space-y-1 md:w-56">
              <label className="text-sm font-medium">Action type</label>
              <Select
                value={activityType || "all"}
                onValueChange={(value) => {
                  setActivityType(value === "all" ? "" : value);
                  setPage(1);
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All actions" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">
                    All actions
                    {meta ? ` (${meta.types.length})` : ""}
                  </SelectItem>
                  {(meta?.types || []).map((type) => (
                    <SelectItem key={type.activityType} value={type.activityType}>
                      {type.activityType} ({type.count})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <label className="text-sm font-medium">From</label>
              <Input
                type="date"
                value={from}
                max={to || undefined}
                onChange={(event) => {
                  setFrom(event.target.value);
                  setPage(1);
                }}
              />
            </div>
            <div className="space-y-1">
              <label className="text-sm font-medium">To</label>
              <Input
                type="date"
                value={to}
                min={from || undefined}
                onChange={(event) => {
                  setTo(event.target.value);
                  setPage(1);
                }}
              />
            </div>
            {hasFilters && (
              <Button variant="ghost" size="sm" onClick={clearFilters}>
                <FilterX className="mr-2 h-4 w-4" />
                Clear
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Log table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ScrollText className="h-5 w-5" />
            Activity
          </CardTitle>
          <p className="text-sm text-muted-foreground">
            {isLoading
              ? "Loading…"
              : `Showing ${rangeStart.toLocaleString()}–${rangeEnd.toLocaleString()} of ${pagination.total.toLocaleString()} events`}
          </p>
        </CardHeader>
        <CardContent>
          {error ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <ShieldAlert className="h-10 w-10 text-red-500 mb-3" />
              <p className="text-sm font-medium">{error}</p>
              <Button
                variant="outline"
                size="sm"
                className="mt-4"
                onClick={() => setRefreshKey((key) => key + 1)}
              >
                Try again
              </Button>
            </div>
          ) : isLoading && entries.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
              <p className="text-sm text-muted-foreground mt-3">Loading audit log…</p>
            </div>
          ) : entries.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <Inbox className="h-10 w-10 text-muted-foreground mb-3" />
              <p className="text-sm font-medium">No audit entries found</p>
              <p className="text-sm text-muted-foreground mt-1">
                {hasFilters
                  ? "Try adjusting or clearing the filters."
                  : "Actions performed in the admin console will appear here."}
              </p>
              {hasFilters && (
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-4"
                  onClick={clearFilters}
                >
                  <FilterX className="mr-2 h-4 w-4" />
                  Clear filters
                </Button>
              )}
            </div>
          ) : (
            <>
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[190px]">Timestamp</TableHead>
                      <TableHead>User</TableHead>
                      <TableHead className="w-[240px]">Action</TableHead>
                      <TableHead>Description</TableHead>
                      <TableHead className="w-[40px]" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {entries.map((entry) => {
                      const category = categoryFor(entry.activityType);
                      const isExpanded = expandedId === entry.id;
                      const consoleRole = entry.user.role?.roleName;
                      const isConsoleUser = Boolean(
                        consoleRole &&
                          ["admin", "superadmin"].includes(
                            consoleRole.toLowerCase().replace(/[\s_-]+/g, ""),
                          ),
                      );
                      return (
                        <Fragment key={entry.id}>
                          <TableRow
                            className="cursor-pointer"
                            onClick={() =>
                              setExpandedId(isExpanded ? null : entry.id)
                            }
                          >
                            <TableCell className="whitespace-nowrap text-sm">
                              {formatTimestamp(entry.createdAt)}
                            </TableCell>
                            <TableCell>
                              <div className="text-sm font-medium">
                                {entry.user.firstName} {entry.user.lastName}
                              </div>
                              <div className="text-xs text-muted-foreground">
                                {entry.user.email || `user #${entry.user.id}`}
                                {consoleRole ? ` · ${consoleRole}` : ""}
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="flex flex-wrap items-center gap-1.5">
                                <Badge
                                  variant="outline"
                                  className={cn("text-xs font-medium", category.className)}
                                >
                                  {category.label}
                                </Badge>
                                {isConsoleUser && (
                                  <Badge
                                    variant="outline"
                                    className="text-xs bg-indigo-50 text-indigo-700 border-indigo-200"
                                  >
                                    Console
                                  </Badge>
                                )}
                              </div>
                              <code className="text-xs text-muted-foreground">
                                {entry.activityType}
                              </code>
                            </TableCell>
                            <TableCell
                              className="max-w-[360px] truncate text-sm text-muted-foreground"
                              title={entry.description}
                            >
                              {entry.description}
                            </TableCell>
                            <TableCell className="text-right">
                              <ChevronDown
                                className={cn(
                                  "h-4 w-4 text-muted-foreground transition-transform",
                                  isExpanded && "rotate-180",
                                )}
                              />
                            </TableCell>
                          </TableRow>
                          {isExpanded && (
                            <TableRow key={`${entry.id}-detail`}>
                              <TableCell colSpan={5} className="bg-muted/40">
                                <div className="grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
                                  <div>
                                    <div className="text-xs font-medium uppercase text-muted-foreground">
                                      Event ID
                                    </div>
                                    <div className="font-mono">#{entry.id}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs font-medium uppercase text-muted-foreground">
                                      Actor
                                    </div>
                                    <div>
                                      {entry.user.firstName} {entry.user.lastName} (
                                      {entry.user.email || `user #${entry.user.id}`})
                                    </div>
                                  </div>
                                  <div>
                                    <div className="text-xs font-medium uppercase text-muted-foreground">
                                      Role
                                    </div>
                                    <div>{consoleRole || "—"}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs font-medium uppercase text-muted-foreground">
                                      Recorded
                                    </div>
                                    <div>{new Date(entry.createdAt).toISOString()}</div>
                                  </div>
                                  <div className="sm:col-span-2 lg:col-span-4">
                                    <div className="text-xs font-medium uppercase text-muted-foreground">
                                      Full description
                                    </div>
                                    <div className="whitespace-pre-wrap">
                                      {entry.description}
                                    </div>
                                  </div>
                                </div>
                              </TableCell>
                            </TableRow>
                          )}
                        </Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>

              {/* Pagination */}
              <div className="mt-4 flex flex-col items-center justify-between gap-3 sm:flex-row">
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <span>Rows per page</span>
                  <Select
                    value={String(pagination.pageSize)}
                    onValueChange={(value) => {
                      setPageSize(Number(value));
                      setPage(1);
                    }}
                  >
                    <SelectTrigger className="h-8 w-[70px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {[10, 25, 50, 100].map((size) => (
                        <SelectItem key={size} value={String(size)}>
                          {size}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex items-center gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={pagination.page <= 1 || isLoading}
                    onClick={() => setPage((value) => Math.max(1, value - 1))}
                  >
                    <ChevronLeft className="h-4 w-4" />
                    Prev
                  </Button>
                  {pageWindow().map((number) => (
                    <Button
                      key={number}
                      variant={number === pagination.page ? "default" : "outline"}
                      size="sm"
                      className="w-9"
                      disabled={isLoading}
                      onClick={() => setPage(number)}
                    >
                      {number}
                    </Button>
                  ))}
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={pagination.page >= pagination.pages || isLoading}
                    onClick={() =>
                      setPage((value) => Math.min(pagination.pages, value + 1))
                    }
                  >
                    Next
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
