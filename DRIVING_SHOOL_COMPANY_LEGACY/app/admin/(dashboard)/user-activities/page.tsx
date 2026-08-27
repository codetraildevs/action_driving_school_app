// app/admin/user-activities/page.tsx
"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { apiClient } from "@/lib/api-client";
import { Search, Filter, Trash2, Eye, Loader2, Calendar, User, Activity } from "lucide-react";
import { toast } from "sonner";

interface UserActivity {
  id: number;
  activityType: string;
  description: string;
  userId: number;
  createdAt: string;
  user: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
  };
}

export default function UserActivitiesPage() {
  const [activities, setActivities] = useState<UserActivity[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedActivity, setSelectedActivity] = useState<UserActivity | null>(null);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [selectedActivities, setSelectedActivities] = useState<number[]>([]);
  const [filters, setFilters] = useState({
    userId: "",
    type: "",
    startDate: "",
    endDate: "",
    page: 1,
    limit: 20
  });

  useEffect(() => {
    fetchActivities();
  }, [filters]);

  const fetchActivities = async () => {
    try {
      const queryParams = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (value) queryParams.append(key, value.toString());
      });

      const data = await apiClient.get<{ 
        data: UserActivity[];
        pagination: any;
      }>(`/api/admin/user-activities?${queryParams}`);
      
      setActivities(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch user activities");
    } finally {
      setIsLoading(false);
    }
  };

  const handleView = (activity: UserActivity) => {
    setSelectedActivity(activity);
    setIsViewDialogOpen(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this activity?")) return;

    try {
      await apiClient.delete(`/api/admin/user-activities/${id}`);
      toast.success("Activity deleted successfully");
      fetchActivities();
    } catch (error) {
      toast.error("Failed to delete activity");
    }
  };

  const handleBulkDelete = async () => {
    if (selectedActivities.length === 0) {
      toast.error("Please select activities to delete");
      return;
    }

    if (!confirm(`Are you sure you want to delete ${selectedActivities.length} activities?`)) return;

    try {
      await apiClient.delete("/api/admin/user-activities", {
        ids: selectedActivities
      });
      toast.success(`${selectedActivities.length} activities deleted successfully`);
      setSelectedActivities([]);
      fetchActivities();
    } catch (error) {
      toast.error("Failed to delete activities");
    }
  };

  const getActivityTypeColor = (type: string) => {
    const typeColors: { [key: string]: string } = {
      login: "bg-blue-100 text-blue-800",
      registration: "bg-green-100 text-green-800",
      test_attempt: "bg-purple-100 text-purple-800",
      payment: "bg-orange-100 text-orange-800",
      download: "bg-indigo-100 text-indigo-800"
    };
    return typeColors[type] || "bg-gray-100 text-gray-800";
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">User Activities</h1>
          <p className="text-muted-foreground">Monitor and manage user activities</p>
        </div>
        {selectedActivities.length > 0 && (
          <Button variant="destructive" onClick={handleBulkDelete}>
            <Trash2 className="mr-2 h-4 w-4" />
            Delete Selected ({selectedActivities.length})
          </Button>
        )}
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Filter className="h-5 w-5" />
            Filters
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="space-y-2">
              <Label>User ID</Label>
              <Input
                placeholder="User ID"
                value={filters.userId}
                onChange={(e) => setFilters(prev => ({ ...prev, userId: e.target.value }))}
              />
            </div>
            <div className="space-y-2">
              <Label>Activity Type</Label>
              <Input
                placeholder="Activity type"
                value={filters.type}
                onChange={(e) => setFilters(prev => ({ ...prev, type: e.target.value }))}
              />
            </div>
            <div className="space-y-2">
              <Label>Start Date</Label>
              <Input
                type="date"
                value={filters.startDate}
                onChange={(e) => setFilters(prev => ({ ...prev, startDate: e.target.value }))}
              />
            </div>
            <div className="space-y-2">
              <Label>End Date</Label>
              <Input
                type="date"
                value={filters.endDate}
                onChange={(e) => setFilters(prev => ({ ...prev, endDate: e.target.value }))}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Activities Table */}
      <Card>
        <CardHeader>
          <CardTitle>All User Activities</CardTitle>
          <CardDescription>
            {activities.length} activities found
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : activities.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No activities found
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12">
                    <input
                      type="checkbox"
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedActivities(activities.map(a => a.id));
                        } else {
                          setSelectedActivities([]);
                        }
                      }}
                    />
                  </TableHead>
                  <TableHead>User</TableHead>
                  <TableHead>Activity Type</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {activities.map((activity) => (
                  <TableRow key={activity.id}>
                    <TableCell>
                      <input
                        type="checkbox"
                        checked={selectedActivities.includes(activity.id)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedActivities(prev => [...prev, activity.id]);
                          } else {
                            setSelectedActivities(prev => prev.filter(id => id !== activity.id));
                          }
                        }}
                      />
                    </TableCell>
                    <TableCell>
                      <div>
                        <div className="font-medium">
                          {activity.user.firstName} {activity.user.lastName}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {activity.user.email}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge className={getActivityTypeColor(activity.activityType)}>
                        {activity.activityType}
                      </Badge>
                    </TableCell>
                    <TableCell className="max-w-md">
                      <div className="line-clamp-2">{activity.description}</div>
                    </TableCell>
                    <TableCell>
                      {new Date(activity.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleView(activity)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(activity.id)}
                          className="text-red-500 hover:text-red-600"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* View Activity Dialog */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Activity Details</DialogTitle>
            <DialogDescription>
              Detailed information about user activity
            </DialogDescription>
          </DialogHeader>
          
          {selectedActivity && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Activity Type</Label>
                  <Badge className={getActivityTypeColor(selectedActivity.activityType)}>
                    {selectedActivity.activityType}
                  </Badge>
                </div>
                <div className="space-y-2">
                  <Label>Date</Label>
                  <div className="flex items-center gap-2 text-sm">
                    <Calendar className="h-4 w-4" />
                    {new Date(selectedActivity.createdAt).toLocaleString()}
                  </div>
                </div>
              </div>

              <div className="space-y-2">
                <Label>Description</Label>
                <p className="text-sm p-3 border rounded-lg bg-muted/50">
                  {selectedActivity.description}
                </p>
              </div>

              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <User className="h-4 w-4" />
                  User Information
                </Label>
                <div className="grid grid-cols-2 gap-4 p-3 border rounded-lg bg-muted/50">
                  <div>
                    <div className="text-sm font-medium">Name</div>
                    <div className="text-sm">
                      {selectedActivity.user.firstName} {selectedActivity.user.lastName}
                    </div>
                  </div>
                  <div>
                    <div className="text-sm font-medium">Email</div>
                    <div className="text-sm">{selectedActivity.user.email}</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium">Phone</div>
                    <div className="text-sm">{selectedActivity.user.phoneNumber}</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium">User ID</div>
                    <div className="text-sm">{selectedActivity.user.id}</div>
                  </div>
                </div>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsViewDialogOpen(false)}
            >
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}