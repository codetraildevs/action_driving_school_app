"use client";

import {
  startTransition,
  useCallback,
  useEffect,
  useOptimistic,
  useRef,
  useState,
} from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Switch } from "@/components/ui/switch";
import { apiClient } from "@/lib/api-client";
import {
  Search,
  Edit,
  Trash2,
  Loader2,
  User,
  Phone,
  X,
  PhoneCall,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { toast } from "sonner";
import { UserDetailDialog } from "@/components/admin/user-detail-dialog";
import {
  Device,
  Language,
  UserTestAccess,
  UserTestAccessStatus,
} from "@/lib/generated/prisma";
import { format } from "date-fns";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/lib/auth/auth-context";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
interface User {
  id: number;
  firstName: string;
  middleName?: string;
  lastName?: string;
  email: string;
  phoneNumber: string;
  pending?: false | boolean;
  isActive: boolean;
  role: {
    id: number;
    roleName: string;
  };
  language: Language;
  userTestAccess: UserTestAccess;
  devices: Device[];
  createdAt: string;
}

interface UserFormData {
  firstName: string;
  middleName?: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  languageId: number;
  roleId: number;
}

interface UserTestAccessFormData {
  tests: number;
  expiresAt: Date;
  status: UserTestAccessStatus;
}

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalUsers, setTotalUsers] = useState(0);
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState("all");
  const [isUpdatingAccess, setIsUpdatingAccess] = useState(false);
  const [availableLanguages, setAvailableLanguages] = useState<Language[]>([]);
  const [optimisticUsers, addOptimisticUser] = useOptimistic(
    users,
    (users, newUser: User) => {
      return users.map((user) =>
        user.id === newUser.id ? { ...newUser } : user,
      );
    },
  );
  const { user: currentUser } = useAuth();

  const [formData, setFormData] = useState<UserFormData>({
    firstName: "",
    middleName: "",
    lastName: "",
    email: "",
    phoneNumber: "",
    roleId: 2,
    languageId: -1,
  });

  const [accessFormData, setAccessFormData] = useState<UserTestAccessFormData>({
    tests: 0,
    status: UserTestAccessStatus.PENDING,
    expiresAt: new Date(),
  });

  const fetchLanguages = async () => {
    try {
      const languages = await apiClient.get<{ data: Language[] }>(
        "/api/languages"
      );
      setAvailableLanguages(languages.data);
    } catch (error) {}
  };
  const [debouncedSearch, setDebouncedSearch] = useState("");

  const fetchUsers = useCallback(async () => {
    try {
      setIsLoading(true);
      const params = new URLSearchParams();
      params.set("page", String(page));
      params.set("pageSize", "25");
      if (debouncedSearch.trim()) params.set("search", debouncedSearch.trim());
      if (activeTab !== "all") params.set("tab", activeTab);
      const data = await apiClient.get<{
        data: User[];
        total: number;
        page: number;
        totalPages: number;
      }>(`/api/admin/users?${params.toString()}`);
      setUsers(data.data || []);
      setTotalUsers(data.total || 0);
      setTotalPages(data.totalPages || 1);
      setPage(data.page || page);
    } catch (error) {
      toast.error("Failed to fetch users: " + (error as Error).message);
    } finally {
      setIsLoading(false);
    }
  }, [page, debouncedSearch, activeTab]);

  // Debounce search input, then reset to page 1
  useEffect(() => {
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      setDebouncedSearch(searchQuery);
      setPage(1);
    }, 400);
    return () => {
      if (searchTimer.current) clearTimeout(searchTimer.current);
    };
  }, [searchQuery]);

  // Reset to page 1 when the tab changes
  useEffect(() => {
    setPage(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  // Load the page whenever page / search / tab changes
  useEffect(() => {
    fetchUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, debouncedSearch, activeTab]);

  useEffect(() => {
    fetchLanguages();
  }, []);

  const handleEditUserAccess = async () => {
    if (!selectedUser) {
      toast.error("First name and email are required");
      return;
    }

    setIsUpdatingAccess(true);

    try {
      setUsers((prev) =>
        prev.map((user) =>
          user.id === selectedUser.id
            ? {
                ...user,
                userTestAccess: {
                  ...selectedUser.userTestAccess,
                  maxTest: accessFormData.tests,
                  expiresAt: accessFormData.expiresAt,
                  status: accessFormData.status,
                },
              }
            : user,
        ),
      );

      await apiClient.put(
        `/api/admin/subscriptions/${selectedUser.id}`,
        accessFormData,
      );
      toast.success("User access updated successfully");

      setIsEditDialogOpen(false);
      resetForm();
      setSelectedUser(null);
    } catch (error) {
      fetchUsers();
      toast.error("Failed to update user: " + (error as Error).message);
    } finally {
      setIsUpdatingAccess(false);
    }
  };

  const handleEditUser = async () => {
    if (!selectedUser || !formData.firstName.trim()) {
      toast.error("First name and email are required");
      return;
    }

    setIsEditDialogOpen(false);
    try {
      startTransition(async () => {
        addOptimisticUser({
          ...selectedUser,
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email,
          phoneNumber: formData.phoneNumber,
          pending: true,
        });
        const resp = (await apiClient.put(
          `/api/admin/users/${selectedUser.id}`,
          formData,
        )) as any;
        const updatedUser = resp.data;
        toast.success("User updated successfully");

        resetForm();
        setSelectedUser(null);
        startTransition(() => {
          setUsers((prev) =>
            prev.map((user) =>
              user.id == updatedUser.id
                ? {
                    ...user,
                    firstName: updatedUser.firstName,
                    lastName: updatedUser.lastName,
                    email: updatedUser.email,
                    phoneNumber: updatedUser.phoneNumber,
                    pending: false,
                    language: updatedUser.language,
                    role: updatedUser.role,
                  }
                : user,
            ),
          );
        });
      });
    } catch (error) {
      // Revert on error
      fetchUsers(); // Refetch to get correct data
      toast.error("Failed to update user: " + (error as Error).message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteUser = async (userId: number) => {
    const user = users.find((user) => user.id === userId);
    if (!user) return;
    try {
      startTransition(async () => {
        addOptimisticUser({ ...user, pending: true });
        await apiClient.delete(`/api/admin/users/${userId}`);
        toast.success("User deleted successfully");
        startTransition(() => {
          setUsers((prev) => prev.filter((user) => user.id !== userId));
        });
      });
    } catch (error) {
      // Revert on error
      if (user) {
        setUsers((prev) => [...prev, user].sort((a, b) => a.id - b.id));
      }
      toast.error("Failed to delete user: " + (error as Error).message);
    }
  };

  const handleToggleUserStatus = async (
    userId: number,
    currentStatus: boolean,
  ) => {
    try {
      const user = users.find((u) => u.id === userId) || users[0];

      startTransition(async () => {
        addOptimisticUser({ ...user, isActive: !user.isActive, pending: true });
        const resp = (await apiClient.put(`/api/admin/users/${userId}`, {
          isActive: !currentStatus,
        })) as any;
        const updatedUser = resp.data;

        toast.success(
          `User ${!currentStatus ? "activated" : "deactivated"} successfully`,
        );
        startTransition(() => {
          setUsers((prev) =>
            prev.map((user) =>
              user.id === userId
                ? { ...user, isActive: updatedUser.isActive, pending: false }
                : user,
            ),
          );
        });
      });
    } catch (error) {
      // Revert on error
      setUsers((prev) =>
        prev.map((user) =>
          user.id === userId ? { ...user, isActive: currentStatus } : user,
        ),
      );
      toast.error("Failed to update user status: " + (error as Error).message);
    }
  };

  const handleEditClick = (user: User) => {
    setSelectedUser(user);
    setFormData({
      firstName: user.firstName,
      middleName: user.middleName,
      lastName: user.lastName || "",
      email: user.email,
      phoneNumber: user.phoneNumber,
      roleId: user.role.id,
      languageId: user.language.id,
    });
    setAccessFormData({
      tests: user?.userTestAccess?.maxTest,
      status: user?.userTestAccess?.status,
      expiresAt: user?.userTestAccess?.expiresAt,
    });
    setIsEditDialogOpen(true);
  };

  const resetForm = () => {
    setFormData({
      firstName: "",
      lastName: "",
      email: "",
      phoneNumber: "",
      roleId: 2,
      languageId:-1
    });
    setAccessFormData({
      tests: 0,
      status: UserTestAccessStatus.PENDING,
      expiresAt: new Date(),
    });
  };

  // Filtering now happens server-side via the /api/admin/users?search&tab&page params
  const filteredUsers = optimisticUsers;

  // Mobile Grid Card Component
  const UserCard = ({ user }: { user: User }) => (
    <Card className="p-4">
      <div className="">
        {/* User Header */}
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <h3 className="text-xl font-bold">
              {user.firstName} {user.lastName}{" "}
              <span className="text-sm">({user.language.nativeName})</span>
            </h3>

            <a
              href={`tel:${user.phoneNumber}`}
              className="flex items-center gap-2  font-mono text-2xl font-bold"
            >
              <Phone className="h-3 w-3" fill="primary" />
              <span>{user.phoneNumber}</span>
            </a>
          </div>
          <div className="flex flex-col items-end gap-1">
            <div className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground">Active</span>
              <Switch
                checked={user.isActive}
                onCheckedChange={() =>
                  handleToggleUserStatus(user.id, user.isActive)
                }
                className="h-6 w-8"
                disabled={user.id === currentUser?.id || user.pending}
              />
              {user.pending && <Loader2 className="animate-spin" />}
            </div>
            <Badge variant="outline" className="text-xs">
              {user.role.roleName}
            </Badge>
          </div>
        </div>

        {/* Subscription Information */}
        <div className="space-y-2">
          {user.userTestAccess ? (
            <div className="space-y-1 flex justify-center">
              <Badge variant="secondary" className="text-xs">
                {user.userTestAccess.maxTest} Tests
              </Badge>
              {user.userTestAccess.expiresAt && (
                <div>
                  {new Date(user.userTestAccess.expiresAt) > new Date() ? (
                    <Badge variant="default" className="text-xs">
                      Active
                    </Badge>
                  ) : (
                    <Badge variant="destructive" className="text-xs">
                      Expired
                    </Badge>
                  )}
                  <Badge variant={"secondary"}>
                    Status: {user.userTestAccess.status}
                  </Badge>
                </div>
              )}
            </div>
          ) : (
            <Badge variant="outline" className="text-xs">
              No Access Granted
            </Badge>
          )}
          <div className="text-sm text-center">
            <span className="font-bold">Joined:</span>
            <span className="text-muted-foreground ">
              {" "}
              {format(new Date(user.createdAt), "yyyy-MM-dd HH:mm:ss")}
            </span>
          </div>
        </div>
        {/* Device Information */}
        {user.devices.length > 0 && (
          <div className="flex  justify-center items-center text-[10px] flex-wrap">
            <div>
              <span className="font-bold">ID: </span>
              <span className="font-mono">
                {user.devices[0].physicalAddress}
              </span>
            </div>
            <div>
              <span className="font-bold">Name: </span>
              <span>{user.devices[0].name}</span>
            </div>

            <div>
              <span className="font-bold">Manufacturer: </span>
              <span>{user.devices[0].manufacturer}</span>
            </div>
          </div>
        )}

        <div className="flex items-center justify-between pt-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleEditClick(user)}
            title="Edit user"
            disabled={user.id === currentUser?.id || user.pending}
          >
            <Edit className="h-4 w-4" />
          </Button>
          <a
            href={`tel:${user.phoneNumber}`}
            className="flex items-center gap-2 text-sm rounded-full p-4 border-4 border-b-primary border-t-primary transition-colors hover:bg-primary/10  "
          >
            <PhoneCall className="h-5 w-5 text-primary" fill="primary" />
          </a>

          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                disabled={user.id === currentUser?.id || user.pending}
              >
                <Trash2 className="h-4 w-4 text-red-500" />
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>
                  Are you sure you want to delete this user?
                </AlertDialogTitle>
                <AlertDialogDescription>
                  This action cannot be undone. This will permanently delete
                  your user account.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction onClick={() => handleDeleteUser(user.id)}>
                  {" "}
                  {user.pending && <Loader2 className="animate-spin" />}
                  Continue{" "}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </div>
    </Card>
  );

  return (
    <div className="space-y-2 md:space-y-6 space-x-0 p-0 md:p-6">
      {/* Header */}
      <div className="items-center justify-between hidden md:flex">
        <div>
          <h1 className="md:text-3xl text-lg font-bold tracking-tight">
            Users
          </h1>
          <p className="text-muted-foreground sm:text-sm">
            Manage your application users
          </p>
        </div>
      </div>

      {/* Search */}
      <div className="flex items-center space-x-2 sticky top-15 z-10 bg-background ">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search users..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8"
          />
        </div>
      </div>

      {/* Tabs */}
      <Tabs
        value={activeTab}
        onValueChange={setActiveTab}
        className="space-y-2 border-none"
      >
        <TabsList className="grid grid-cols-5 w-full max-w-md">
          <TabsTrigger value="all">All</TabsTrigger>
          <TabsTrigger value="active">Active</TabsTrigger>
          <TabsTrigger value="inactive">Inactive</TabsTrigger>
          <TabsTrigger value="new">New</TabsTrigger>
          <TabsTrigger value="old">Old</TabsTrigger>
        </TabsList>

        <TabsContent
          value={activeTab}
          className="space-y-2 max-h-[75vh] overflow-auto border-none"
        >
          <Card className="border-none p-0 shadow-none w-full">
            <CardHeader>
              <CardTitle className="py-0">
                {activeTab === "all" && "All Users"}
                {activeTab === "active" && "Active Users"}
                {activeTab === "inactive" && "Inactive Users"}
                {activeTab === "new" && "New Users (Last 7 days)"}
                {activeTab === "old" && "Existing Users"}
              </CardTitle>
              <CardDescription>
                {totalUsers} user
                {totalUsers !== 1 ? "s" : ""} found
              </CardDescription>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin" />
                </div>
              ) : filteredUsers.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  No users found
                </div>
              ) : (
                <>
                  {/* Desktop Table View */}
                  <div className="hidden md:block">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Name</TableHead>
                          <TableHead>Phone No</TableHead>
                          <TableHead>Device Id</TableHead>
                          <TableHead>Device Name</TableHead>
                          <TableHead>Manufacturer</TableHead>
                          <TableHead>Language</TableHead>
                          <TableHead>Role</TableHead>
                          <TableHead>Status</TableHead>
                          <TableHead>Access</TableHead>
                          <TableHead>Joined</TableHead>
                          <TableHead className="text-right">Actions</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {filteredUsers.map((user) => (
                          <TableRow key={user.id}>
                            <TableCell className="font-medium">
                              {user.firstName} {user.lastName}
                            </TableCell>
                            <TableCell>
                              <a
                                href={`tel:${user.phoneNumber}`}
                                className="flex items-center gap-2 text-sm text-muted-foreground"
                              >
                                <Phone className="h-3 w-3" fill="primary" />
                                <span>{user.phoneNumber}</span>
                              </a>
                            </TableCell>
                            <TableCell>
                              {user.devices.length > 0 &&
                                user.devices[0].physicalAddress}
                            </TableCell>
                            <TableCell>
                              {user.devices.length > 0 && user.devices[0].name}
                            </TableCell>

                            <TableCell>
                              {user.devices.length > 0 &&
                                user.devices[0].manufacturer}
                            </TableCell>
                            <TableCell>{user.language.nativeName}</TableCell>
                            <TableCell>
                              <Badge variant="outline">
                                {user.role.roleName}
                              </Badge>
                            </TableCell>
                            <TableCell>
                              <div className="flex items-center gap-2">
                                <Switch
                                  checked={user.isActive}
                                  onCheckedChange={() =>
                                    handleToggleUserStatus(
                                      user.id,
                                      user.isActive,
                                    )
                                  }
                                  disabled={user.id === currentUser?.id}
                                />
                                <Badge
                                  variant={
                                    user.isActive ? "default" : "secondary"
                                  }
                                >
                                  {user.isActive ? "Active" : "Inactive"}
                                </Badge>
                              </div>
                            </TableCell>
                            <TableCell>
                              {user?.userTestAccess && (
                                <div className="space-y-1">
                                  <Badge variant={"secondary"}>
                                    {user.userTestAccess.maxTest} Tests
                                  </Badge>
                                  {user.userTestAccess.expiresAt && (
                                    <div>
                                      {new Date(user.userTestAccess.expiresAt) >
                                      new Date() ? (
                                        <Badge
                                          variant={"default"}
                                          className="text-xs"
                                        >
                                          Active
                                        </Badge>
                                      ) : (
                                        <Badge
                                          variant={"destructive"}
                                          className="text-xs"
                                        >
                                          Expired
                                        </Badge>
                                      )}
                                    </div>
                                  )}

                                  <Badge variant={"secondary"}>
                                    Status: {user.userTestAccess.status}
                                  </Badge>
                                </div>
                              )}
                            </TableCell>
                            <TableCell>
                              {format(
                                new Date(user.createdAt),
                                "yyyy-MM-dd HH:mm:ss",
                              )}
                            </TableCell>
                            <TableCell className="text-right">
                              <div className="flex items-center justify-end gap-2">
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleEditClick(user)}
                                  title="Edit user"
                                  disabled={
                                    user.id === currentUser?.id ||
                                    user.id === currentUser?.id ||
                                    user.pending
                                  }
                                >
                                  <Edit className="h-4 w-4" />
                                </Button>

                                <AlertDialog>
                                  <AlertDialogTrigger asChild>
                                    <Button
                                      variant="ghost"
                                      size="sm"
                                      disabled={
                                        user.id === currentUser?.id ||
                                        user.pending
                                      }
                                    >
                                      <Trash2 className="h-4 w-4 text-red-500" />
                                    </Button>
                                  </AlertDialogTrigger>
                                  <AlertDialogContent>
                                    <AlertDialogHeader>
                                      <AlertDialogTitle>
                                        Are you sure you want to delete this
                                        user?
                                      </AlertDialogTitle>
                                      <AlertDialogDescription>
                                        This action cannot be undone. This will
                                        permanently delete your user account.
                                      </AlertDialogDescription>
                                    </AlertDialogHeader>
                                    <AlertDialogFooter>
                                      <AlertDialogCancel>
                                        Cancel
                                      </AlertDialogCancel>
                                      <AlertDialogAction
                                        onClick={() =>
                                          handleDeleteUser(user.id)
                                        }
                                      >
                                        {" "}
                                        {user.pending && (
                                          <Loader2 className="animate-spin" />
                                        )}
                                        Continue{" "}
                                      </AlertDialogAction>
                                    </AlertDialogFooter>
                                  </AlertDialogContent>
                                </AlertDialog>
                                {user.pending && (
                                  <Loader2 className="animate-spin" />
                                )}
                              </div>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>

                  {/* Mobile Grid View */}
                  <div className="grid grid-cols-1 gap-2 md:hidden ">
                    {filteredUsers.map((user) => (
                      <UserCard key={user.id} user={user} />
                    ))}
                  </div>

                  {/* Pagination */}
                  {totalPages > 1 && (
                    <div className="flex items-center justify-between pt-4 mt-4 border-t">
                      <p className="text-sm text-muted-foreground">
                        Page {page} of {totalPages}
                      </p>
                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setPage((p) => Math.max(1, p - 1))
                          }
                          disabled={page <= 1 || isLoading}
                        >
                          <ChevronLeft className="h-4 w-4" /> Previous
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setPage((p) => Math.min(totalPages, p + 1))
                          }
                          disabled={page >= totalPages || isLoading}
                        >
                          Next <ChevronRight className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Edit User Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit User</DialogTitle>
            <DialogDescription>Update user information.</DialogDescription>
          </DialogHeader>
          <div className="p-2">
            <h5 className="font-bold">User Information</h5>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="edit-firstName">First Name *</Label>
                <Input
                  id="edit-firstName"
                  placeholder="John"
                  value={formData.firstName}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      firstName: e.target.value,
                    }))
                  }
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="edit-firstName">Middle Name *</Label>
                <Input
                  id="edit-middleName"
                  placeholder="John"
                  value={formData.middleName}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      firstName: e.target.value,
                    }))
                  }
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-lastName">Last Name</Label>
                <Input
                  id="edit-lastName"
                  placeholder="Doe"
                  value={formData.lastName}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      lastName: e.target.value,
                    }))
                  }
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-phone">Phone Number</Label>
                <Input
                  id="edit-phone"
                  placeholder="+250 700 000 000"
                  value={formData.phoneNumber}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      phoneNumber: e.target.value,
                    }))
                  }
                />
              </div>
            </div>

              <div className="space-y-2">
              <Label htmlFor="language">Language</Label>
              <Select
                value={formData.languageId.toString()}
                onValueChange={(value) =>
                  setFormData((prev) => ({
                    ...prev,
                    languageId: Number(value) as number,
                  }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select language" />
                </SelectTrigger>
                <SelectContent>
                  {availableLanguages.map((s, idx) => (
                    <SelectItem value={s.id.toString()} key={idx}>
                      {s.languageName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex gap-2 pt-1">
              <Button onClick={handleEditUser} disabled={isSubmitting}>
                {isSubmitting && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Update User info
              </Button>
            </div>
          </div>

          <div className="border-2 p-4 rounded-md">
            <h5 className="font-bold">User Access Level</h5>

            <div className="space-y-2">
              <Label htmlFor="edit-phone">Tests</Label>
              <Input
                id="edit-tests"
                type="number"
                value={accessFormData.tests}
                min={0}
                onChange={(e) =>
                  setAccessFormData((prev) => ({
                    ...prev,
                    tests: Number(e.target.value),
                  }))
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit-expireAt">ExpireAt</Label>
              <Input
                id="edit-expire"
                type="date"
                min={new Date().toISOString().split("T")[0]}
                value={
                  accessFormData.expiresAt
                    ? new Date(accessFormData.expiresAt)
                        .toISOString()
                        .split("T")[0]
                    : ""
                }
                onChange={(e) =>
                  setAccessFormData((prev) => ({
                    ...prev,
                    expiresAt: new Date(e.target.value),
                  }))
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="status">Status</Label>
              <Select
                value={accessFormData.status}
                onValueChange={(value) =>
                  setAccessFormData((prev) => ({
                    ...prev,
                    status: value as UserTestAccessStatus,
                  }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent>
                  {Object.values(UserTestAccessStatus).map((s, idx) => (
                    <SelectItem value={s} key={idx}>
                      {s}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-2 pt-1">
              <Button
                onClick={handleEditUserAccess}
                disabled={isUpdatingAccess}
              >
                {isUpdatingAccess && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Update User Access
              </Button>
            </div>

            
          </div>
          <div className="flex justify-end">
            {" "}
            <Button
              variant="outline"
              onClick={() => {
                setIsEditDialogOpen(false);
                resetForm();
                setSelectedUser(null);
              }}
            >
              <X />
              Close
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* User Detail Dialog */}
      <UserDetailDialog
        user={selectedUser}
        open={isDetailDialogOpen}
        onOpenChange={setIsDetailDialogOpen}
        onUpdate={fetchUsers}
      />
    </div>
  );
}
