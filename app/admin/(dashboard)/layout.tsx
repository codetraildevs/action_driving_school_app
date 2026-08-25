"use client";

import { Sidebar } from "@/components/admin/sidebar";
import { ProtectedRoute } from "@/components/admin/protected-route";
import {
  useState,
  useEffect,
  useRef,
  useOptimistic,
  startTransition,
} from "react";
import { Button } from "@/components/ui/button";
import {
  BellIcon,
  Menu, Check,
  X,
  RefreshCcw,
  Loader2,
  UserCircle2,
  LogOut, ChevronLeft,
  ChevronRight,
  Home,
  Users,
  Settings,
  FileText,
  ChevronDown
} from "lucide-react";
import { getPendingSubscriptionRequests } from "@/app/actions/pendingRequests";
import { Badge } from "@/components/ui/badge";
import {
  UserSubscriptionRequest,
  User,
  UserRequestStatus,
  Language,
} from "@/lib/generated/prisma";
import { apiClient } from "@/lib/api-client";
import { toast } from "sonner";
import Link from "next/link";
import { differenceInDays } from "date-fns";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth/auth-context";

// Sheet imports
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";

interface userWithLanguage extends User{
  language:Language,
  Pendinglanguage:Language

}
interface SubscriptionRequest extends UserSubscriptionRequest {
  user: userWithLanguage;
  pending: false | boolean;
}

