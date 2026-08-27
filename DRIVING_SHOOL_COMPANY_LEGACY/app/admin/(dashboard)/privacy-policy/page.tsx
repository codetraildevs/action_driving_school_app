// app/admin/privacy-policies/page.tsx
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { apiClient } from "@/lib/api-client";
import { Plus, Edit, Trash2, Eye, Loader2, Copy, CheckCircle, XCircle, Globe, Smartphone } from "lucide-react";
import { toast } from "sonner";
import dynamic from 'next/dynamic';

import MDEditor from '@uiw/react-md-editor';

interface PrivacyPolicy {
  id: number;
  version: string;
  title: string;
  content: string;
  isActive: boolean;
  appVersion: string;
  language: string;
  createdAt: string;
  updatedAt: string;
  _count: {
    acceptances: number;
  };
}

export default function PrivacyPoliciesPage() {
  const [policies, setPolicies] = useState<PrivacyPolicy[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<PrivacyPolicy | null>(null);
  const [selectedPolicy, setSelectedPolicy] = useState<PrivacyPolicy | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    version: "",
    title: "",
    content: "",
    isActive: false,
    appVersion: "",
    language: "en"
  });

  useEffect(() => {
    fetchPolicies();
  }, []);

  const fetchPolicies = async () => {
    try {
      const data = await apiClient.get<{ data: PrivacyPolicy[] }>("/api/admin/privacy-policies");
      setPolicies(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch privacy policies");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      await apiClient.post("/api/admin/privacy-policies", formData);
      toast.success("Privacy policy created successfully");
      setIsDialogOpen(false);
      resetForm();
      fetchPolicies();
    } catch (error: any) {
      toast.error(error.message || "Failed to create privacy policy");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPolicy) return;

    setIsSubmitting(true);

    try {
      await apiClient.put(`/api/admin/privacy-policies/${editingPolicy.id}`, formData);
      toast.success("Privacy policy updated successfully");
      setIsDialogOpen(false);
      resetForm();
      fetchPolicies();
    } catch (error: any) {
      toast.error(error.message || "Failed to update privacy policy");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this privacy policy?")) return;

    try {
      await apiClient.delete(`/api/admin/privacy-policies/${id}`);
      toast.success("Privacy policy deleted successfully");
      fetchPolicies();
    } catch (error: any) {
      toast.error(error.message || "Failed to delete privacy policy");
    }
  };

  const handleSetActive = async (policy: PrivacyPolicy) => {
    try {
      await apiClient.patch(`/api/admin/privacy-policies/${policy.id}`, {
        isActive: !policy.isActive
      });
      toast.success(`Policy ${!policy.isActive ? 'activated' : 'deactivated'} successfully`);
      fetchPolicies();
    } catch (error: any) {
      toast.error(error.message || "Failed to update policy status");
    }
  };

  const handleEdit = (policy: PrivacyPolicy) => {
    setEditingPolicy(policy);
    setIsEditing(true);
    setFormData({
      version: policy.version,
      title: policy.title,
      content: policy.content,
      isActive: policy.isActive,
      appVersion: policy.appVersion,
      language: policy.language
    });
    setIsDialogOpen(true);
  };

  const handleView = (policy: PrivacyPolicy) => {
    setSelectedPolicy(policy);
    setIsViewDialogOpen(true);
  };

  const handleCreateNew = () => {
    setIsEditing(false);
    setEditingPolicy(null);
    resetForm();
    setIsDialogOpen(true);
  };

  const resetForm = () => {
    setFormData({
      version: "",
      title: "",
      content: "",
      isActive: false,
      appVersion: "",
      language: "en"
    });
  };

  const copyPublicUrl = (policy: PrivacyPolicy) => {
    const url = `${window.location.origin}/privacy-policy?v=${policy.version}&version=${policy.appVersion}&language=${policy.language}`;
    navigator.clipboard.writeText(url);
    toast.success("Public URL copied to clipboard");
  };

  const getLanguageFlag = (language: string) => {
    const flags: { [key: string]: string } = {
      en: "🇺🇸",
      fr: "🇫🇷",
      es: "🇪🇸",
      de: "🇩🇪",
      rw: "🇷🇼"
    };
    return flags[language] || "🌐";
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Privacy Policies</h1>
          <p className="text-muted-foreground">Manage privacy policy versions for different app versions</p>
        </div>
        <Button onClick={handleCreateNew}>
          <Plus className="mr-2 h-4 w-4" />
          New Policy
        </Button>
      </div>

      {/* Policies Table */}
      <Card>
        <CardHeader>
          <CardTitle>All Privacy Policies</CardTitle>
          <CardDescription>
            {policies.length} policy version{policies.length !== 1 ? 's' : ''} configured
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : policies.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No privacy policies configured
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Version</TableHead>
                  <TableHead>Title</TableHead>
                  <TableHead>App Version</TableHead>
                  <TableHead>Language</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Acceptances</TableHead>
                  <TableHead>Last Updated</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {policies.map((policy) => (
                  <TableRow key={policy.id}>
                    <TableCell className="font-medium">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">v{policy.version}</Badge>
                        {policy.isActive && (
                          <CheckCircle className="h-4 w-4 text-green-500" />
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="max-w-xs">
                      <div className="line-clamp-2">{policy.title}</div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Smartphone className="h-4 w-4 text-muted-foreground" />
                        {policy.appVersion}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <span className="text-lg">{getLanguageFlag(policy.language)}</span>
                        <Badge variant="outline" className="uppercase">
                          {policy.language}
                        </Badge>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Switch
                          checked={policy.isActive}
                          onCheckedChange={() => handleSetActive(policy)}
                        />
                        <span className="text-sm">
                          {policy.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">
                        {policy._count.acceptances} users
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {new Date(policy.updatedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => copyPublicUrl(policy)}
                          title="Copy Public URL"
                        >
                          <Copy className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleView(policy)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleEdit(policy)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(policy.id)}
                          className="text-red-500 hover:text-red-600"
                          disabled={policy._count.acceptances > 0}
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

      {/* Create/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-4xl h-[90vh] flex flex-col">
          <DialogHeader>
            <DialogTitle>
              {isEditing ? "Edit Privacy Policy" : "Create Privacy Policy"}
            </DialogTitle>
            <DialogDescription>
              {isEditing 
                ? "Update the privacy policy details below." 
                : "Create a new privacy policy version for specific app version."
              }
            </DialogDescription>
          </DialogHeader>
          
          <form onSubmit={isEditing ? handleUpdate : handleCreate} className="flex-1 flex flex-col">
            <Tabs defaultValue="basic" className="flex-1 flex flex-col">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="basic">Basic Info</TabsTrigger>
                <TabsTrigger value="content">Policy Content</TabsTrigger>
              </TabsList>
              
              <TabsContent value="basic" className="flex-1 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="version">Policy Version *</Label>
                    <Input
                      id="version"
                      placeholder="2.1.0"
                      value={formData.version}
                      onChange={(e) => setFormData(prev => ({ ...prev, version: e.target.value }))}
                      required
                    />
                    <p className="text-xs text-muted-foreground">
                      Version number for this policy (e.g., 1.0, 2.1)
                    </p>
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="appVersion">App Version *</Label>
                    <Input
                      id="appVersion"
                      placeholder="2.0.0"
                      value={formData.appVersion}
                      onChange={(e) => setFormData(prev => ({ ...prev, appVersion: e.target.value }))}
                      required
                    />
                    <p className="text-xs text-muted-foreground">
                      App version this policy applies to
                    </p>
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="title">Policy Title *</Label>
                  <Input
                    id="title"
                    placeholder="Privacy Policy for App Version 2.0"
                    value={formData.title}
                    onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="language">Language</Label>
                    <Select
                      value={formData.language}
                      onValueChange={(value) => setFormData(prev => ({ ...prev, language: value }))}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select language" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="en">English</SelectItem>
                        <SelectItem value="fr">French</SelectItem>
                        <SelectItem value="rw">Kinyarwanda</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="isActive">Activate Policy</Label>
                    <div className="flex items-center space-x-2 pt-2">
                      <Switch
                        id="isActive"
                        checked={formData.isActive}
                        onCheckedChange={(checked) => setFormData(prev => ({ ...prev, isActive: checked }))}
                      />
                      <Label htmlFor="isActive" className="text-sm">
                        Set as active version for {formData.language}
                      </Label>
                    </div>
                  </div>
                </div>
              </TabsContent>
              
              <TabsContent value="content" className="flex-1 flex flex-col">
                <div className="space-y-2 flex-1 flex flex-col">
                  <Label htmlFor="content">Policy Content (Markdown) *</Label>
                  <div className="flex-1 min-h-[400px]">
                    <MDEditor
                      value={formData.content}
                      onChange={(value) => setFormData(prev => ({ ...prev, content: value || '' }))}
                      preview="edit"
                      height={400}
                    />
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Use markdown to format your privacy policy. Supports headers, lists, links, and more.
                  </p>
                </div>
              </TabsContent>
            </Tabs>

            <DialogFooter className="mt-6">
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
                {isEditing ? "Update Policy" : "Create Policy"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* View Policy Dialog */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent className="max-w-4xl h-[90vh] overflow-y-auto flex flex-col">
          <DialogHeader>
            <DialogTitle>Privacy Policy Preview</DialogTitle>
            <DialogDescription>
              Preview how users will see this privacy policy
            </DialogDescription>
          </DialogHeader>
          
          {selectedPolicy && (
            <div className="flex-1 flex flex-col space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold">{selectedPolicy.title}</h3>
                  <div className="flex items-center gap-4 text-sm text-muted-foreground mt-1">
                    <span>Version: {selectedPolicy.version}</span>
                    <span>App: {selectedPolicy.appVersion}</span>
                    <span>Language: {selectedPolicy.language}</span>
                    <span>Acceptances: {selectedPolicy._count.acceptances}</span>
                  </div>
                </div>
                <Button
                  variant="outline"
                  onClick={() => copyPublicUrl(selectedPolicy)}
                >
                  <Copy className="mr-2 h-4 w-4" />
                  Copy URL
                </Button>
              </div>

              <div className="flex-1 border rounded-lg p-6 overflow-y-auto">
                <div className="prose prose-sm max-w-none">
                  <MDEditor.Markdown source={selectedPolicy.content} />
                </div>
              </div>

              <div className="text-sm text-muted-foreground">
                Last updated: {new Date(selectedPolicy.updatedAt).toLocaleString()}
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