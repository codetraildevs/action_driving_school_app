"use client";

import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { User } from "@/lib/generated/prisma";
import { apiClient } from "@/lib/api-client";
import { Search, Loader2, ChevronDown } from "lucide-react";
import { ScrollArea } from "@radix-ui/react-scroll-area";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

interface UserWithIremboRequestsInfo extends User {
  numberOfPendingIremboDrivingLicenseRequests: number;
  numberOfPendingIremboSpecialRequests: number;
}

interface Props {
  selectedUserId?: number;
  onSelect: (user: User, color: string) => void;
}

const PAGE_SIZE = 50;

export default function UserSidebar({ selectedUserId, onSelect }: Props) {
  const [users, setUsers] = useState<UserWithIremboRequestsInfo[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const randomColor = () =>
    `hsl(${Math.floor(Math.random() * 360)}, 70%, 60%)`;

  // Debounce search input so we query the server instead of filtering a
  // client-side copy of every Irembo user in memory.
  useEffect(() => {
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      setDebouncedSearch(searchInput.trim());
    }, 400);
    return () => {
      if (searchTimer.current) clearTimeout(searchTimer.current);
    };
  }, [searchInput]);

  // First page load / search change: replace the list, reset to page 1
  useEffect(() => {
    setIsLoading(true);
    setError(null);
    const params = new URLSearchParams({
      from: "irembo",
      page: "1",
      pageSize: String(PAGE_SIZE),
    });
    if (debouncedSearch) params.set("search", debouncedSearch);

    apiClient
      .get(`/api/admin/users?${params.toString()}`)
      .then((res: any) => {
        setUsers(res.data || []);
        setTotalPages(res.totalPages || 1);
        setPage(res.page || 1);
      })
      .catch((err) => {
        setError("Failed to load users");
        console.error(err);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [debouncedSearch]);

  const loadMore = () => {
    if (isLoadingMore || page >= totalPages) return;
    setIsLoadingMore(true);
    const params = new URLSearchParams({
      from: "irembo",
      page: String(page + 1),
      pageSize: String(PAGE_SIZE),
    });
    if (debouncedSearch) params.set("search", debouncedSearch);

    apiClient
      .get(`/api/admin/users?${params.toString()}`)
      .then((res: any) => {
        setUsers((prev) => [...prev, ...(res.data || [])]);
        setTotalPages(res.totalPages || 1);
        setPage(res.page || page + 1);
      })
      .catch((err) => {
        console.error(err);
      })
      .finally(() => {
        setIsLoadingMore(false);
      });
  };

  return (
    <aside className="flex flex-col">
      <div className="p-4 font-semibold sticky top-15 z-50 bg-background border-b">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground w-4 h-4" />
          <input
            type="text"
            placeholder="Search users..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
      </div>

      <ScrollArea className="flex-1 h-screen p-4">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
            <Loader2 className="w-8 h-8 animate-spin mb-2" />
            <p className="text-sm">Loading users...</p>
          </div>
        ) : error ? (
          <div className="flex items-center justify-center py-8 text-destructive">
            <p className="text-sm">{error}</p>
          </div>
        ) : users.length === 0 ? (
          <div className="flex items-center justify-center py-8 text-muted-foreground">
            <p className="text-sm">
              {debouncedSearch ? "No users found" : "No users available"}
            </p>
          </div>
        ) : (
          <>
            {users.map((user) => {
              const color = randomColor();
              return (
                <div
                  key={user.id}
                  onClick={() => onSelect(user, color)}
                  className={cn(
                    "px-4 py-3 cursor-pointer hover:bg-muted flex items-center justify-between gap-3 rounded-md border my-1 transition-colors",
                    selectedUserId === user.id && "bg-muted font-medium"
                  )}
                >
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center font-semibold text-sm text-white flex-shrink-0"
                    style={{
                      backgroundColor: color,
                    }}
                  >
                    {user.firstName?.[0]?.toUpperCase() || "?"}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="truncate">
                      {user.firstName} {user.lastName}
                    </div>
                    {user.email && (
                      <div className="text-xs text-muted-foreground truncate">
                        {user.phoneNumber}
                      </div>
                    )}
                  </div>
                  {user.numberOfPendingIremboDrivingLicenseRequests +
                    user.numberOfPendingIremboSpecialRequests >
                    0 && (
                    <Badge className="mt-1 rounded-full px-2 py-0.5 w-fit">
                      {user.numberOfPendingIremboDrivingLicenseRequests +
                        user.numberOfPendingIremboSpecialRequests}
                    </Badge>
                  )}
                </div>
              );
            })}

            {page < totalPages && (
              <div className="flex justify-center py-4">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={loadMore}
                  disabled={isLoadingMore}
                >
                  {isLoadingMore ? (
                    <Loader2 className="w-4 h-4 animate-spin mr-2" />
                  ) : (
                    <ChevronDown className="w-4 h-4 mr-2" />
                  )}
                  Load more
                </Button>
              </div>
            )}
          </>
        )}
      </ScrollArea>
    </aside>
  );
}
