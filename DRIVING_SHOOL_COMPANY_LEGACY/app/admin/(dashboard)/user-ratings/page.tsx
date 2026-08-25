// app/admin/user-ratings/page.tsx
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
import { Switch } from "@/components/ui/switch";
import { apiClient } from "@/lib/api-client";
import { Search, Filter, Trash2, Eye, Loader2, Star, User, Smartphone, Laptop, CheckCircle, XCircle } from "lucide-react";
import { toast } from "sonner";

interface UserRating {
  id: number;
  rating: string;
  title: string;
  comment: string;
  userId: number;
  platform: string;
  appVersion: string;
  isVerified: boolean;
  createdAt: string;
  user: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
  };
}

interface RatingStats {
  average: number;
  total: number;
  min: number;
  max: number;
}

export default function UserRatingsPage() {
  const [ratings, setRatings] = useState<UserRating[]>([]);
  const [stats, setStats] = useState<RatingStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedRating, setSelectedRating] = useState<UserRating | null>(null);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [selectedRatings, setSelectedRatings] = useState<number[]>([]);
  const [filters, setFilters] = useState({
    userId: "",
    platform: "",
    minRating: "",
    isVerified: "",
    page: 1,
    limit: 20
  });

  useEffect(() => {
    fetchRatings();
  }, [filters]);

  const fetchRatings = async () => {
    try {
      const queryParams = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (value) queryParams.append(key, value.toString());
      });

      const data = await apiClient.get<{ 
        data: UserRating[];
        stats: RatingStats;
        pagination: any;
      }>(`/api/admin/user-ratings?${queryParams}`);
      
      setRatings(data.data || []);
      setStats(data.stats || null);
    } catch (error) {
      toast.error("Failed to fetch user ratings");
    } finally {
      setIsLoading(false);
    }
  };

  const handleView = (rating: UserRating) => {
    setSelectedRating(rating);
    setIsViewDialogOpen(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this rating?")) return;

    try {
      await apiClient.delete(`/api/admin/user-ratings/${id}`);
      toast.success("Rating deleted successfully");
      fetchRatings();
    } catch (error) {
      toast.error("Failed to delete rating");
    }
  };

  const handleBulkDelete = async () => {
    if (selectedRatings.length === 0) {
      toast.error("Please select ratings to delete");
      return;
    }

    if (!confirm(`Are you sure you want to delete ${selectedRatings.length} ratings?`)) return;

    try {
      await apiClient.delete("/api/admin/user-ratings", {
        ids: selectedRatings
      });
      toast.success(`${selectedRatings.length} ratings deleted successfully`);
      setSelectedRatings([]);
      fetchRatings();
    } catch (error) {
      toast.error("Failed to delete ratings");
    }
  };

  const handleVerificationToggle = async (rating: UserRating) => {
    try {
      await apiClient.put(`/api/admin/user-ratings/${rating.id}`, {
        isVerified: !rating.isVerified
      });
      toast.success(`Rating ${!rating.isVerified ? 'verified' : 'unverified'} successfully`);
      fetchRatings();
    } catch (error) {
      toast.error("Failed to update rating verification");
    }
  };

  const getRatingColor = (rating: string) => {
    const numRating = parseFloat(rating);
    if (numRating >= 4) return "bg-green-100 text-green-800 border-green-200";
    if (numRating >= 3) return "bg-yellow-100 text-yellow-800 border-yellow-200";
    return "bg-red-100 text-red-800 border-red-200";
  };

  const renderStars = (rating: string) => {
    const numRating = parseFloat(rating);
    const stars = [];
    for (let i = 1; i <= 5; i++) {
      stars.push(
        <Star
          key={i}
          className={`h-4 w-4 ${
            i <= numRating ? "fill-yellow-400 text-yellow-400" : "text-gray-300"
          }`}
        />
      );
    }
    return <div className="flex gap-0.5">{stars}</div>;
  };

  const getPlatformIcon = (platform: string) => {
    switch (platform) {
      case 'ios':
        return <Smartphone className="h-4 w-4" />;
      case 'android':
        return <Smartphone className="h-4 w-4" />;
      case 'web':
        return <Laptop className="h-4 w-4" />;
      default:
        return <Smartphone className="h-4 w-4" />;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">App Ratings & Reviews</h1>
          <p className="text-muted-foreground">Manage user ratings and feedback for the application</p>
        </div>
        {selectedRatings.length > 0 && (
          <Button variant="destructive" onClick={handleBulkDelete}>
            <Trash2 className="mr-2 h-4 w-4" />
            Delete Selected ({selectedRatings.length})
          </Button>
        )}
      </div>

      {/* Rating Stats */}
      {stats && (
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Average Rating</CardTitle>
              <Star className="h-4 w-4 text-yellow-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.average.toFixed(1)}/5</div>
              <div className="flex items-center gap-1 mt-1">
                {renderStars(stats.average.toString())}
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Ratings</CardTitle>
              <User className="h-4 w-4 text-blue-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.total}</div>
              <p className="text-xs text-muted-foreground">User reviews</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Highest Rating</CardTitle>
              <Star className="h-4 w-4 text-green-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.max}/5</div>
              <p className="text-xs text-muted-foreground">Best rating</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Lowest Rating</CardTitle>
              <Star className="h-4 w-4 text-red-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.min}/5</div>
              <p className="text-xs text-muted-foreground">Worst rating</p>
            </CardContent>
          </Card>
        </div>
      )}

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
              <Label>Platform</Label>
              <Select
                value={filters.platform}
                onValueChange={(value) => setFilters(prev => ({ ...prev, platform: value }))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All platforms" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ios">iOS</SelectItem>
                  <SelectItem value="android">Android</SelectItem>
                  <SelectItem value="web">Web</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Minimum Rating</Label>
              <Select
                value={filters.minRating}
                onValueChange={(value) => setFilters(prev => ({ ...prev, minRating: value }))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All ratings" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">1 Star & Above</SelectItem>
                  <SelectItem value="2">2 Stars & Above</SelectItem>
                  <SelectItem value="3">3 Stars & Above</SelectItem>
                  <SelectItem value="4">4 Stars & Above</SelectItem>
                  <SelectItem value="5">5 Stars Only</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Verification Status</Label>
              <Select
                value={filters.isVerified}
                onValueChange={(value) => setFilters(prev => ({ ...prev, isVerified: value }))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All statuses" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="true">Verified Only</SelectItem>
                  <SelectItem value="false">Not Verified</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Ratings Table */}
      <Card>
        <CardHeader>
          <CardTitle>All User Ratings</CardTitle>
          <CardDescription>
            {ratings.length} rating{ratings.length !== 1 ? 's' : ''} found
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : ratings.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No ratings found
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
                          setSelectedRatings(ratings.map(r => r.id));
                        } else {
                          setSelectedRatings([]);
                        }
                      }}
                    />
                  </TableHead>
                  <TableHead>User</TableHead>
                  <TableHead>Rating</TableHead>
                  <TableHead>Title</TableHead>
                  <TableHead>Platform</TableHead>
                  <TableHead>Verified</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {ratings.map((rating) => (
                  <TableRow key={rating.id}>
                    <TableCell>
                      <input
                        type="checkbox"
                        checked={selectedRatings.includes(rating.id)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedRatings(prev => [...prev, rating.id]);
                          } else {
                            setSelectedRatings(prev => prev.filter(id => id !== rating.id));
                          }
                        }}
                      />
                    </TableCell>
                    <TableCell>
                      <div>
                        <div className="font-medium">
                          {rating.user.firstName} {rating.user.lastName}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {rating.user.email}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {renderStars(rating.rating)}
                        <Badge variant="outline" className={getRatingColor(rating.rating)}>
                          {rating.rating}/5
                        </Badge>
                      </div>
                    </TableCell>
                    <TableCell className="max-w-xs">
                      <div className="line-clamp-2 font-medium">
                        {rating.title}
                      </div>
                      <div className="line-clamp-1 text-sm text-muted-foreground mt-1">
                        {rating.comment}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {getPlatformIcon(rating.platform)}
                        <Badge variant="outline" className="capitalize">
                          {rating.platform}
                        </Badge>
                        <div className="text-xs text-muted-foreground">
                          v{rating.appVersion}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Switch
                          checked={rating.isVerified}
                          onCheckedChange={() => handleVerificationToggle(rating)}
                        />
                        {rating.isVerified ? (
                          <CheckCircle className="h-4 w-4 text-green-500" />
                        ) : (
                          <XCircle className="h-4 w-4 text-gray-400" />
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      {new Date(rating.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleView(rating)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(rating.id)}
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

      {/* View Rating Dialog */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Rating Details</DialogTitle>
            <DialogDescription>
              Complete information about user rating and feedback
            </DialogDescription>
          </DialogHeader>
          
          {selectedRating && (
            <div className="space-y-6">
              {/* Rating Header */}
              <div className="flex items-start justify-between">
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    {renderStars(selectedRating.rating)}
                    <Badge variant="outline" className={getRatingColor(selectedRating.rating)}>
                      {selectedRating.rating}/5
                    </Badge>
                  </div>
                  <h3 className="text-lg font-semibold">{selectedRating.title}</h3>
                </div>
                <div className="flex items-center gap-2">
                  <Switch
                    checked={selectedRating.isVerified}
                    onCheckedChange={() => handleVerificationToggle(selectedRating)}
                  />
                  <Label>{selectedRating.isVerified ? 'Verified' : 'Not Verified'}</Label>
                </div>
              </div>

              {/* Comment */}
              <div className="space-y-2">
                <Label>User Feedback</Label>
                <div className="p-4 border rounded-lg bg-muted/50">
                  <p className="text-sm whitespace-pre-wrap">{selectedRating.comment}</p>
                </div>
              </div>

              {/* Platform & Version */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Platform</Label>
                  <div className="flex items-center gap-2 p-3 border rounded-lg">
                    {getPlatformIcon(selectedRating.platform)}
                    <span className="capitalize font-medium">{selectedRating.platform}</span>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>App Version</Label>
                  <div className="p-3 border rounded-lg font-mono">
                    {selectedRating.appVersion}
                  </div>
                </div>
              </div>

              {/* User Information */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <User className="h-4 w-4" />
                  User Information
                </Label>
                <div className="grid grid-cols-2 gap-4 p-4 border rounded-lg bg-muted/50">
                  <div>
                    <div className="text-sm font-medium">Name</div>
                    <div className="text-sm">
                      {selectedRating.user.firstName} {selectedRating.user.lastName}
                    </div>
                  </div>
                  <div>
                    <div className="text-sm font-medium">Email</div>
                    <div className="text-sm">{selectedRating.user.email}</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium">Phone</div>
                    <div className="text-sm">{selectedRating.user.phoneNumber}</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium">User ID</div>
                    <div className="text-sm">{selectedRating.user.id}</div>
                  </div>
                </div>
              </div>

              {/* Metadata */}
              <div className="flex items-center justify-between text-sm text-muted-foreground">
                <div>
                  Submitted on {new Date(selectedRating.createdAt).toLocaleString()}
                </div>
                <div>
                  Rating ID: {selectedRating.id}
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