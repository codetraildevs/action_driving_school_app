"use client";

import {
  useCallback,
  useEffect,
  useOptimistic,
  useRef,
  startTransition,
  useState,
} from "react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  RefreshCw,
  Loader2,
  CheckCircle,
  XCircle,
  Copy,
  User,
  Calendar,
  FileText,
  Search,
} from "lucide-react";
import { toast } from "sonner";
import { apiClient } from "@/lib/api-client";
import { Language, UserRequestStatus } from "@/lib/generated/prisma";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { format } from "date-fns";
import { UserSubscriptionRequest as userRequestType } from "@/lib/generated/prisma";
import { Label } from "@/components/ui/label";

// Calculate days between dates
const getDaysDifference = (
  endDate: string | Date,
  startDate: string | Date
) => {
  const end = new Date(endDate).getTime();
  const start = new Date(startDate).getTime();
  return Math.floor((end - start) / (1000 * 60 * 60 * 24));
};

interface UserSubscriptionRequest extends userRequestType {
  user: {
    id: number;
    firstName: string;
    middleName?: string;
    lastName: string;
    email: string;
    phoneNumber:string;
    language:Language;
    Pendinglanguage:Language;
  };
  pending: false | boolean;
}

// Grouped data type
interface GroupedUserRequest {
  userId: number;
  userName: string;
  userEmail: string;
  
  userLanguage:string;
  requests: UserSubscriptionRequest[];
  pendingCount: number;
  acceptedCount: number;
  rejectedCount: number;
  latestRequestDate: Date;
}

