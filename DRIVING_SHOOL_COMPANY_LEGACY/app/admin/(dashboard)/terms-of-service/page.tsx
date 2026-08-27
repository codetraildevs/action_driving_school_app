// app/admin/terms-of-service/page.tsx
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
import { Plus, Edit, Trash2, Eye, Loader2, Copy, CheckCircle, FileText, Smartphone, Users } from "lucide-react";
import { toast } from "sonner";
import MDEditor from '@uiw/react-md-editor';

interface TermsOfService {
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

export default function TermsOfServicePage() {
  const [terms, setTerms] = useState<TermsOfService[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editingTerms, setEditingTerms] = useState<TermsOfService | null>(null);
  const [selectedTerms, setSelectedTerms] = useState<TermsOfService | null>(null);
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
    fetchTerms();
  }, []);

  const fetchTerms = async () => {
    try {
      const data = await apiClient.get<{ data: TermsOfService[] }>("/api/admin/terms-of-service");
      setTerms(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch terms of service");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      await apiClient.post("/api/admin/terms-of-service", formData);
      toast.success("Terms of service created successfully");
      setIsDialogOpen(false);
      resetForm();
      fetchTerms();
    } catch (error: any) {
      toast.error(error.message || "Failed to create terms of service");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingTerms) return;

    setIsSubmitting(true);

    try {
      await apiClient.put(`/api/admin/terms-of-service/${editingTerms.id}`, formData);
      toast.success("Terms of service updated successfully");
      setIsDialogOpen(false);
      resetForm();
      fetchTerms();
    } catch (error: any) {
      toast.error(error.message || "Failed to update terms of service");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete these terms of service?")) return;

    try {
      await apiClient.delete(`/api/admin/terms-of-service/${id}`);
      toast.success("Terms of service deleted successfully");
      fetchTerms();
    } catch (error: any) {
      toast.error(error.message || "Failed to delete terms of service");
    }
  };

  const handleSetActive = async (terms: TermsOfService) => {
    try {
      await apiClient.patch(`/api/admin/terms-of-service/${terms.id}`, {
        isActive: !terms.isActive
      });
      toast.success(`Terms ${!terms.isActive ? 'activated' : 'deactivated'} successfully`);
      fetchTerms();
    } catch (error: any) {
      toast.error(error.message || "Failed to update terms status");
    }
  };

  const handleEdit = (terms: TermsOfService) => {
    setEditingTerms(terms);
    setIsEditing(true);
    setFormData({
      version: terms.version,
      title: terms.title,
      content: terms.content,
      isActive: terms.isActive,
      appVersion: terms.appVersion,
      language: terms.language
    });
    setIsDialogOpen(true);
  };

  const handleView = (terms: TermsOfService) => {
    setSelectedTerms(terms);
    setIsViewDialogOpen(true);
  };

  const handleCreateNew = () => {
    setIsEditing(false);
    setEditingTerms(null);
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

  const copyPublicUrl = (terms: TermsOfService) => {
    const url = `${window.location.origin}/terms-of-service?v=${terms.version}&version=${terms.appVersion}&language=${terms.language}`;
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
          <h1 className="text-3xl font-bold tracking-tight">Terms of Service</h1>
          <p className="text-muted-foreground">Manage terms of service versions for different app versions</p>
        </div>
        <Button onClick={handleCreateNew}>
          <Plus className="mr-2 h-4 w-4" />
          New Terms
        </Button>
      </div>

      {/* Terms Table */}
      <Card>
        <CardHeader>
          <CardTitle>All Terms of Service</CardTitle>
          <CardDescription>
            {terms.length} terms version{terms.length !== 1 ? 's' : ''} configured
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : terms.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No terms of service configured
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
                {terms.map((term) => (
                  <TableRow key={term.id}>
                    <TableCell className="font-medium">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">v{term.version}</Badge>
                        {term.isActive && (
                          <CheckCircle className="h-4 w-4 text-green-500" />
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="max-w-xs">
                      <div className="line-clamp-2">{term.title}</div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Smartphone className="h-4 w-4 text-muted-foreground" />
                        {term.appVersion}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <span className="text-lg">{getLanguageFlag(term.language)}</span>
                        <Badge variant="outline" className="uppercase">
                          {term.language}
                        </Badge>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Switch
                          checked={term.isActive}
                          onCheckedChange={() => handleSetActive(term)}
                        />
                        <span className="text-sm">
                          {term.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Users className="h-4 w-4 text-muted-foreground" />
                        <Badge variant="secondary">
                          {term._count.acceptances}
                        </Badge>
                      </div>
                    </TableCell>
                    <TableCell>
                      {new Date(term.updatedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => copyPublicUrl(term)}
                          title="Copy Public URL"
                        >
                          <Copy className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleView(term)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleEdit(term)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(term.id)}
                          className="text-red-500 hover:text-red-600"
                          disabled={term._count.acceptances > 0}
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
              {isEditing ? "Edit Terms of Service" : "Create Terms of Service"}
            </DialogTitle>
            <DialogDescription>
              {isEditing 
                ? "Update the terms of service details below." 
                : "Create new terms of service for specific app version."
              }
            </DialogDescription>
          </DialogHeader>
          
          <form onSubmit={isEditing ? handleUpdate : handleCreate} className="flex-1 flex flex-col">
            <Tabs defaultValue="basic" className="flex-1 flex flex-col">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="basic">Basic Info</TabsTrigger>
                <TabsTrigger value="content">Terms Content</TabsTrigger>
              </TabsList>
              
              <TabsContent value="basic" className="flex-1 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="version">Terms Version *</Label>
                    <Input
                      id="version"
                      placeholder="2.1.0"
                      value={formData.version}
                      onChange={(e) => setFormData(prev => ({ ...prev, version: e.target.value }))}
                      required
                    />
                    <p className="text-xs text-muted-foreground">
                      Version number for these terms (e.g., 1.0, 2.1)
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
                      App version these terms apply to
                    </p>
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="title">Terms Title *</Label>
                  <Input
                    id="title"
                    placeholder="Terms of Service for App Version 2.0"
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
                    <Label htmlFor="isActive">Activate Terms</Label>
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
                  <Label htmlFor="content">Terms Content (Markdown) *</Label>
                  <div className="flex-1 min-h-[400px]">
                    <MDEditor
                      value={formData.content}
                      onChange={(value) => setFormData(prev => ({ ...prev, content: value || '' }))}
                      preview="edit"
                      height={400}
                    />
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Use markdown to format your terms of service. Supports headers, lists, links, and more.
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
                {isEditing ? "Update Terms" : "Create Terms"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* View Terms Dialog */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent className="max-w-4xl h-[90vh] flex flex-col overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Terms of Service Preview</DialogTitle>
            <DialogDescription>
              Preview how users will see these terms of service
            </DialogDescription>
          </DialogHeader>
          
          {selectedTerms && (
            <div className="flex-1 flex flex-col space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold">{selectedTerms.title}</h3>
                  <div className="flex items-center gap-4 text-sm text-muted-foreground mt-1">
                    <span>Version: {selectedTerms.version}</span>
                    <span>App: {selectedTerms.appVersion}</span>
                    <span>Language: {selectedTerms.language}</span>
                    <span>Acceptances: {selectedTerms._count.acceptances}</span>
                  </div>
                </div>
                <Button
                  variant="outline"
                  onClick={() => copyPublicUrl(selectedTerms)}
                >
                  <Copy className="mr-2 h-4 w-4" />
                  Copy URL
                </Button>
              </div>

              <div className="flex-1 border rounded-lg p-6 overflow-y-auto">
                <div className="prose prose-sm max-w-none">
                  <MDEditor.Markdown source={selectedTerms.content} />
                </div>
              </div>

              <div className="text-sm text-muted-foreground">
                Last updated: {new Date(selectedTerms.updatedAt).toLocaleString()}
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