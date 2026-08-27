"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiClient } from "@/lib/api-client";
import RequestCard from "@/components/admin/RequestCard";
import RequestDetailsDialog from "@/components/admin/RequestDetailsDialog";
import { AdminRequest } from "@/types/request";
import UserSidebar from "@/components/admin/usersSidebar";
import { User } from "@/lib/generated/prisma";
import { ArrowLeft, Loader2, Scroll, Search } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { toast } from "sonner";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ScrollArea, ScrollBar } from "@/components/ui/scroll-area";

export default function AdminRequestsPage() {
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [requests, setRequests] = useState<AdminRequest[]>([]);
  const [selectedRequest, setSelectedRequest] = useState<AdminRequest | null>(
    null
  );
  const itemRefs = useRef(new Map());
  
  // Single ref to track which item to scroll to
  const pendingScrollId = useRef<string | null>(null);

  const [open, setOpen] = useState(false);
  const [currentUserColor, setCurrentUserColor] = useState<string>("");
  const [isLoadingRequests, setIsLoadingRequests] = useState(false);

  // Per-user request pagination state
  const [requestsPage, setRequestsPage] = useState(1);
  const [requestsTotal, setRequestsTotal] = useState(0);
  const [requestsTotalPages, setRequestsTotalPages] = useState(1);
  const [isLoadingMoreRequests, setIsLoadingMoreRequests] = useState(false);

  // Delete confirmation dialog state
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [requestToDelete, setRequestToDelete] = useState<AdminRequest | null>(
    null
  );
  const [isDeleting, setIsDeleting] = useState(false);

  // Search state
  const [searchInput, setSearchInput] = useState("");
  const [isSearching, setIsSearching] = useState(false);

  // Fetch requests WHEN user is clicked (paginated, newest first)
  const fetchRequests = useCallback(
    async (targetPage: number, append: boolean) => {
      if (!selectedUser) return;
      if (append) setIsLoadingMoreRequests(true);
      else setIsLoadingRequests(true);
      try {
        const body = await apiClient.get(
          `/api/admin/users/${selectedUser.id}/requests?page=${targetPage}&pageSize=50`
        );
        const list = Array.isArray(body) ? body : body?.data || [];
        const total = Array.isArray(body)
          ? list.length
          : body?.total ?? list.length;
        const totalPages = Array.isArray(body) ? 1 : body?.totalPages ?? 1;
        setRequests((prev) => (append ? [...prev, ...list] : list));
        setRequestsTotal(total);
        setRequestsTotalPages(totalPages);
        setRequestsPage(targetPage);
      } catch (err) {
        console.error("Error fetching requests:", err);
        if (!append) setRequests([]);
      } finally {
        if (append) setIsLoadingMoreRequests(false);
        else setIsLoadingRequests(false);
      }
    },
    [selectedUser]
  );

  useEffect(() => {
    if (!selectedUser) return;
    setRequests([]);
    fetchRequests(1, false);
  }, [selectedUser, fetchRequests]);

  // Single useEffect to handle scrolling after requests are loaded
  useEffect(() => {
    if (!pendingScrollId.current || requests.length === 0 || isLoadingRequests) {
      return;
    }

    const scrollToId = pendingScrollId.current;
    
    // Wait for DOM to update and refs to be set
    const timeoutId = setTimeout(() => {
      const element = itemRefs.current.get(scrollToId);
      
      if (element) {
        element.scrollIntoView({
          behavior: "smooth",
          block: "center",
        });
        
        // Optional: Add a highlight effect
        element.classList.add("ring-2", "ring-blue-400", "ring-offset-2");
        setTimeout(() => {
          element.classList.remove("ring-2", "ring-blue-400", "ring-offset-2");
        }, 2000);
        
        // Clear the pending scroll
        pendingScrollId.current = null;
      } else {
        console.warn("Element not found for scrolling:", scrollToId);
      }
    }, 300); // Increased timeout to ensure DOM is fully ready

    return () => clearTimeout(timeoutId);
  }, [requests, isLoadingRequests]);

  // Handle search function
  const handleSearch = async () => {
    if (!searchInput.trim()) {
      toast.error("Please enter a request number");
      return;
    }

    setIsSearching(true);

    try {
      const response = await apiClient.get(
        `/api/irembo/applications/${searchInput.trim()}`
      );

      if (response && response.data) {
        const data = response.data;

        // Set the pending scroll ID
        pendingScrollId.current = data.referenceId;

        // Select the user (this will trigger the useEffect to fetch requests)
        handleUserSelect(data.user, "#60A5FA");

        toast.success("Request found!");
      } else {
        toast.error("Request not found");
      }
    } catch (error: any) {
      console.error("Search error:", error);

      if (error?.message?.toLowerCase().includes("not found")) {
        toast.error("Request not found");
      } else {
        toast.error("Error searching for request");
      }
    } finally {
      setIsSearching(false);
    }
  };

  // Handle key press for search
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  };

  // Handle user selection
  const handleUserSelect = (user: User, color: string) => {
    setRequests([]);
    setSelectedUser(user);
    setCurrentUserColor(color);
    // Only clear search input, keep pendingScrollId if it exists
    setSearchInput("");
  };

  // Handle back button
  const handleBack = () => {
    setSelectedUser(null);
    setSearchInput("");
    pendingScrollId.current = null;
  };

  // Handle delete request
  const handleDeleteRequest = (request: AdminRequest) => {
    setRequestToDelete(request);
    setDeleteDialogOpen(true);
  };

  // Confirm delete
  const confirmDelete = async () => {
    if (!requestToDelete) return;

    setIsDeleting(true);
    try {
      await apiClient.delete(
        `/api/admin/requests/${requestToDelete.id}?type=${requestToDelete.type}`
      );

      setRequests((prev) =>
        prev.filter((r) => r.referenceId !== requestToDelete.referenceId)
      );
      setRequestsTotal((total) => Math.max(0, total - 1));

      setDeleteDialogOpen(false);
      setRequestToDelete(null);
    } catch (err) {
      toast.error("Failed to delete request");
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div>
      <div className="flex items-center p-4 justify-end">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              id="request-search"
              placeholder="Enter request number..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyPress={handleKeyPress}
              className="pl-9"
              disabled={isSearching}
              type="search"
            />
          </div>
          <Button
            onClick={handleSearch}
            disabled={isSearching || !searchInput.trim()}
            size="sm"
          >
            {isSearching ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              "Search"
            )}
          </Button>
        </div>
      </div>
      <div className="flex h-screen bg-background relative">
        
        <div
          className={`
        ${selectedUser ? "hidden" : "block"} md:block
        w-full md:w-80 lg:w-96
      `}
        >
          {/* User Sidebar */}
          <div className="px-4">
            <UserSidebar
              selectedUserId={selectedUser?.id}
              onSelect={handleUserSelect}
            />
          </div>
        </div>

        {/* RIGHT: REQUESTS - Hidden on mobile when no user is selected */}
        <main
          className={`
        ${!selectedUser ? "hidden" : "flex"} md:flex
        flex-1 flex-col w-full md:w-auto 
      `}
        >
          {!selectedUser ? (
            <div className="flex flex-col items-center justify-center h-full text-muted-foreground px-6">
              <div className="text-center max-w-md">
                <Search className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <h3 className="text-lg font-medium mb-2">
                  Search or Select User
                </h3>
                <p className="text-sm">
                  Search for a request by number above, or select a user from
                  the sidebar to view their requests.
                </p>
              </div>
            </div>
          ) : (
            <>
              {/* HEADER */}
              <header className="bg-primary text-primary-foreground px-3 py-3 flex items-center gap-3 shadow-sm sticky top-15 z-10">
                <button
                  onClick={handleBack}
                  className="p-2 hover:bg-primary/90 rounded-full transition-colors block md:hidden"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>

                <div
                  className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center font-semibold text-sm"
                  style={{ backgroundColor: currentUserColor }}
                >
                  {selectedUser.firstName?.[0]?.toUpperCase() || "?"}
                </div>

                <div className="flex-1 min-w-0">
                  <h1 className="font-semibold text-base text-wrap">
                    {selectedUser.firstName} {selectedUser.lastName} {selectedUser.phoneNumber}
                  </h1>
                  <p className="text-xs opacity-80 truncate">
                    {isLoadingRequests
                      ? "Loading..."
                      : `${requestsTotal} request${
                          requestsTotal !== 1 ? "s" : ""
                        }`}
                  </p>
                </div>
              </header>

              {/* MESSAGES AREA */}
              <ScrollArea className="flex-1 h-[90vh]  px-1 py-4 space-y-2 bg-muted/30">
                {isLoadingRequests ? (
                  <div className="flex items-center justify-center h-full">
                    <div className="text-center text-muted-foreground bg-background rounded-lg shadow-sm p-8 max-w-sm mx-auto">
                      <Loader2 className="w-8 h-8 animate-spin mx-auto mb-3 text-primary" />
                      <p className="text-sm font-medium">Loading requests...</p>
                    </div>
                  </div>
                ) : requests.length === 0 ? (
                  <div className="flex items-center justify-center h-full">
                    <div className="text-center text-muted-foreground bg-background rounded-lg shadow-sm p-6 max-w-sm mx-auto">
                      <p className="text-sm">No requests yet</p>
                    </div>
                  </div>
                ) : (
                  <>
                    {requests.map((r) => (
                      <div
                        key={`${r.type}-${r.id}`}
                        className="flex justify-end transition-all duration-300"
                        ref={(el) => {
                          if (el) {
                            itemRefs.current.set(r.referenceId, el);
                          } else {
                            itemRefs.current.delete(r.referenceId);
                          }
                        }}
                      >
                        <div className="w-full md:w-3/4 lg:w-2/3">
                          <RequestCard
                            request={r}
                            onClick={() => {
                              setSelectedRequest(r);
                              setOpen(true);
                            }}
                            onDelete={handleDeleteRequest}
                          />
                        </div>
                      </div>
                    ))}

                    {requestsTotalPages > requestsPage && (
                      <div className="flex justify-center py-4">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => fetchRequests(requestsPage + 1, true)}
                          disabled={isLoadingMoreRequests}
                        >
                          {isLoadingMoreRequests ? (
                            <Loader2 className="h-4 w-4 animate-spin mr-2" />
                          ) : null}
                          Load more
                        </Button>
                      </div>
                    )}
                  </>
                )}
              </ScrollArea>
            </>
          )}
        </main>

        {/* DETAILS DIALOG */}
        {selectedRequest && (
          <RequestDetailsDialog
            open={open}
            onOpenChange={setOpen}
            request={selectedRequest}
            onUpdated={(updated) => {
              setRequests((prev) =>
                prev.map((r) =>
                  r.referenceId == updated.referenceId ? updated : r
                )
              );
            }}
          />
        )}

        {/* DELETE CONFIRMATION DIALOG */}
        <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete Request</AlertDialogTitle>
              <AlertDialogDescription>
                Are you sure you want to delete this request? This action cannot
                be undone.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={isDeleting}>
                Cancel
              </AlertDialogCancel>
              <AlertDialogAction
                onClick={confirmDelete}
                disabled={isDeleting}
                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              >
                {isDeleting ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Deleting...
                  </>
                ) : (
                  "Delete"
                )}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  );
}