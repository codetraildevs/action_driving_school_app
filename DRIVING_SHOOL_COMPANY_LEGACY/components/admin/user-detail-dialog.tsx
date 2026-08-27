"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { apiClient } from "@/lib/api-client";
import { toast } from "sonner";
import { Loader2, User, CreditCard, Key } from "lucide-react";

interface UserDetailDialogProps {
  user: {
    id: number;
    firstName: string;
    lastName?: string;
    email: string;
    phoneNumber: string;
    isActive: boolean;
    role: { roleName: string };
    userSubscription?: {
      id: number;
      startDate: any;
      endDate: any;
      subscriptionPlan: {
        id: number;

        planName: string;
      };
    };
  } | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdate: () => void;
}

interface SubscriptionPlan {
  id: number;
  planName: string;
  amount: string;
  startDate: any;
  endDate: any;
  duration: number;
}

export function UserDetailDialog({
  user,
  open,
  onOpenChange,
  onUpdate,
}: UserDetailDialogProps) {
  const [subscriptionPlans, setSubscriptionPlans] = useState<
    SubscriptionPlan[]
  >([]);
  const [selectedPlan, setSelectedPlan] = useState<string>("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (open) {
      fetchSubscriptionPlans();
      if (user?.userSubscription) {
        setSelectedPlan(user.userSubscription.subscriptionPlan.id.toString());
      }
    }
  }, [open, user]);

  const fetchSubscriptionPlans = async () => {
    try {
      const data = await apiClient.get<{ data: SubscriptionPlan[] }>(
        "/api/subscriptions"
      );
      setSubscriptionPlans(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch subscription plans");
    }
  };

  const handleUpdateSubscription = async () => {
    if (!selectedPlan) {
      toast.error("Please select a subscription plan");
      return;
    }

    setIsLoading(true);
    try {
      await apiClient.post(`/api/admin/users/${user?.id}/subscription`, {
        subscriptionPlanId: parseInt(selectedPlan),
      });
      toast.success("User subscription updated successfully");
      onUpdate();
      onOpenChange(false);
    } catch (error) {
      toast.error("Failed to update subscription: " + (error as Error).message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPassword = async () => {
    if (!newPassword || !confirmPassword) {
      toast.error("Please fill in all password fields");
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error("Passwords do not match");
      return;
    }

    if (newPassword.length < 8) {
      toast.error("Password must be at least 8 characters");
      return;
    }

    setIsLoading(true);
    try {
      await apiClient.post(`/api/admin/users/${user?.id}/reset-password`, {
        newPassword,
      });
      toast.success("Password reset successfully");
      setNewPassword("");
      setConfirmPassword("");
      onUpdate();
    } catch (error) {
      toast.error("Failed to reset password: " + (error as Error).message);
    } finally {
      setIsLoading(false);
    }
  };

  if (!user) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <User className="h-5 w-5" />
            Manage User: {user.firstName} {user.lastName}
          </DialogTitle>
          <DialogDescription>
            Update user subscription or reset their password
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="info" className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="info">
              <User className="h-4 w-4 mr-2" />
              <span className="sm:hidden"> Info</span>
            </TabsTrigger>
            <TabsTrigger value="subscription">
              <CreditCard className="h-4 w-4 mr-2" />
              <span className="sm:hidden">Subscription</span>
            </TabsTrigger>
            {/* <TabsTrigger value="password">
              <Key className="h-4 w-4 mr-2" />
              <span className="sm:hidden"> Password</span>
            </TabsTrigger> */}
          </TabsList>

          {/* User Info Tab */}
          <TabsContent value="info" className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <Label className="text-muted-foreground">Name</Label>
                <p className="font-medium">
                  {user.firstName} {user.lastName}
                </p>
              </div>
              <div>
                <Label className="text-muted-foreground">Email</Label>
                <p className="font-medium">{user.email}</p>
              </div>
              <div>
                <Label className="text-muted-foreground">Phone</Label>
                <p className="font-medium">{user.phoneNumber}</p>
              </div>
              <div>
                <Label className="text-muted-foreground">Role</Label>
                <Badge variant="outline">{user.role.roleName}</Badge>
              </div>
              <div>
                <Label className="text-muted-foreground">Status</Label>
                <Badge variant={user.isActive ? "default" : "secondary"}>
                  {user.isActive ? "Active" : "Inactive"}
                </Badge>
              </div>
            </div>
          </TabsContent>

          {/* Subscription Management Tab */}
          <TabsContent value="subscription" className="space-y-4">
            <div className="space-y-4">
              {user.userSubscription ? (
                <div className="flex flex-col   gap-1">
                  <div className="flex justify-between gap-1">
                    <Label className="text-muted-foreground">
                      Current Subscription
                    </Label>

                    <Badge>
                      {user.userSubscription.subscriptionPlan.planName}
                    </Badge>
                  </div>

                  <div className="flex justify-between gap-1">
                    <Label className="text-muted-foreground">Start On</Label>

                    <span>
                      {user.userSubscription.startDate
                        ? new Date(user.userSubscription?.startDate).toLocaleDateString()
                        : "N/A"}
                    </span>
                  </div>

                  <div className="flex justify-between gap-1">
                    <Label className="text-muted-foreground">End On</Label>

                    <span>
                      {user.userSubscription.endDate
                        ? new Date(user?.userSubscription?.endDate).toLocaleDateString()
                        : "N/A"}
                    </span>
                  </div>

                      <div className="flex justify-between gap-1">
                    <Label className="text-muted-foreground">Status</Label>

                  { user.userSubscription.endDate&& <span>
                      {
                      new Date(user.userSubscription.endDate)> new Date()   ? <Badge variant={"default"}>Active</Badge>
                        :  <Badge variant={"destructive"}>Expired</Badge>}
                    </span>}
                  </div>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">No subscription</p>
              )}
              <div>
                <Label htmlFor="subscription-plan py-2">
                  Subscription Plan
                </Label>
                <Select value={selectedPlan} onValueChange={setSelectedPlan}>
                  <SelectTrigger id="subscription-plan">
                    <SelectValue placeholder="Select a subscription plan" />
                  </SelectTrigger>
                  <SelectContent>
                    {subscriptionPlans.map((plan) => (
                      <SelectItem key={plan.id} value={plan.id.toString()}>
                        {plan.planName} -{" "}
                        {parseFloat(plan.amount).toLocaleString()} RWF (
                        {plan.duration} days)
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-sm text-muted-foreground mt-2">
                  {user.userSubscription
                    ? "Update the user's current subscription plan"
                    : "Assign a subscription plan to this user"}
                </p>
              </div>

              <Button
                onClick={handleUpdateSubscription}
                disabled={isLoading || !selectedPlan}
                className="w-full"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Updating...
                  </>
                ) : (
                  "Update Subscription"
                )}
              </Button>
            </div>
          </TabsContent>

          {/* Password Reset Tab */}
          <TabsContent value="password" className="space-y-4">
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="new-password">New Password</Label>
                <Input
                  id="new-password"
                  type="password"
                  placeholder="Enter new password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirm-password">Confirm Password</Label>
                <Input
                  id="confirm-password"
                  type="password"
                  placeholder="Confirm new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>

              <div className="rounded-md bg-amber-50 dark:bg-amber-950/20 p-3">
                <p className="text-sm text-amber-800 dark:text-amber-400">
                  ⚠️ This will immediately change the user&apos;s password. They
                  will need to use the new password to log in.
                </p>
              </div>

              <Button
                onClick={handleResetPassword}
                disabled={isLoading}
                className="w-full"
                variant="destructive"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Resetting...
                  </>
                ) : (
                  "Reset Password"
                )}
              </Button>
            </div>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
