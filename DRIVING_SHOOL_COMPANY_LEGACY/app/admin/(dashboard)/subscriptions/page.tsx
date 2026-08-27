"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { apiClient } from "@/lib/api-client";
import { Plus, Edit, Trash2, Loader2, DollarSign, Users, Clock, X, Save, Key } from "lucide-react";
import { toast } from "sonner";

interface SubscriptionPlan {
  id: number;
  planName: string;
  amount: string;
  duration: number;
  permissions: Array<{ id: number; permissionName: string }>;
  _count: {
    userSubscriptions: number;
  };
  createdAt: string;
}

interface PlanFormData {
  planName: string;
  amount: string;
  duration: string;
  permissions: string[];
}

const defaultPermissions = [
  "access_basic_tests",
  "access_premium_tests", 
  "download_materials",
  "unlimited_attempts",
  "progress_tracking",
  "certificate_generation",
  "video_content",
  "priority_support"
];

export default function SubscriptionsPage() {
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editingPlan, setEditingPlan] = useState<SubscriptionPlan | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [newPermission, setNewPermission] = useState("");

  const [formData, setFormData] = useState<PlanFormData>({
    planName: "",
    amount: "",
    duration: "",
    permissions: [],
  });

  useEffect(() => {
    fetchPlans();
  }, []);

  const fetchPlans = async () => {
    try {
      const data = await apiClient.get<{ data: SubscriptionPlan[] }>("/api/subscriptions");
      setPlans(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch plans");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validate form
    if (!formData.planName.trim() || !formData.amount || !formData.duration) {
      toast.error("Please fill in all required fields");
      return;
    }

    setIsSubmitting(true);

    try {
      const payload = {
        planName: formData.planName.trim(),
        amount: parseFloat(formData.amount),
        duration: parseInt(formData.duration),
        permissions: formData.permissions
      };

      console.log("Creating plan with payload:", payload); // Debug log

      await apiClient.post("/api/subscriptions", payload);
      toast.success("Subscription plan created successfully");
      setIsDialogOpen(false);
      resetForm();
      fetchPlans();
    } catch (error: any) {
      console.error("Create plan error:", error);
      const errorMessage = error.message || "Failed to create plan";
      toast.error(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPlan) return;

    // Validate form
    if (!formData.planName.trim() || !formData.amount || !formData.duration) {
      toast.error("Please fill in all required fields");
      return;
    }

    setIsSubmitting(true);

    try {
      const payload = {
        planName: formData.planName.trim(),
        amount: parseFloat(formData.amount),
        duration: parseInt(formData.duration),
        permissions: formData.permissions
      };

      console.log("Updating plan with payload:", payload); // Debug log

      await apiClient.put(`/api/subscriptions/${editingPlan.id}`, payload);
      toast.success("Subscription plan updated successfully");
      setIsDialogOpen(false);
      resetForm();
      fetchPlans();
    } catch (error: any) {
      console.error("Update plan error:", error);
      const errorMessage = error.message || "Failed to update plan";
      toast.error(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this plan? This action cannot be undone.")) return;

    try {
      await apiClient.delete(`/api/subscriptions/${id}`);
      toast.success("Plan deleted successfully");
      fetchPlans();
    } catch (error: any) {
      const errorMessage = error.message || "Failed to delete plan";
      toast.error(errorMessage);
    }
  };

  const handleEdit = (plan: SubscriptionPlan) => {
    setEditingPlan(plan);
    setIsEditing(true);
    setFormData({
      planName: plan.planName,
      amount: plan.amount,
      duration: plan.duration.toString(),
      permissions: plan.permissions.map(p => p.permissionName),
    });
    setIsDialogOpen(true);
  };

  const handleCreateNew = () => {
    setIsEditing(false);
    setEditingPlan(null);
    resetForm();
    setIsDialogOpen(true);
  };

  const resetForm = () => {
    setFormData({
      planName: "",
      amount: "",
      duration: "",
      permissions: [],
    });
    setNewPermission("");
  };

  const addPermission = () => {
    const trimmedPermission = newPermission.trim();
    if (trimmedPermission && !formData.permissions.includes(trimmedPermission)) {
      setFormData(prev => ({
        ...prev,
        permissions: [...prev.permissions, trimmedPermission]
      }));
      setNewPermission("");
      toast.success("Permission added");
    } else if (formData.permissions.includes(trimmedPermission)) {
      toast.error("Permission already exists");
    }
  };

  const removePermission = (permission: string) => {
    setFormData(prev => ({
      ...prev,
      permissions: prev.permissions.filter(p => p !== permission)
    }));
    toast.success("Permission removed");
  };

  const addDefaultPermission = (permission: string) => {
    if (!formData.permissions.includes(permission)) {
      setFormData(prev => ({
        ...prev,
        permissions: [...prev.permissions, permission]
      }));
      toast.success("Permission added");
    } else {
      toast.error("Permission already exists");
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addPermission();
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Subscription Plans</h1>
          <p className="text-muted-foreground">Manage subscription tiers and pricing</p>
        </div>
        <Button onClick={handleCreateNew}>
          <Plus className="mr-2 h-4 w-4" />
          Add Plan
        </Button>
      </div>

      {/* Cards Grid View */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {plans.map((plan) => (
          <Card key={plan.id} className="relative">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div>
                  <CardTitle className="text-xl">{plan.planName}</CardTitle>
                  <CardDescription>
                    {plan._count.userSubscriptions} subscriber{plan._count.userSubscriptions !== 1 ? "s" : ""}
                  </CardDescription>
                </div>
                <Badge variant="secondary">
                  {plan.permissions.length} permissions
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-sm">
                  <DollarSign className="h-4 w-4 text-muted-foreground" />
                  <span className="font-semibold text-lg">
                    {parseFloat(plan.amount).toLocaleString()} RWF
                  </span>
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <Clock className="h-4 w-4 text-muted-foreground" />
                  <span>{plan.duration} days</span>
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <Users className="h-4 w-4 text-muted-foreground" />
                  <span>{plan._count.userSubscriptions} active</span>
                </div>
              </div>

              {plan.permissions.length > 0 && (
                <div className="space-y-2">
                  <Label className="text-sm">Permissions</Label>
                  <div className="flex flex-wrap gap-1">
                    {plan.permissions.slice(0, 3).map((permission) => (
                      <Badge key={permission.id} variant="outline" className="text-xs">
                        <Key className="h-3 w-3 mr-1" />
                        {permission.permissionName}
                      </Badge>
                    ))}
                    {plan.permissions.length > 3 && (
                      <Badge variant="outline" className="text-xs">
                        +{plan.permissions.length - 3} more
                      </Badge>
                    )}
                  </div>
                </div>
              )}

              <div className="flex gap-2 pt-2">
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="flex-1"
                  onClick={() => handleEdit(plan)}
                >
                  <Edit className="mr-2 h-4 w-4" />
                  Edit
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleDelete(plan.id)}
                  className="text-red-500 hover:text-red-600 hover:bg-red-50"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-8 w-8 animate-spin" />
        </div>
      )}

      {!isLoading && plans.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16">
            <p className="text-muted-foreground mb-4">No subscription plans yet</p>
            <Button onClick={handleCreateNew}>
              <Plus className="mr-2 h-4 w-4" />
              Create Your First Plan
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Create/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {isEditing ? "Edit Subscription Plan" : "Create Subscription Plan"}
            </DialogTitle>
            <DialogDescription>
              {isEditing 
                ? "Update the subscription plan details below." 
                : "Add a new subscription tier with custom pricing and permissions."
              }
            </DialogDescription>
          </DialogHeader>
          
          <form onSubmit={isEditing ? handleUpdate : handleCreate} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="planName">Plan Name *</Label>
                <Input
                  id="planName"
                  placeholder="Premium, Basic, etc."
                  value={formData.planName}
                  onChange={(e) => setFormData({ ...formData, planName: e.target.value })}
                  required
                />
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="duration">Duration (days) *</Label>
                <Input
                  id="duration"
                  type="number"
                  placeholder="30"
                  min="1"
                  value={formData.duration}
                  onChange={(e) => setFormData({ ...formData, duration: e.target.value })}
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="amount">Amount (RWF) *</Label>
              <Input
                id="amount"
                type="number"
                placeholder="5000"
                min="0"
                step="0.01"
                value={formData.amount}
                onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                required
              />
            </div>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Permissions</Label>
                <div className="flex gap-2">
                  <Input
                    placeholder="Add custom permission..."
                    value={newPermission}
                    onChange={(e) => setNewPermission(e.target.value)}
                    onKeyPress={handleKeyPress}
                  />
                  <Button type="button" onClick={addPermission} variant="outline">
                    Add
                  </Button>
                </div>
              </div>

              {/* Default Permissions Quick Add */}
              <div className="space-y-2">
                <Label className="text-sm text-muted-foreground">Quick Add Common Permissions</Label>
                <div className="flex flex-wrap gap-2">
                  {defaultPermissions.map((permission) => (
                    <Button
                      key={permission}
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => addDefaultPermission(permission)}
                      disabled={formData.permissions.includes(permission)}
                    >
                      <Key className="h-3 w-3 mr-1" />
                      {permission}
                    </Button>
                  ))}
                </div>
              </div>

              {/* Selected Permissions */}
              <div className="space-y-2">
                <Label className="text-sm">
                  Selected Permissions ({formData.permissions.length})
                </Label>
                {formData.permissions.length > 0 ? (
                  <div className="flex flex-wrap gap-2 p-3 border rounded-lg bg-muted/50 min-h-12">
                    {formData.permissions.map((permission) => (
                      <Badge key={permission} variant="secondary" className="flex items-center gap-1 py-1">
                        <Key className="h-3 w-3" />
                        {permission}
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          className="h-4 w-4 p-0 hover:bg-transparent ml-1"
                          onClick={() => removePermission(permission)}
                        >
                          <X className="h-3 w-3" />
                        </Button>
                      </Badge>
                    ))}
                  </div>
                ) : (
                  <div className="p-3 border rounded-lg bg-muted/20 text-muted-foreground text-sm">
                    No permissions added yet
                  </div>
                )}
              </div>
            </div>

            <DialogFooter>
              <Button 
                type="button" 
                variant="outline" 
                onClick={() => {
                  setIsDialogOpen(false);
                  resetForm();
                }}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {isEditing ? (
                  <>
                    <Save className="mr-2 h-4 w-4" />
                    Update Plan
                  </>
                ) : (
                  <>
                    <Plus className="mr-2 h-4 w-4" />
                    Create Plan
                  </>
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}