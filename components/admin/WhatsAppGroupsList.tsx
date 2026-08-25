"use client";

import { useState, useEffect } from "react";

import { GroupDialog } from "./GroupDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  MessageCircle,
  Plus,
  Search,
  Filter,
  Users,
  Loader2,
} from "lucide-react";
import { WhatsAppGroup } from "@/lib/generated/prisma";
import { toast } from "sonner";
import { GroupCard } from "./groupcard";
import { apiClient } from "@/lib/api-client";

interface WhatsAppGroupsListProps {
  initialGroups?: WhatsAppGroup[];
}

export function WhatsAppGroupsList({
  initialGroups = [],
}: WhatsAppGroupsListProps) {
  const [groups, setGroups] = useState<WhatsAppGroup[]>(initialGroups);
  const [filteredGroups, setFilteredGroups] =
    useState<WhatsAppGroup[]>(initialGroups);
  const [isLoading, setIsLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<WhatsAppGroup | null>(
    null
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [groupToDelete, setGroupToDelete] = useState<WhatsAppGroup | null>(
    null
  );

  useEffect(() => {
    fetchGroups();
  }, []);

  useEffect(() => {
    filterGroups();
  }, [groups, searchTerm, statusFilter]);

  const fetchGroups = async () => {
    setIsLoading(true);
    try {
      const response = (await apiClient.get(
        "/api/admin/whatsapp-groups"
      )) as any;
      if (response) {
        const data = response;
        setGroups(data);
      }
    } catch (error) {
      console.error("Error fetching groups:", error);
      toast.error("Failed to load groups");
    } finally {
      setIsLoading(false);
    }
  };

  const filterGroups = () => {
    let filtered = groups;

    // Apply search filter
    if (searchTerm) {
      filtered = filtered.filter(
        (group) =>
          group.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          group.description?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Apply status filter
    if (statusFilter === "active") {
      filtered = filtered.filter((group) => group.isActive);
    } else if (statusFilter === "inactive") {
      filtered = filtered.filter((group) => !group.isActive);
    }

    setFilteredGroups(filtered);
  };

  const handleCreateGroup = () => {
    setSelectedGroup(null);
    setIsDialogOpen(true);
  };

  const handleEditGroup = (group: WhatsAppGroup) => {
    setSelectedGroup(group);
    setIsDialogOpen(true);
  };

  const handleDeleteGroup = (group: WhatsAppGroup) => {
    setGroupToDelete(group);
    setDeleteDialogOpen(true);
  };

  const confirmDelete = async () => {
    if (!groupToDelete) return;

    try {
      const response = (await apiClient.delete(
        `/api/admin/whatsapp-groups/${groupToDelete.id}`
      )) as any;

      if (response) {
        setGroups(groups.filter((g) => g.id !== groupToDelete.id));
        toast.success("Group deleted successfully");
      } else {
        toast.error("Failed to delete group");
      }
    } catch (error) {
      console.error("Error deleting group:", error);
      toast.error("Failed to delete group");
    } finally {
      setDeleteDialogOpen(false);
      setGroupToDelete(null);
    }
  };

  const handleSubmit = async (data: any) => {
    setIsSubmitting(true);
    try {
      const url = selectedGroup
        ? `/api/admin/whatsapp-groups/${selectedGroup.id}`
        : "/api/admin/whatsapp-groups";

      const method = selectedGroup ? "PATCH" : "POST";

      const response =
        method === "PATCH"
          ? await apiClient.patch(url, data)
          : ((await apiClient.post(
              url,

              data
            )) as any);

      if (response) {
        const updatedGroup = response;

        if (selectedGroup) {
          setGroups(
            groups.map((g) => (g.id === updatedGroup.id ? updatedGroup : g))
          );
          toast.success("Group updated successfully");
        } else {
          setGroups([updatedGroup, ...groups]);
          toast.success("Group created successfully");
        }

        setIsDialogOpen(false);
      } else {
        const error = await response;
        toast.error(error.error || "Failed to save group");
      }
    } catch (error) {
      console.error("Error saving group:", error);
      toast.error("Failed to save group");
    } finally {
      setIsSubmitting(false);
    }
  };

  const stats = {
    total: groups.length,
    active: groups.filter((g) => g.isActive).length,
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-foreground flex items-center gap-3">
            <MessageCircle className="h-8 w-8 text-primary" />
            WhatsApp Groups
          </h1>
          <p className="text-muted-foreground mt-2">
            Manage learning groups and their members
          </p>
        </div>
        <Button
          onClick={handleCreateGroup}
          className="bg-primary text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4 mr-2" />
          Create Group
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-card border border-border rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Total Groups</p>
              <p className="text-2xl font-bold text-foreground">
                {stats.total}
              </p>
            </div>
            <MessageCircle className="h-8 w-8 text-primary/60" />
          </div>
        </div>

        <div className="bg-card border border-border rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Active Groups</p>
              <p className="text-2xl font-bold text-foreground">
                {stats.active}
              </p>
            </div>
            <Badge className="bg-primary text-primary-foreground">Live</Badge>
          </div>
        </div>

     
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search groups..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 bg-background border-input"
          />
        </div>

        <div className="flex gap-2">
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-[180px] bg-background border-input">
              <Filter className="h-4 w-4 mr-2" />
              <SelectValue placeholder="Filter by status" />
            </SelectTrigger>
            <SelectContent className="bg-background border-border">
              <SelectItem value="all">All Groups</SelectItem>
              <SelectItem value="active">Active Only</SelectItem>
              <SelectItem value="inactive">Inactive Only</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Groups List */}
      {isLoading ? (
        <div className="flex justify-center items-center h-64">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : filteredGroups.length === 0 ? (
        <div className="text-center py-12">
          <MessageCircle className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
          <h3 className="text-lg font-medium text-foreground mb-2">
            {searchTerm ? "No groups found" : "No groups yet"}
          </h3>
          <p className="text-muted-foreground mb-6">
            {searchTerm
              ? "Try adjusting your search or filters"
              : "Create your first WhatsApp learning group to get started"}
          </p>
          {!searchTerm && (
            <Button
              onClick={handleCreateGroup}
              className="bg-primary text-primary-foreground hover:bg-primary/90"
            >
              <Plus className="h-4 w-4 mr-2" />
              Create Group
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredGroups.map((group) => (
            <GroupCard
              key={group.id}
              group={group}
              onEdit={handleEditGroup}
              onDelete={handleDeleteGroup}
            />
          ))}
        </div>
      )}

      {/* Create/Edit Dialog */}
      <GroupDialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        group={selectedGroup}
        onSubmit={handleSubmit}
        isSubmitting={isSubmitting}
      />

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent className="bg-background border-border">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-foreground">
              Delete Group
            </AlertDialogTitle>
            <AlertDialogDescription className="text-muted-foreground">
              Are you sure you want to delete "{groupToDelete?.name}"? This
              action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setDeleteDialogOpen(false)}
              className="border-input text-foreground hover:bg-accent"
            >
              Cancel
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={confirmDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