export function UserSubscriptionRequests() {
  const [data, setData] = useState<UserSubscriptionRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Tracks whether the first page has ever loaded; after that, search
  // refetches use the subtle "Searching..." indicator instead of the spinner.
  const hasLoadedRef = useRef(false);
  const [selectedUser, setSelectedUser] = useState<GroupedUserRequest | null>(
    null
  );
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const statusConfig = {
    PENDING: { variant: "secondary" as const, label: "Pending" },
    ACCEPTED: { variant: "default" as const, label: "Accepted" },
    REJECTED: { variant: "destructive" as const, label: "Rejected" },
  };

  const [optimisticRequests, updateOptmisticRequest] = useOptimistic(
    data,
    (data, updatedRequest: UserSubscriptionRequest) => {
      return data.map((req) =>
        req.id === updatedRequest.id ? { ...updatedRequest } : req
      );
    }
  );
  // Paginated fetch: replace the list on page 1 / search change, append on
  // "Load more" (previously the API returned every request row at once).
  // Live search: only the first load shows the full-screen spinner; search
  // refetches keep the current list visible with a subtle "Searching..."
  // indicator so typing never blanks the page.
  const fetchRequests = useCallback(
    async (targetPage = 1, append = false) => {
      try {
        if (append) setIsLoadingMore(true);
        else if (hasLoadedRef.current) setIsSearching(true);
        else setIsLoading(true);
        const params = new URLSearchParams({
          page: String(targetPage),
          pageSize: "50",
        });
        if (debouncedSearch) params.set("search", debouncedSearch);

        const response = (await apiClient.get(
          `/api/subscriptions/userRequests?${params.toString()}`
        )) as any;

        if (response.success) {
          const list = response.data || [];
          setData((prev) => (append ? [...prev, ...list] : list));
          setTotalPages(response.totalPages || 1);
          setPage(targetPage);
        }
      } catch (error) {
        console.error("Error fetching subscription requests:", error);
        toast.error("Failed to load subscription requests");
      } finally {
        if (append) setIsLoadingMore(false);
        else {
          setIsLoading(false);
          setIsSearching(false);
          hasLoadedRef.current = true;
        }
        setIsRefreshing(false);
      }
    },
    [debouncedSearch]
  );

  // Debounce search input, then reload from page 1 (300ms for snappier
  // live search — fast enough to feel instant, slow enough to not spam the
  // server on every keystroke).
  useEffect(() => {
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      setDebouncedSearch(searchQuery.trim());
    }, 300);
    return () => {
      if (searchTimer.current) clearTimeout(searchTimer.current);
    };
  }, [searchQuery]);

  useEffect(() => {
    fetchRequests(1, false);
  }, [fetchRequests]);

  const handleRefresh = () => {
    setIsRefreshing(true);
    fetchRequests(1, false);
  };

  const handleAcceptRequest = async (requestId: number, days: number) => {
    const request = data.find((d) => d.id == requestId);
    if (!request) return;
    try {
      setIsDialogOpen(false);
      startTransition(async () => {
        updateOptmisticRequest({
          ...request,
          status: UserRequestStatus.ACCEPTED,
          pending: true,
        });
        await apiClient.patch(
          `/api/subscriptions/userRequests/${requestId}/accept`,
          { days }
        );

        startTransition(() => {
          setData((prev) => {
            const updatedData = prev.map((request) =>
              request.id === requestId
                ? { ...request, status: UserRequestStatus.ACCEPTED }
                : request
            );

            // Update selectedUser to keep dialog in sync
            if (selectedUser) {
              const updatedRequests = selectedUser.requests.map((request) =>
                request.id === requestId
                  ? {
                      ...request,
                      status: UserRequestStatus.ACCEPTED,
                      pending: false,
                    }
                  : request
              );
              setSelectedUser({
                ...selectedUser,
                requests: updatedRequests,
                pendingCount: Math.max(0, selectedUser.pendingCount - 1),
                acceptedCount: selectedUser.acceptedCount + 1,
              });
            }

            return updatedData;
          });

          toast.success("Subscription request accepted successfully");
        });
      });
    } catch (error) {
      toast.error("Failed to accept subscription request");
      fetchRequests();
    }
  };

  const handleRejectRequest = async (requestId: number) => {
    const request = data.find((d) => d.id == requestId);
    if (!request) return;
    try {
      setIsDialogOpen(false);
      startTransition(async () => {
        updateOptmisticRequest({
          ...request,
          status: UserRequestStatus.REJECTED,
          pending: true,
        });
        await apiClient.patch(
          `/api/subscriptions/userRequests/${requestId}/reject`
        );

        startTransition(() => {
          setData((prev) => {
            const updatedData = prev.map((request) =>
              request.id === requestId
                ? {
                    ...request,
                    status: UserRequestStatus.REJECTED,
                    pending: false,
                  }
                : request
            );

            // Update selectedUser to keep dialog in sync
            if (selectedUser) {
              const updatedRequests = selectedUser.requests.map((request) =>
                request.id === requestId
                  ? { ...request, status: UserRequestStatus.REJECTED }
                  : request
              );
              setSelectedUser({
                ...selectedUser,
                requests: updatedRequests,
                pendingCount: Math.max(0, selectedUser.pendingCount - 1),
                acceptedCount: selectedUser.acceptedCount + 1,
              });
            }

            return updatedData;
          });

          toast.success("Subscription request accepted successfully");
        });
      });
    } catch (error) {
      fetchRequests();
      toast.error("Failed to reject subscription request");
    }
  };

  // Group data by user
  const getGroupedData = (): GroupedUserRequest[] => {
    const grouped = optimisticRequests.reduce((acc, request) => {
      const userId = request.user.id;

      if (!acc[userId]) {
        acc[userId] = {
          userId,
          userName: `${request.user.firstName} ${
            request.user.middleName || ""
          } ${request.user.lastName} (${request.user.phoneNumber})`.trim(),
          userEmail: request.user.email,
          userLanguage: request?.user?.Pendinglanguage?.nativeName,
          requests: [],
          pendingCount: 0,
          acceptedCount: 0,
          rejectedCount: 0,
          latestRequestDate: request.createdAt,
        };
      }

      acc[userId].requests.push(request);

      // Track latest request date
      if (
        new Date(request.createdAt) > new Date(acc[userId].latestRequestDate)
      ) {
        acc[userId].latestRequestDate = request.createdAt;
      }

      if (request.status === "PENDING") acc[userId].pendingCount++;
      if (request.status === "ACCEPTED") acc[userId].acceptedCount++;
      if (request.status === "REJECTED") acc[userId].rejectedCount++;

      return acc;
    }, {} as Record<number, GroupedUserRequest>);

    return Object.values(grouped).sort(
      (a, b) =>
        new Date(b.latestRequestDate).getTime() -
        new Date(a.latestRequestDate).getTime()
    );
  };

  // Filter against the DEBOUNCED value (the one the server actually searched
  // with), so the client-side grouping never hides matches the server returned
  // — typing "jo" no longer filters only the currently loaded page.
  const getFilteredGroupedData = () => {
    const groupedData = getGroupedData();

    if (!debouncedSearch) return groupedData;

    const searchTerm = debouncedSearch.toLowerCase();
    return groupedData.filter((group) => {
      return (
        group.userName.toLowerCase().includes(searchTerm) ||
        group.userEmail.toLowerCase().includes(searchTerm)
      );
    });
  };

  const filteredGroupedData = getFilteredGroupedData();

  const handleUserClick = (group: GroupedUserRequest) => {
    setSelectedUser(group);
    setIsDialogOpen(true);
  };

  const handleCopyId = (requestId: number) => {
    navigator.clipboard.writeText(requestId.toString());
    toast("Request ID copied", {
      description: "Request ID has been copied successfully",
    });
  };

  const handleDaysChange = (reqId: number, days: number) => {
    if (!selectedUser) return;
    const requestToChange = selectedUser.requests.find((r) => r.id == reqId);
    if (requestToChange) {
      setSelectedUser({
        ...selectedUser,
        requests: selectedUser.requests.map((req) => {
          if (req.id === reqId) {
            return { ...req, requestedDays: days };
          } else {
            return req;
          }
        }),
      });
    }
  };

  // User Group Card Component for Mobile
  const UserGroupCard = ({ group }: { group: GroupedUserRequest }) => {
    return (
      <Card
        className="p-4 cursor-pointer hover:bg-accent/50 transition-colors"
        onClick={() => handleUserClick(group)}
      >
        <div className="space-y-3">
          <div className="flex items-start justify-between">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <User className="h-4 w-4 text-muted-foreground" />
                <h3 className="font-semibold text-lg">{group.userName}</h3>
              </div>
            </div>
            {optimisticRequests.find((req) => req.userId == group.userId)
              ?.pending && <Loader2 className="animate-spin" />}
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            <Badge variant="secondary" className="text-xs">
              {group.requests.length} request
              {group.requests.length !== 1 ? "s" : ""}
            </Badge>
            {group.pendingCount > 0 && (
              <Badge variant="secondary" className="text-xs">
                {group.pendingCount} pending
              </Badge>
            )}
            {group.acceptedCount > 0 && (
              <Badge variant="default" className="text-xs">
                {group.acceptedCount} accepted
              </Badge>
            )}
            {group.rejectedCount > 0 && (
              <Badge variant="destructive" className="text-xs">
                {group.rejectedCount} rejected
              </Badge>
            )}
          </div>

          <div className="text-xs text-muted-foreground">
            Latest:{" "}
            {format(new Date(group.latestRequestDate), "yyyy-MM-dd HH:mm:ss")}
          </div>
        </div>
      </Card>
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  return (
    <div className=" space-x-0 md:space-y-6">
      <div className="flex flex-col sticky top-15 z-10 bg-background gap-0 md:gap-4 mb-4 pt-4 gap-1">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="md:text-3xl text-xl font-bold tracking-tight">
              Subscription Requests
            </h2>
            <p className="text-muted-foreground hidden md:block">
              Manage user subscription requests and approvals
            </p>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            <Button
              variant="default"
              onClick={handleRefresh}
              disabled={isRefreshing}
            >
              <RefreshCw
                className={`h-4 w-4 ${isRefreshing ? "animate-spin" : ""}`}
              />
              <span className="hidden md:block">Refresh</span>
            </Button>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search users..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8"
            />
            {isSearching && (
              <div className="absolute right-2.5 top-2.5 flex items-center gap-1 text-xs text-muted-foreground">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Searching
              </div>
            )}
          </div>
        </div>
      </div>

      <Card className="border-none shadow-none w-full">
        <CardHeader className="p-0">
          <CardTitle>Users with Requests</CardTitle>
          <CardDescription>
            {debouncedSearch
              ? `Searching for "${debouncedSearch}" - ${
                  filteredGroupedData.length
                } user${filteredGroupedData.length !== 1 ? "s" : ""}`
              : `${filteredGroupedData.length} user${
                  filteredGroupedData.length !== 1 ? "s" : ""
                } with requests`}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {/* Desktop Table View */}
          <div className="hidden md:block">
            <div className="rounded-md border">
              <table className="w-full">
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                      User
                    </th>
                    
                     <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                      Language
                    </th>
                    <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                      Total Requests
                    </th>
                    <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                      Status Summary
                    </th>
                    <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                      Latest Request
                    </th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {filteredGroupedData.length === 0 ? (
                    <tr>
                      <td
                        colSpan={5}
                        className="h-24 text-center text-muted-foreground"
                      >
                        {debouncedSearch
                          ? "No users found matching your search."
                          : "No subscription requests found."}
                      </td>
                    </tr>
                  ) : (
                    filteredGroupedData.map((group) => (
                      <tr
                        key={group.userId}
                        className="border-b cursor-pointer hover:bg-accent/50 transition-colors"
                        onClick={() => handleUserClick(group)}
                      >
                        <td className="p-4 align-middle">
                          <div className="flex items-center gap-2 font-medium">
                            <User className="h-4 w-4 text-muted-foreground" />
                            {group.userName}
                          </div>
                        </td>
                       
                        <td className="p-4 align-middle">{group.userLanguage}</td>
                        <td className="p-4 align-middle">
                          <Badge variant="secondary">
                            {group.requests.length}
                          </Badge>
                        </td>
                        <td className="p-4 align-middle">
                          <div className="flex items-center gap-2 flex-wrap">
                            {group.pendingCount > 0 && (
                              <Badge variant="secondary" className="text-xs">
                                {group.pendingCount} pending
                              </Badge>
                            )}
                            {group.acceptedCount > 0 && (
                              <Badge variant="default" className="text-xs">
                                {group.acceptedCount} accepted
                              </Badge>
                            )}
                            {group.rejectedCount > 0 && (
                              <Badge variant="destructive" className="text-xs">
                                {group.rejectedCount} rejected
                              </Badge>
                            )}
                          </div>
                        </td>
                        <td className="p-4 align-middle text-muted-foreground">
                          {new Date(
                            group.latestRequestDate
                          ).toLocaleDateString()}
                        </td>
                        <td className="p-4 align-middle text-muted-foreground">
                          {optimisticRequests.find(
                            (req) => req.userId == group.userId
                          )?.pending && <Loader2 className="animate-spin" />}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Mobile Grid View */}
          <div className="grid grid-cols-1 gap-2 md:hidden max-h-[75vh] overflow-auto">
            {filteredGroupedData.length === 0 ? (
              <div className="text-center py-2 text-muted-foreground">
                {debouncedSearch
                  ? "No users found matching your search."
                  : "No subscription requests found."}
              </div>
            ) : (
              filteredGroupedData.map((group) => (
                <UserGroupCard key={group.userId} group={group} />
              ))
            )}
          </div>

          {page < totalPages && (
            <div className="flex justify-center py-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchRequests(page + 1, true)}
                disabled={isLoadingMore}
              >
                {isLoadingMore ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : null}
                Load more
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Single shared dialog for the clicked user (desktop table + mobile grid) */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
                          <DialogHeader>
                            <DialogTitle className="flex items-center gap-2">
                              <User className="h-5 w-5" />
                              {selectedUser?.userName}  ({selectedUser?.userLanguage})
                            </DialogTitle>
                             
                          </DialogHeader>

                          <div className="space-y-4 mt-4">
                            {selectedUser?.requests.map((request) => {
                              const config =
                                statusConfig[
                                  request.status as keyof typeof statusConfig
                                ] || statusConfig.PENDING;
                              const isPending =
                                request.status === UserRequestStatus.PENDING;

                              return (
                                <div className="relative" key={request.id}>
                                  {request.pending && (
                                    <div className="absolute inset-0 bg-background/20 backdrop-blur-sm z-10 rounded-lg flex items-center justify-center">
                                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                        <Loader2 className="h-4 w-4 animate-spin" />
                                      </div>
                                    </div>
                                  )}

                                  <Card className="p-4">
                                    <div className="space-y-3">
                                      <div className="flex items-start justify-between">
                                        <div className="space-y-1">
                                          <div className="flex items-center gap-2">
                                            <span className="text-sm text-muted-foreground">
                                              Request ID:
                                            </span>
                                            <span className="font-mono text-sm">
                                              {request.id}
                                            </span>
                                          </div>
                                        </div>
                                        <div className="flex items-center gap-2">
                                          <Badge variant={config.variant}>
                                            {config.label}
                                          </Badge>
                                        </div>
                                      </div>

                                      {request && (
                                        <div className="space-y-2">
                                          <div className="flex items-center gap-2 text-sm">
                                            <FileText className="h-4 w-4" />
                                            <span className="font-medium">
                                              Test Access Details
                                            </span>
                                          </div>
                                          <div className="grid grid-cols-2 gap-2 text-sm">
                                            <div>
                                              <span className="text-muted-foreground">
                                                Tests:{" "}
                                              </span>
                                              <span className="font-medium">
                                                {request.requestedTests || 0}
                                              </span>
                                            </div>
                                            <div>
                                              <span className="text-muted-foreground">
                                                Duration:{" "}
                                              </span>
                                              <span>
                                                {getDaysDifference(
                                                  request.requestedExpiresAt,
                                                  request.createdAt
                                                )}{" "}
                                                days
                                              </span>
                                            </div>
                                          </div>
                                        </div>
                                      )}

                                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                        <Calendar className="h-4 w-4" />
                                        <span>
                                          Requested:{" "}
                                          {format(
                                            new Date(request.createdAt),
                                            "yyyy-MM-dd HH:mm:ss"
                                          )}
                                        </span>
                                      </div>

                                      <div className="flex items-center gap-2 pt-2 flex-wrap">
                                        <Button
                                          variant="outline"
                                          size="sm"
                                          onClick={() =>
                                            handleCopyId(request.id)
                                          }
                                          className="text-blue-600 border-blue-200 hover:bg-blue-50 hover:text-blue-700"
                                        >
                                          <Copy className="h-4 w-4 mr-2" />
                                          Copy ID
                                        </Button>
                                        <div>
                                          <Label htmlFor="days">Days</Label>
                                          <Input
                                          disabled={!(request.status === UserRequestStatus.PENDING)}
                                            min={0}
                                            value={
                                              !isNaN(request.requestedDays)
                                                ? request.requestedDays
                                                : 0
                                            }
                                            onChange={(e) => {
                                              handleDaysChange(
                                                request.id,
                                                parseInt(e.target.value)
                                              );
                                            }}
                                          />
                                        </div>

                                        {isPending && (
                                          <>
                                            <Button
                                              variant="outline"
                                              size="sm"
                                              onClick={() => {
                                                const days =
                                                  selectedUser.requests.find(
                                                    (req) =>
                                                      req.id == request.id
                                                  )?.requestedDays;
                                                handleAcceptRequest(
                                                  request.id,
                                                  days ? days : 0
                                                );
                                              }}
                                              disabled={request.pending}
                                              className="text-green-600 border-green-200 hover:bg-green-50 hover:text-green-700"
                                            >
                                              <CheckCircle className="h-4 w-4 mr-2" />
                                              Accept
                                            </Button>
                                            <Button
                                              variant="outline"
                                              size="sm"
                                              onClick={() =>
                                                handleRejectRequest(request.id)
                                              }
                                              disabled={request.pending}
                                              className="text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700"
                                            >
                                              <XCircle className="h-4 w-4 mr-2" />
                                              Reject
                                            </Button>
                                          </>
                                        )}
                                      </div>
                                    </div>
                                  </Card>
                                </div>
                              );
                            })}
                          </div>
                        </DialogContent>
                      </Dialog>
    </div>
  );
}
