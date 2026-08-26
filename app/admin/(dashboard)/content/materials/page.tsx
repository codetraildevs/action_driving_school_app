// frontend/components/learning-materials-page.tsx
"use client";

import { useEffect, useState } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { apiClient } from "@/lib/api-client";
import {
  Plus,
  Search,
  Edit,
  Trash2,
  Loader2,
  FileText,
  Video,
  Image as ImageIcon,
  Eye,
  EyeOff,
} from "lucide-react";
import { toast } from "sonner";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface LearningMaterial {
  id: number;
  title: string;
  description: string;
  fileType: string;
  filePath: string;
  thumbnailUrl?: string;
  isPublic: boolean;
  createdAt: string;
}

interface MaterialFormData {
  title: string;
  description: string;
  isPublic: boolean;
  file?: string;
}

interface FileItem {
  id: number;
  name: string;
  description: string;
  filePath: string;
  fileType: string;
  fileSize: number;
  thumbnailUrl?: string;
  folderId?: number;
  folder?: {
    id: number;
    name: string;
    path: string;
  };
  createdAt: string;
  updatedAt: string;
}

export default function LearningMaterialsPage() {
  const [materials, setMaterials] = useState<LearningMaterial[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedMaterial, setSelectedMaterial] =
    useState<LearningMaterial | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [files, setFiles] = useState<FileItem[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [isBulkDeleting, setIsBulkDeleting] = useState(false);
  const [formData, setFormData] = useState<MaterialFormData>({
    title: "",
    description: "",
    isPublic: true,
  });

  const fetchFiles = async () => {
    try {
      const data = await apiClient.get<{ data: FileItem[] }>("/api/files");
      setFiles(data.data || []);
      if (data.data.length > 0) {
        setFormData((prev) => ({ ...prev, file: data.data[0].id.toString() }));
      }
    } catch (error) {
      toast.error("Failed to fetch files");
    }
  };

  useEffect(() => {
    Promise.all([fetchFiles(), fetchMaterials()]);
  }, []);

  const fetchMaterials = async () => {
    try {
      const data = await apiClient.get<{ materials: LearningMaterial[] }>(
        "/api/learning-materials"
      );
      setMaterials(data.materials || []);
    } catch (error) {
      toast.error("Failed to fetch materials");
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this material?")) return;
    const materialToDelete = materials.find((material) => material.id === id);
    try {
      // Optimistically update the UI

      setMaterials((prev) => prev.filter((material) => material.id !== id));

      await apiClient.delete(`/api/learning-materials/${id}`);
      toast.success("Material deleted successfully");
    } catch (error) {
      // Revert on error
      if (materialToDelete) {
        setMaterials((prev) =>
          [...prev, materialToDelete].sort((a, b) => a.id - b.id)
        );
      }
      toast.error("Failed to delete material");
    }
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    if (
      !confirm(
        `Are you sure you want to delete ${selectedIds.length} material(s)? This action cannot be undone.`
      )
    )
      return;

    setIsBulkDeleting(true);
    try {
      // Optimistically remove from UI
      setMaterials((prev) => prev.filter((m) => !selectedIds.includes(m.id)));
      setSelectedIds([]);

      await apiClient.post(`/api/learning-materials/bulk-delete`, {
        ids: selectedIds,
      });
      toast.success(`${selectedIds.length} material(s) deleted successfully`);
    } catch (error) {
      toast.error("Failed to delete materials");
      fetchMaterials(); // Refetch on error
    } finally {
      setIsBulkDeleting(false);
    }
  };

  const toggleSelectAll = () => {
    if (selectedIds.length === filteredMaterials.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(filteredMaterials.map((m) => m.id));
    }
  };

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  const handleUpload = async () => {
    if (!formData.title.trim()) {
      toast.error("Title is required");
      return;
    }

    if (!formData.file) {
      toast.error("Please select a file");
      return;
    }

    setIsSubmitting(true);

    try {
      const selectedFile = files.find((f) => f.id.toString() === formData.file);

      // Optimistically create material
      const newMaterial: LearningMaterial = {
        id: Date.now(), // Temporary ID
        title: formData.title,
        description: formData.description,
        fileType: selectedFile?.fileType || "",
        filePath: selectedFile?.filePath || "",
        thumbnailUrl: selectedFile?.thumbnailUrl,
        isPublic: formData.isPublic,
        createdAt: new Date().toISOString(),
      };

      setMaterials((prev) => [newMaterial, ...prev]);

      const uploadData = {
        fileId: formData.file,
        title: formData.title,
        description: formData.description,
        isPublic: formData.isPublic.toString(),
      };

      const response = (await apiClient.post(
        "/api/learning-materials/upload",
        uploadData
      )) as any;

      toast.success("Material created successfully");
      setIsUploadModalOpen(false);
      resetForm();

      // Replace temporary material with actual data
      setMaterials((prev) =>
        prev.map((material) =>
          material.id === newMaterial.id ? response : material
        )
      );
    } catch (error) {
      // Revert optimistic update
      setMaterials((prev) =>
        prev.filter((material) => material.id !== Date.now())
      );
      toast.error("Failed to create material");
      console.error("Upload error:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEdit = async () => {
    if (!selectedMaterial || !formData.title.trim()) {
      toast.error("Title is required");
      return;
    }

    setIsSubmitting(true);

    try {
      // Optimistically update the UI
      setMaterials((prev) =>
        prev.map((material) =>
          material.id === selectedMaterial.id
            ? { ...material, ...formData }
            : material
        )
      );

      await apiClient.put(`/api/learning-materials/${selectedMaterial.id}`, {
        file: formData.file,
        title: formData.title,
        description: formData.description,
        isPublic: formData.isPublic,
      });

      toast.success("Material updated successfully");
      setIsEditModalOpen(false);
      resetForm();
    } catch (error) {
      // Revert on error
      fetchMaterials(); // Refetch to get correct data
      toast.error("Failed to update material");
    } finally {
      setIsSubmitting(false);
    }
  };

  const openEditModal = (material: LearningMaterial) => {
    setSelectedMaterial(material);
    setFormData({
      title: material.title,
      description: material.description,
      isPublic: material.isPublic,
      file: material.id.toString(), // Using material ID as file reference
    });
    setIsEditModalOpen(true);
  };

  const resetForm = () => {
    setFormData({
      title: "",
      description: "",
      isPublic: true,
    });
    setSelectedMaterial(null);
  };

  const getFileIcon = (fileType: string) => {
    if (fileType.includes("video")) return <Video className="h-4 w-4" />;
    if (fileType.includes("image")) return <ImageIcon className="h-4 w-4" />;
    return <FileText className="h-4 w-4" />;
  };

  const getFileTypeCategory = (fileType: string) => {
    if (fileType.includes("video")) return "Video";
    if (fileType.includes("image")) return "Image";
    if (fileType.includes("pdf")) return "PDF";
    if (fileType.includes("word") || fileType.includes("document"))
      return "Document";
    if (fileType.includes("powerpoint") || fileType.includes("presentation"))
      return "Presentation";
    if (fileType.includes("audio")) return "Audio";
    return "File";
  };

  const filteredMaterials = materials.filter(
    (material) =>
      material.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      material.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Mobile Card Component
  const MaterialCard = ({ material }: { material: LearningMaterial }) => (
    <Card className="p-4 border-none">
      <div className="space-y-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3 flex-1">
            {material.thumbnailUrl ? (
              <img
                src={material.thumbnailUrl}
                alt={material.title}
                className="h-12 w-12 object-cover rounded-lg border"
              />
            ) : (
              <div className="h-12 w-12 bg-muted rounded-lg border flex items-center justify-center">
                {getFileIcon(material.fileType)}
              </div>
            )}
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold text-sm truncate">
                {material.title}
              </h3>
              <div className="flex items-center gap-2 mt-1">
                <Badge variant="outline" className="text-xs">
                  {getFileTypeCategory(material.fileType)}
                </Badge>
                <Badge
                  variant={material.isPublic ? "default" : "secondary"}
                  className="text-xs"
                >
                  {material.isPublic ? (
                    <Eye className="h-3 w-3" />
                  ) : (
                    <EyeOff className="h-3 w-3" />
                  )}
                </Badge>
              </div>
            </div>
          </div>
        </div>

        {material.description && (
          <p className="text-sm text-muted-foreground line-clamp-2">
            {material.description}
          </p>
        )}

        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>{new Date(material.createdAt).toLocaleDateString()}</span>
          <div className="flex gap-1">
            <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0"
              onClick={() => openEditModal(material)}
            >
              <Edit className="h-3 w-3" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0"
              onClick={() => handleDelete(material.id)}
            >
              <Trash2 className="h-3 w-3 text-red-500" />
            </Button>
          </div>
        </div>
      </div>
    </Card>
  );

  return (
    <div className="s">
      <div className="sticky top-15 z-10 bg-background ">
        <div className="flex items-center justify-between   ">
          <div  >
            <h1 className="text-xl md:text-3xl font-bold tracking-tight">
              Learning Materials
            </h1>
            <p className="text-muted-foreground hidden md:block">
              Manage educational content and resources
            </p>
          </div>
          <div></div>
          <Button onClick={() => setIsUploadModalOpen(true)}>
            <Plus className=" h-4 w-4" />
            <span className="hidden md:block"> Add Material</span>
          </Button>
        </div>

        <div className="flex items-center space-x-2 sticky   ">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search materials..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8"
            />
          </div>
        </div>
      </div>

      <Card className="border-none shadow-none">
        <CardHeader>
          <CardTitle>All Materials</CardTitle>
          <CardDescription>
            {filteredMaterials.length} material
            {filteredMaterials.length !== 1 ? "s" : ""} found
          </CardDescription>
        </CardHeader>
        <CardContent className="border-none">
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : filteredMaterials.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No materials found
            </div>
          ) : (
            <>
              {/* Desktop Table View */}
              <div className="hidden md:block">
                {selectedIds.length > 0 && (
                  <div className="flex items-center gap-2 mb-4 p-3 bg-muted rounded-lg">
                    <span className="text-sm font-medium">
                      {selectedIds.length} item(s) selected
                    </span>
                    <Button
                      variant="destructive"
                      size="sm"
                      onClick={handleBulkDelete}
                      disabled={isBulkDeleting}
                    >
                      {isBulkDeleting ? (
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      ) : (
                        <Trash2 className="mr-2 h-4 w-4" />
                      )}
                      Delete Selected
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setSelectedIds([])}
                    >
                      Cancel
                    </Button>
                  </div>
                )}
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-10">
                        <input
                          type="checkbox"
                          checked={selectedIds.length === filteredMaterials.length && filteredMaterials.length > 0}
                          onChange={toggleSelectAll}
                          className="rounded"
                        />
                      </TableHead>
                      <TableHead>Thumbnail</TableHead>
                      <TableHead>Title</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Description</TableHead>
                      <TableHead>Visibility</TableHead>
                      <TableHead>Created</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredMaterials.map((material) => (
                      <TableRow key={material.id}>
                        <TableCell>
                          <input
                            type="checkbox"
                            checked={selectedIds.includes(material.id)}
                            onChange={() => toggleSelect(material.id)}
                            className="rounded"
                          />
                        </TableCell>
                        <TableCell>
                          {material.thumbnailUrl ? (
                            <img
                              src={material.thumbnailUrl}
                              alt={material.title}
                              className="h-12 w-16 object-cover rounded border"
                            />
                          ) : (
                            <div className="h-12 w-16 bg-muted rounded border flex items-center justify-center">
                              {getFileIcon(material.fileType)}
                            </div>
                          )}
                        </TableCell>
                        <TableCell className="font-medium">
                          {material.title}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            {getFileIcon(material.fileType)}
                            <Badge variant="outline">
                              {getFileTypeCategory(material.fileType)}
                            </Badge>
                          </div>
                        </TableCell>
                        <TableCell className="max-w-xs truncate">
                          {material.description}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              material.isPublic ? "default" : "secondary"
                            }
                          >
                            {material.isPublic ? "Public" : "Private"}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {new Date(material.createdAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => openEditModal(material)}
                            >
                              <Edit className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDelete(material.id)}
                            >
                              <Trash2 className="h-4 w-4 text-red-500" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* Mobile Grid View */}
              <div className="grid grid-cols-1 gap-4 md:hidden">
                {filteredMaterials.map((material) => (
                  <MaterialCard key={material.id} material={material} />
                ))}
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Upload Modal */}
      <Dialog open={isUploadModalOpen} onOpenChange={setIsUploadModalOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Upload New Material</DialogTitle>
            <DialogDescription>
              Add a new learning material to your library. Supported formats:
              PDF, PowerPoint, Word, Videos, Audio, Images.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="title">Title *</Label>
              <Input
                id="title"
                placeholder="Enter material title"
                value={formData.title}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, title: e.target.value }))
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Enter material description"
                value={formData.description}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    description: e.target.value,
                  }))
                }
                rows={3}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="file">File *</Label>
              <Select
                value={formData.file?.toString() || ""}
                onValueChange={(value) =>
                  setFormData((prev) => ({ ...prev, file: value }))
                }
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select File" />
                </SelectTrigger>
                <SelectContent>
                  {files.map((file) => (
                    <SelectItem key={file.id} value={file.id.toString()}>
                      {file.name} - {(file.fileSize / 1024 / 1024).toFixed(2)}{" "}
                      MB
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center space-x-2">
              <Switch
                id="isPublic"
                checked={formData.isPublic}
                onCheckedChange={(checked) =>
                  setFormData((prev) => ({ ...prev, isPublic: checked }))
                }
              />
              <Label htmlFor="isPublic">Make this material public</Label>
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setIsUploadModalOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button onClick={handleUpload} disabled={isSubmitting}>
              {isSubmitting && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Upload Material
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Modal */}
      <Dialog open={isEditModalOpen} onOpenChange={setIsEditModalOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit Material</DialogTitle>
            <DialogDescription>
              Update the material information.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-6">
            {selectedMaterial && (
              <div className="flex items-center space-x-4 p-4 border rounded-lg bg-muted/50">
                {selectedMaterial.thumbnailUrl ? (
                  <img
                    src={selectedMaterial.thumbnailUrl}
                    alt={selectedMaterial.title}
                    className="h-16 w-20 object-cover rounded border"
                  />
                ) : (
                  <div className="h-16 w-20 bg-background rounded border flex items-center justify-center">
                    {getFileIcon(selectedMaterial.fileType)}
                  </div>
                )}
                <div className="flex-1">
                  <p className="font-medium">{selectedMaterial.title}</p>
                  <p className="text-sm text-muted-foreground">
                    {getFileTypeCategory(selectedMaterial.fileType)} •{" "}
                    {new Date(selectedMaterial.createdAt).toLocaleDateString()}
                  </p>
                </div>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="edit-title">Title *</Label>
              <Input
                id="edit-title"
                placeholder="Enter material title"
                value={formData.title}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, title: e.target.value }))
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit-description">Description</Label>
              <Textarea
                id="edit-description"
                placeholder="Enter material description"
                value={formData.description}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    description: e.target.value,
                  }))
                }
                rows={3}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit-file">File</Label>
              <Select
                value={formData.file?.toString() || ""}
                onValueChange={(value) =>
                  setFormData((prev) => ({ ...prev, file: value }))
                }
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select File" />
                </SelectTrigger>
                <SelectContent>
                  {files.map((file) => (
                    <SelectItem key={file.id} value={file.id.toString()}>
                      {file.name} - {(file.fileSize / 1024 / 1024).toFixed(2)}{" "}
                      MB
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center space-x-2">
              <Switch
                id="edit-isPublic"
                checked={formData.isPublic}
                onCheckedChange={(checked) =>
                  setFormData((prev) => ({ ...prev, isPublic: checked }))
                }
              />
              <Label htmlFor="edit-isPublic">Make this material public</Label>
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setIsEditModalOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button onClick={handleEdit} disabled={isSubmitting}>
              {isSubmitting && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Update Material
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