interface GroupedRequests {
  userId: number;
  user: userWithLanguage;
  requests: SubscriptionRequest[];
  totalTests: number;
  oldestRequestDate: Date;
}

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const [requests, setRequests] = useState<SubscriptionRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const { logout } = useAuth();
  const [sidebarVisible, setSidebarVisible] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [optimisticRequests, updateOptmisticRequest] = useOptimistic(
    requests,
    (requests, updatedRequest: SubscriptionRequest) => {
      return requests.map((req) =>
        req.id === updatedRequest.id ? { ...updatedRequest } : req
      );
    }
  );

  const [visible, setVisible] = useState(true);
  const lastScrollY = useRef(0);
  const ticking = useRef(false);

  const pathname = usePathname();

  const handleLogout = () => {
    logout();
  };

  // Group requests by user
  const groupedRequests: GroupedRequests[] = Object.values(
    optimisticRequests.reduce((acc, req) => {
      const userId = req.userId;
      if (!acc[userId]) {
        acc[userId] = {
          userId,
          user: req.user,
          requests: [],
          totalTests: 0,
          oldestRequestDate: req.createdAt,
        };
      }
      acc[userId].requests.push(req);
      acc[userId].totalTests += req.requestedTests;
      if (new Date(req.createdAt) < new Date(acc[userId].oldestRequestDate)) {
        acc[userId].oldestRequestDate = req.createdAt;
      }
      return acc;
    }, {} as Record<number, GroupedRequests>)
  );

  const fetchPendingRequests = async () => {
    setLoading(true);
    try {
      const result = (await getPendingSubscriptionRequests()) as any;
      if (result.success) {
        setRequests(result.data || []);
      }
    } catch (error) {
      console.error("Failed to fetch requests:", error);
      toast.error("Failed to load requests");
    } finally {
      setLoading(false);
    }
  };
  const handleDaysChange = (reqId: number, days: number) => {
    const requestToChange = requests.find((r) => r.id == reqId);
    if (requestToChange) {
      setRequests([
        ...requests.map((req) => {
          if (req.id == reqId) {
            return { ...req, requestedDays: days };
          } else {
            return req;
          }
        }),
      ]);
      startTransition(async () => {
        updateOptmisticRequest({
          ...requestToChange,
          requestedDays: days,
        });
      });
    }
  };

  const handleAcceptRequest = async (requestId: number, days: number) => {
    const request = requests.find((d) => d.id == requestId);
    if (!request) return;
    try {
      startTransition(async () => {
        updateOptmisticRequest({
          ...request,
          status: UserRequestStatus.ACCEPTED,
          pending: true,
        });

        const response = (await apiClient.patch(
          `/api/subscriptions/userRequests/${requestId}/accept`,
          { days }
        )) as any;

        startTransition(() => {
          setRequests((prev) => {
            return prev.filter((request) => request.id !== requestId);
          });

          toast.success("Subscription request accepted successfully");
        });
      });
    } catch (error) {
      toast.error("Failed to accept request");
      fetchPendingRequests();
    }
  };

  const handleRejectRequest = async (requestId: number) => {
    const request = requests.find((d) => d.id == requestId);
    if (!request) return;
    try {
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
          setRequests((prev) => {
            return prev.filter((request) => request.id !== requestId);
          });

          toast.success("Subscription request rejected successfully");
        });
      });
    } catch (error) {
      toast.error("Failed to reject request");
      fetchPendingRequests();
    }
  };

  console.log(optimisticRequests)

  useEffect(() => {
    const handleScroll = () => {
      if (!ticking.current) {
        window.requestAnimationFrame(() => {
          const currentScrollY = window.scrollY;
          if (currentScrollY > lastScrollY.current && currentScrollY > 100) {
            setVisible(false);
          } else if (currentScrollY < lastScrollY.current) {
            setVisible(true);
          }
          lastScrollY.current = currentScrollY;
          ticking.current = false;
        });
        ticking.current = true;
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  useEffect(() => {
    // Create EventSource connection
    const eventSource = new EventSource("/api/sse");

    eventSource.onopen = () => {
      console.log("SSE connection established");
    };

    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);

      // Ignore the initial "Connected!" handshake message
      if (!data.data) return;

      try {
        const pendingRequests = data.data;
        setRequests(pendingRequests || []);
        setLoading(false);
        document.title = `(${pendingRequests.length}) Admin Dashboard - Action Driving School`;
      } catch (error) {
        console.error("Failed to check notifications:", error);
      }
    };
    eventSource.onerror = (error) => {
      console.error("SSE error:", error);

      eventSource.close();
    };

    // Cleanup on unmount
    return () => {
      eventSource.close();
    };
  }, []);

  // SSE handles real-time updates. Initial fetch is a fallback in case SSE is slow.
  useEffect(() => {
    const timer = setTimeout(() => {
      if (requests.length === 0) {
        fetchPendingRequests();
      }
    }, 8000);
    return () => clearTimeout(timer);
  }, []);

  // Mobile bottom nav items
  const navItems = [
    { href: "/admin/dashboard", label: "Dashboard", icon: Home },
    { href: "/admin/users", label: "Users", icon: Users },
    { href: "/admin/user-requests", label: "Requests", icon: FileText },
    { href: "/admin/settings", label: "Profile", icon: UserCircle2 },
  ];

  return (
    <ProtectedRoute>
      <div className="flex h-screen overflow-hidden">
        {/* Desktop Sidebar */}
        {sidebarVisible && (
          <div className="hidden md:block transition-all duration-300">
            <Sidebar open={open} onOpen={setOpen} />
          </div>
        )}

        {/* Main Content */}
        <main className="flex-1 overflow-y-auto bg-background pb-20 md:pb-0">
          {/* Top Bar */}
          <div className="flex justify-between border-b h-15 items-center sticky top-0 z-30 bg-background backdrop-blur-sm bg-opacity-95">
            <div className="flex gap-2 md:hidden">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setOpen(!open)}
              >
                <Menu className="h-6 w-6" />
              </Button>
              <Button
                variant="ghost"
                onClick={() => {
                  setIsRefreshing(true);
                  window.location.reload();
                }}
              >
                {isRefreshing ? (
                  <Loader2 className="h-6 w-6 animate-spin" />
                ) : (
                  <RefreshCcw className="h-6 w-6" />
                )}
              </Button>
            </div>

            <Button
              variant="ghost"
              size="icon"
              className="hidden md:block mx-2"
              onClick={() => setSidebarVisible(!sidebarVisible)}
            >
              <Menu className="h-6 w-6" />
            </Button>

            <Badge variant="default" className="flex items-center gap-1">
              <ChevronLeft className="h-4 w-4" />
              <span>Admin</span>
              <ChevronRight className="h-4 w-4" />
            </Badge>

            <div className="flex items-center space-x-2">
              {/* NOTIFICATIONS - NOW USING SHEET FROM RIGHT */}
              <Sheet>
                <SheetTrigger asChild>
                  <Button variant="ghost" className="relative">
                    <BellIcon className="h-6 w-6" />
                    {requests.length > 0 && (
                      <Badge className="absolute -top-1 -right-1 h-5 w-5 p-0 text-xs flex items-center justify-center">
                        {requests.length}
                      </Badge>
                    )}
                  </Button>
                </SheetTrigger>

                <SheetContent
                  side="right"
                  className="p-0 w-80 transition-all duration-300"
                >
                  {/* Header */}
                  <div className="flex items-center justify-between p-6 border-b">
                    <h2 className="text-lg font-semibold">Pending Requests</h2>
                    <Badge variant="secondary" className="text-sm">
                      {optimisticRequests.length} new
                    </Badge>
                  </div>

                  {/* Body */}
                  <div className="flex-1 overflow-y-auto max-h-[82vh]">
                    {loading ? (
                      <div className="flex items-center justify-center h-64">
                        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                      </div>
                    ) : groupedRequests.length == 0 ? (
                      <div className="p-12 text-center">
                        <BellIcon className="h-16 w-16 mx-auto mb-4 text-muted-foreground/30" />
                        <p className="text-muted-foreground">
                          No pending requests
                        </p>
                        <p className="text-sm text-muted-foreground/70 mt-2">
                          You're all caught up!
                        </p>
                      </div>
                    ) : (
                      <div className="divide-y">
                        {groupedRequests.map((group) => (
                          <details
                            key={group.userId}
                            className="group"
                            open={group.requests.length === 1}
                          >
                            <summary className="p-4 hover:bg-accent/50 cursor-pointer transition-colors list-none">
                              <div className="flex items-center justify-between">
                                <div className="flex-1 min-w-0">
                                  <div className="flex items-center gap-2">
                                    <p className="font-bold text-base text-wrap">
                                      {group.user.firstName} {group.user.lastName} {group.user.phoneNumber}
                                    </p>
                                    <Badge variant="outline" className="text-xs">
                                      {group.requests.length} request{group.requests.length > 1 ? 's' : ''}
                                    </Badge>
                                  </div>
                                  <p className="text-sm text-muted-foreground truncate">
                                    {group.user.phoneNumber}
                                  </p>
                                  <p className="text-xs text-muted-foreground truncate">
                                    {group.user.email}
                                  </p>
                                  <p className="text-xs text-muted-foreground mt-1">
                                    Total Tests: {group.totalTests}
                                  </p>
                                  <p className="text-xs text-muted-foreground">
                                    {differenceInDays(
                                      new Date(),
                                      new Date(group.oldestRequestDate)
                                    ) === 0
                                      ? "Today"
                                      : `${differenceInDays(
                                          new Date(),
                                          new Date(group.oldestRequestDate)
                                        )} days ago`}
                                  </p>
                                </div>
                                <ChevronDown className="h-5 w-5 text-muted-foreground transition-transform group-open:rotate-180" />
                              </div>
                            </summary>

                            {/* Individual requests for this user */}
                            <div className="bg-muted/30">
                              {group.requests.map((req, index) => (
                                <div
                                  key={req.id}
                                  className={`p-4 px-3 ${
                                    index !== group.requests.length - 1 ? 'border-b border-border/50' : ''
                                  }`}
                                >
                                  <div className="flex items-center flex-col border rounded-md bg-background justify-between gap-2 py-3 px-2">
                                    <div className="flex-1 w-full min-w-0 flex flex-col items-center">
                                      <div className="flex justify-between w-full px-2 items-center mb-2">
                                        <Badge variant="secondary" className="text-xs">
                                          Request #{index + 1}
                                        </Badge>
                                        {req.pending && (
                                          <Loader2 className="h-4 w-4 animate-spin" />
                                        )}
                                      </div>
                                      
                                      <p className="text-sm text-muted-foreground truncate">
                                        Language: {req.user.Pendinglanguage.nativeName}
                                      </p>
                                      <p className="text-sm font-medium mt-1">
                                        Tests: {req.requestedTests}
                                      </p>
                                      
                                      <div className="w-full px-4 mt-2">
                                        <Label htmlFor={`days-${req.id}`} className="text-xs">
                                          Days
                                        </Label>
                                        <Input
                                          id={`days-${req.id}`}
                                          type="number"
                                          min={0}
                                          value={
                                            !isNaN(req.requestedDays)
                                              ? req.requestedDays
                                              : 0
                                          }
                                          onChange={(e) => {
                                            handleDaysChange(
                                              req.id,
                                              parseInt(e.target.value)
                                            );
                                          }}
                                          className="mt-1"
                                        />
                                      </div>
                                      
                                      <p className="text-xs text-muted-foreground mt-2">
                                        {differenceInDays(
                                          new Date(),
                                          new Date(req.createdAt)
                                        ) === 0
                                          ? "Requested today"
                                          : `Requested ${differenceInDays(
                                              new Date(),
                                              new Date(req.createdAt)
                                            )} days ago`}
                                      </p>
                                    </div>

                                    <div className="flex gap-2 shrink-0 mt-2">
                                      <Button
                                        size="sm"
                                        onClick={() => {
                                          const days = requests.find(
                                            (request) => request.id == req.id
                                          )?.requestedDays;
                                          handleAcceptRequest(
                                            req.id,
                                            days ? days : 0
                                          );
                                        }}
                                        disabled={req.pending}
                                      >
                                        <Check className="h-4 w-4 mr-1" /> Accept
                                      </Button>
                                      <Button
                                        size="sm"
                                        disabled={req.pending}
                                        variant="destructive"
                                        onClick={() => handleRejectRequest(req.id)}
                                      >
                                        <X className="h-4 w-4 mr-1" /> Reject
                                      </Button>
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          </details>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Footer */}
                  <div className="border-t p-4 bg-muted/40">
                    <Button
                      variant="outline"
                      className="w-full"
                      onClick={fetchPendingRequests}
                      disabled={loading}
                    >
                      {loading ? (
                        <>
                          <Loader2 className="h-4 w-4 animate-spin mr-2" />
                          Loading...
                        </>
                      ) : (
                        <>
                          <RefreshCcw className="h-4 w-4 mr-2" />
                          Refresh Requests
                        </>
                      )}
                    </Button>
                  </div>
                </SheetContent>
              </Sheet>

              {/* User Menu */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost">
                    <UserCircle2 size={32} />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>Admin Panel</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <Link href="/admin/settings">
                    <DropdownMenuItem>
                      <Settings className="mr-2 h-4 w-4" /> Profile
                    </DropdownMenuItem>
                  </Link>
                  <DropdownMenuItem onClick={handleLogout}>
                    <LogOut className="mr-2 h-4 w-4" /> Logout
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>

          {/* Page Content */}
          <div className="container mx-auto px-4 pt-4">{children}</div>
        </main>

        {/* Mobile Bottom Navigation */}
        <nav
          className={`md:hidden fixed bottom-0 left-0 right-0 z-50 transition-transform duration-300 ${
            visible ? "translate-y-0" : "translate-y-full"
          } bg-white border-t border-gray-200 shadow-2xl`}
        >
          <div className="flex justify-around items-center h-16 bg-gradient-to-t from-gray-50 to-white">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive =
                pathname === item.href || pathname.startsWith(item.href + "/");

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex flex-col items-center justify-center flex-1 h-full transition-all ${
                    isActive ? "text-primary" : "text-gray-500"
                  }`}
                >
                  <div className="relative">
                    <Icon
                      className={`h-6 w-6 transition-transform ${
                        isActive ? "scale-110" : ""
                      }`}
                    />
                    {isActive && (
                      <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 bg-primary rounded-full" />
                    )}
                  </div>
                  <span
                    className={`text-xs mt-1 font-medium ${
                      isActive ? "font-bold" : ""
                    }`}
                  >
                    {item.label}
                  </span>
                </Link>
              );
            })}
          </div>
        </nav>
      </div>
    </ProtectedRoute>
  );
}