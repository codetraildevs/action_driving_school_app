// app/file-manager/page.tsx
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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { apiClient } from "@/lib/api-client";
import {
  Plus,
  Upload,
  Folder,
  File,
  Image,
  Search,
  Edit,
  Trash2,
  Loader2,
  X,
  FolderPlus,
  Grid3X3,
  List,
  Download,
  HardDrive,
  UploadCloud,
} from "lucide-react";
import { toast } from "sonner";

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

interface Folder {
  id: number;
  name: string;
  path: string;
  parentId?: number;
  _count: {
    files: number;
  };
}

export default function FileManagerPage() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalFiles, setTotalFiles] = useState(0);
  const [imageCount, setImageCount] = useState(0);
  const [pdfCount, setPdfCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedFolder, setSelectedFolder] = useState<number | null>(null);
  const [viewMode, setViewMode] = useState<"grid" | "list">("grid");
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isFolderModalOpen, setIsFolderModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<FileItem | null>(null);

  const [uploadForm, setUploadForm] = useState({
    name: "New File",
    description: "",
    folderId: "",
  });

  const [folderForm, setFolderForm] = useState({
    name: "New Folder",
    parentId: "",
  });

  const [editForm, setEditForm] = useState({
    name: "",
    description: "",
    folderId: "",
  });

  useEffect(() => {
    fetchFiles();
    fetchFolders();
  }, [selectedFolder, searchQuery]);

  // Paginated: loads 50 files at a time with "Load more" (previously every
  // file was returned at once).
  const fetchFiles = async (targetPage = 1, append = false) => {
    try {
      if (append) setIsLoadingMore(true);
      else setIsLoading(true);
      const params = new URLSearchParams({
        page: String(targetPage),
        pageSize: "50",
      });
      if (selectedFolder) params.append("folderId", selectedFolder.toString());
      if (searchQuery) params.append("search", searchQuery);

      const data = await apiClient.get<{
        data: FileItem[];
        total?: number;
        counts?: { images: number; pdfs: number };
        totalPages?: number;
      }>(`/api/files?${params}`);
      const list = data.data || [];
      setFiles((prev) => (append ? [...prev, ...list] : list));
      setTotalFiles(data.total ?? list.length);
      if (data.counts) {
        setImageCount(data.counts.images);
        setPdfCount(data.counts.pdfs);
      }
      setTotalPages(data.totalPages ?? 1);
      setPage(targetPage);
    } catch (error) {
      toast.error("Failed to fetch files");
    } finally {
      if (append) setIsLoadingMore(false);
      else setIsLoading(false);
    }
  };

  const fetchFolders = async () => {
    try {
      const data = await apiClient.get<{ data: Folder[] }>("/api/folders");
      setFolders(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch folders");
    }
  };

  const handleUpload = async (file: File) => {
    if (!uploadForm.name.trim()) {
      toast.error("File name is required");
      return;
    }

    setIsSubmitting(true);

    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("name", uploadForm.name);
      formData.append("description", uploadForm.description);
      if (uploadForm.folderId) {
        formData.append("folderId", uploadForm.folderId);
      }

      await apiClient.uploadFile("/api/files", formData);

      toast.success("File uploaded successfully");
      setIsUploadModalOpen(false);
      setUploadForm({ name: "", description: "", folderId: "" });
      fetchFiles();
    } catch (error) {
      toast.error("Failed to upload file");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateFolder = async () => {
    if (!folderForm.name.trim()) {
      toast.error("Folder name is required");
      return;
    }

    setIsSubmitting(true);

    try {
      await apiClient.post("/api/folders", {
        name: folderForm.name,
        parentId: folderForm.parentId ? parseInt(folderForm.parentId) : null,
      });

      toast.success("Folder created successfully");
      setIsFolderModalOpen(false);
      setFolderForm({ name: "", parentId: "" });
      fetchFolders();
    } catch (error) {
      toast.error("Failed to create folder");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEditFile = async () => {
    if (!selectedFile || !editForm.name.trim()) {
      toast.error("File name is required");
      return;
    }

    setIsSubmitting(true);

    try {
      await apiClient.put(`/api/files/${selectedFile.id}`, {
        name: editForm.name,
        description: editForm.description,
        folderId: editForm.folderId ? parseInt(editForm.folderId) : null,
      });

      toast.success("File updated successfully");
      setIsEditModalOpen(false);
      setSelectedFile(null);
      fetchFiles();
    } catch (error) {
      toast.error("Failed to update file");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteFile = async (fileId: number) => {
    if (
      !confirm(
        "Are you sure you want to delete this file? This action cannot be undone."
      )
    )
      return;

    try {
      await apiClient.delete(`/api/files/${fileId}`);
      toast.success("File deleted successfully");
      fetchFiles();
    } catch (error) {
      toast.error("Failed to delete file");
    }
  };

  const handleDeleteFolder = async (folderId: number) => {
    if (
      !confirm(
        "Are you sure you want to delete this folder? All files in this folder will be moved to root."
      )
    )
      return;

    try {
      // First, move all files to root
      await apiClient.put(`/api/folders/${folderId}/move-files`);

      // Then delete the folder
      await apiClient.delete(`/api/folders/${folderId}`);

      toast.success("Folder deleted successfully");
      fetchFolders();
      fetchFiles();
    } catch (error) {
      toast.error("Failed to delete folder");
    }
  };

  const openEditModal = (file: FileItem) => {
    setSelectedFile(file);
    setEditForm({
      name: file.name,
      description: file.description,
      folderId: file.folderId?.toString() || "",
    });
    setIsEditModalOpen(true);
  };

  const getFileIcon = (fileType: string) => {
    if (fileType.includes("image"))
      return <Image className="h-8 w-8 text-blue-500" />;
    if (fileType.includes("pdf"))
      return <File className="h-8 w-8 text-red-500" />;
    if (fileType.includes("video"))
      return <File className="h-8 w-8 text-purple-500" />;
    if (fileType.includes("audio"))
      return <File className="h-8 w-8 text-green-500" />;
    if (fileType.includes("word") || fileType.includes("document"))
      return <File className="h-8 w-8 text-blue-600" />;
    if (fileType.includes("powerpoint") || fileType.includes("presentation"))
      return <File className="h-8 w-8 text-orange-500" />;
    return <File className="h-8 w-8 text-gray-500" />;
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  const getFileType = (fileType: string) => {
    if (fileType.includes("image")) return "Image";
    if (fileType.includes("pdf")) return "PDF";
    if (fileType.includes("video")) return "Video";
    if (fileType.includes("audio")) return "Audio";
    if (fileType.includes("word") || fileType.includes("document"))
      return "Document";
    if (fileType.includes("powerpoint") || fileType.includes("presentation"))
      return "Presentation";
    return "File";
  };

  const handleDownload = (file: FileItem) => {
    // Create a temporary link to trigger download
    const link = document.createElement("a");
    link.href = file.filePath;
    link.download = file.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between sticky top-15 z-20 bg-background ">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">File Manager</h1>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => setIsFolderModalOpen(true)}
              className="text-center"
            >
              <FolderPlus className="h-4 w-4" />
              <span className="hidden md:block"> New Folder</span>
            </Button>
            <Button
              onClick={() => setIsUploadModalOpen(true)}
              className="text-center"
              variant={"default"}
            >
              <UploadCloud className="h-4 w-4" />
              <span className="hidden md:block">Upload File</span>
            </Button>
          </div>
        </div>

        {/* Stats */}
        <div className=" hidden md:grid grid-cols-2 md:grid-cols-4 md:gap-4 gap-2">
          <Card>
            <CardContent className="p-2 md:p-6">
              <div className="flex items-center md:gap-4 gap-2">
                <div className="p-2 bg-blue-100 rounded-lg">
                  <HardDrive className="h-6 w-6 text-blue-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{totalFiles}</p>
                  <p className="text-sm text-muted-foreground">Total Files</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-2 md:p-6">
              <div className="flex items-center md:gap-4 gap-2">
                <div className="p-2 bg-green-100 rounded-lg">
                  <Folder className="h-6 w-6 text-green-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{folders.length}</p>
                  <p className="text-sm text-muted-foreground">Folders</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-2 md:p-6">
              <div className="flex items-center md:gap-4 gap-2">
                <div className="p-2 bg-purple-100 rounded-lg">
                  <Image className="h-6 w-6 text-purple-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{imageCount}</p>
                  <p className="text-sm text-muted-foreground">Images</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="border-none">
            <CardContent className="p-2 md:p-6">
              <div className="flex items-center md:gap-4 gap-2">
                <div className="p-2 bg-orange-100 rounded-lg">
                  <File className="h-6 w-6 text-orange-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{pdfCount}</p>
                  <p className="text-sm text-muted-foreground">PDFs</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Filters and Controls */}

        <div className="flex sm:flex-row gap-4 items-start sm:items-center justify-end sticky top-25 bg-background">
          <div className="flex flex-col sm:flex-row gap-4 flex-1 w-full">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search files..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8"
              />
            </div>
          </div>
          <div className="flex gap-2">
            <div className="hidden md:block">
              {" "}
              <Button
                variant={viewMode === "grid" ? "default" : "outline"}
                size="icon"
                onClick={() => setViewMode("grid")}
              >
                <Grid3X3 className="h-4 w-4" />
              </Button>
              <Button
                variant={viewMode === "list" ? "default" : "outline"}
                size="icon"
                onClick={() => setViewMode("list")}
              >
                <List className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <Tabs defaultValue="files" className="space-y-6">
        <TabsList className="sticky top-35 bg-background  w-full grid grid-cols-3 gap-2 z-20 h-fit">
          <TabsTrigger value="files">Files ({totalFiles})</TabsTrigger>
          <TabsTrigger value="folders">Folders ({folders.length})</TabsTrigger>
          <Select
            value={selectedFolder?.toString() || ""}
            onValueChange={(value) =>
              setSelectedFolder(value ? parseInt(value) : null)
            }
          >
            <SelectTrigger className="w-full sm:w-48">
              <SelectValue placeholder="All Folders" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="null">All Folders</SelectItem>
              {folders.map((folder) => (
                <SelectItem key={folder.id} value={folder.id.toString()}>
                  {folder.name} ({folder._count.files})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </TabsList>

        <TabsContent value="files" className="space-y-4">
          {isLoading ? (
            <Card>
              <CardContent className="flex items-center justify-center py-12">
                <Loader2 className="h-8 w-8 animate-spin" />
              </CardContent>
            </Card>
          ) : files.length === 0 ? (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-12">
                <Folder className="h-12 w-12 text-muted-foreground mb-4" />
                <p className="text-muted-foreground">No files found</p>
                <Button
                  variant="outline"
                  className="mt-4"
                  onClick={() => setIsUploadModalOpen(true)}
                >
                  <Upload className="h-4 w-4" />
                  Upload your first file
                </Button>
              </CardContent>
            </Card>
          ) : viewMode === "grid" ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 max-h-[70vh] overflow-auto">
              {files.map((file) => (
                <Card key={file.id} className="overflow-hidden">
                  <CardContent className="p-0">
                    <div className="aspect-video bg-muted relative group">
                      {file.fileType.includes("image") ? (
                        <img
                          src={file.filePath}
                          alt={file.name}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          {getFileIcon(file.fileType)}
                        </div>
                      )}
                      <div className="absolute inset-0 bg-black/50 md:opacity-0 opacity-80 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleDownload(file)}
                        >
                          <Download className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => openEditModal(file)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleDeleteFile(file.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                    <div className="p-4">
                      <h4 className="font-medium text-sm line-clamp-1 mb-1">
                        {file.name}
                      </h4>
                      <div className="flex items-center justify-between text-xs text-muted-foreground">
                        <span>{formatFileSize(file.fileSize)}</span>
                        <Badge variant="outline">
                          {getFileType(file.fileType)}
                        </Badge>
                      </div>
                      {file.folder && (
                        <div className="flex items-center gap-1 mt-2 text-xs text-muted-foreground">
                          <Folder className="h-3 w-3" />
                          {file.folder.name}
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle>All Files</CardTitle>
                <CardDescription>
                  {totalFiles} file{totalFiles !== 1 ? "s" : ""} found
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Name</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Size</TableHead>
                      <TableHead>Folder</TableHead>
                      <TableHead>Uploaded</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {files.map((file) => (
                      <TableRow key={file.id}>
                        <TableCell>
                          <div className="flex items-center gap-3">
                            <div className="flex-shrink-0">
                              {getFileIcon(file.fileType)}
                            </div>
                            <div>
                              <p className="font-medium">{file.name}</p>
                              <p className="text-sm text-muted-foreground line-clamp-1">
                                {file.description}
                              </p>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">
                            {getFileType(file.fileType)}
                          </Badge>
                        </TableCell>
                        <TableCell>{formatFileSize(file.fileSize)}</TableCell>
                        <TableCell>
                          {file.folder ? (
                            <Badge variant="secondary">
                              {file.folder.name}
                            </Badge>
                          ) : (
                            <span className="text-muted-foreground">-</span>
                          )}
                        </TableCell>
                        <TableCell>
                          {new Date(file.createdAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDownload(file)}
                            >
                              <Download className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => openEditModal(file)}
                            >
                              <Edit className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDeleteFile(file.id)}
                            >
                              <Trash2 className="h-4 w-4 text-red-500" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          )}

          {/* Load more */}
          {page < totalPages && (
            <div className="flex justify-center py-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchFiles(page + 1, true)}
                disabled={isLoadingMore}
              >
                {isLoadingMore ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : null}
                Load more
              </Button>
            </div>
          )}
        </TabsContent>

        <TabsContent value="folders">
          <Card>
            <CardHeader>
              <CardTitle>Folders</CardTitle>
              <CardDescription>
                Manage your folders and organize your files
              </CardDescription>
            </CardHeader>
            <CardContent>
              {folders.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  No folders created yet
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Name</TableHead>
                      <TableHead>Path</TableHead>
                      <TableHead>Files</TableHead>
                      <TableHead>Created</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {folders.map((folder) => (
                      <TableRow key={folder.id}>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <Folder className="h-4 w-4 text-blue-500" />
                            <span className="font-medium">{folder.name}</span>
                          </div>
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          /{folder.path}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">
                            {folder._count.files} files
                          </Badge>
                        </TableCell>
                        <TableCell>{new Date().toLocaleDateString()}</TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeleteFolder(folder.id)}
                          >
                            <Trash2 className="h-4 w-4 text-red-500" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Upload Modal */}
      <Dialog open={isUploadModalOpen} onOpenChange={setIsUploadModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Upload File</DialogTitle>
            <DialogDescription>
              Upload a new file to your library
            </DialogDescription>
          </DialogHeader>
          <FileUploadForm
            formData={uploadForm}
            onFormChange={setUploadForm}
            folders={folders}
            onFileUpload={handleUpload}
            isSubmitting={isSubmitting}
            dragActive={dragActive}
            onDragActiveChange={setDragActive}
          />
        </DialogContent>
      </Dialog>

      {/* Create Folder Modal */}
      <Dialog open={isFolderModalOpen} onOpenChange={setIsFolderModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Create Folder</DialogTitle>
            <DialogDescription>
              Create a new folder to organize your files
            </DialogDescription>
          </DialogHeader>
          <FolderForm
            formData={folderForm}
            onFormChange={setFolderForm}
            folders={folders}
            onSubmit={handleCreateFolder}
            isSubmitting={isSubmitting}
          />
        </DialogContent>
      </Dialog>

      {/* Edit File Modal */}
      <Dialog open={isEditModalOpen} onOpenChange={setIsEditModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Edit File</DialogTitle>
            <DialogDescription>Update file information</DialogDescription>
          </DialogHeader>
          <EditFileForm
            formData={editForm}
            onFormChange={setEditForm}
            folders={folders}
            onSubmit={handleEditFile}
            isSubmitting={isSubmitting}
            selectedFile={selectedFile}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}

// File Upload Form Component
function FileUploadForm({
  formData,
  onFormChange,
  folders,
  onFileUpload,
  isSubmitting,
  dragActive,
  onDragActiveChange,
}: any) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      onDragActiveChange(true);
    } else if (e.type === "dragleave") {
      onDragActiveChange(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onDragActiveChange(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      setSelectedFile(file);
      if (!formData.name) {
        onFormChange((prev: any) => ({
          ...prev,
          name: file.name.split(".")[0],
        }));
      }
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      if (!formData.name) {
        onFormChange((prev: any) => ({
          ...prev,
          name: file.name.split(".")[0],
        }));
      }
    }
  };

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="name">File Name *</Label>
        <Input
          id="name"
          placeholder="Enter file name"
          value={formData.name}
          onChange={(e) =>
            onFormChange((prev: any) => ({ ...prev, name: e.target.value }))
          }
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <Textarea
          id="description"
          placeholder="Enter file description"
          value={formData.description}
          onChange={(e) =>
            onFormChange((prev: any) => ({
              ...prev,
              description: e.target.value,
            }))
          }
          rows={3}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="folder">Folder (Optional)</Label>
        <Select
          value={formData.folderId}
          onValueChange={(value) =>
            onFormChange((prev: any) => ({ ...prev, folderId: value }))
          }
        >
          <SelectTrigger>
            <SelectValue placeholder="Select folder" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="null">No Folder</SelectItem>
            {folders.map((folder: Folder) => (
              <SelectItem key={folder.id} value={folder.id.toString()}>
                {folder.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label>File *</Label>
        <div
          className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-colors ${
            dragActive
              ? "border-primary bg-primary/5"
              : "border-muted-foreground/25"
          }`}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
          onClick={() => document.getElementById("file-upload")?.click()}
        >
          <input
            id="file-upload"
            type="file"
            className="hidden"
            onChange={handleFileChange}
          />

          {selectedFile ? (
            <div className="space-y-2">
              <File className="h-8 w-8 mx-auto text-primary" />
              <p className="font-medium">{selectedFile.name}</p>
              <p className="text-sm text-muted-foreground">
                {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB
              </p>
              <Button
                variant="outline"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  setSelectedFile(null);
                }}
              >
                <X className="h-4 w-4" />
                Change File
              </Button>
            </div>
          ) : (
            <div className="space-y-2">
              <Upload className="h-8 w-8 mx-auto text-muted-foreground" />
              <div>
                <p className="font-medium">Click to upload or drag and drop</p>
                <p className="text-sm text-muted-foreground">
                  PDF, DOC, PPT, Images, Videos, Audio (Max 50MB)
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      <DialogFooter>
        <Button variant="outline" onClick={() => window.history.back()}>
          Cancel
        </Button>
        <Button
          onClick={() => selectedFile && onFileUpload(selectedFile)}
          disabled={!selectedFile || !formData.name || isSubmitting}
        >
          {isSubmitting && <Loader2 className=" h-4 w-4 animate-spin" />}
          Upload File
        </Button>
      </DialogFooter>
    </div>
  );
}

// Folder Form Component
function FolderForm({
  formData,
  onFormChange,
  folders,
  onSubmit,
  isSubmitting,
}: any) {
  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="folderName">Folder Name *</Label>
        <Input
          id="folderName"
          placeholder="Enter folder name"
          value={formData.name}
          onChange={(e) =>
            onFormChange((prev: any) => ({ ...prev, name: e.target.value }))
          }
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="parentFolder">Parent Folder (Optional)</Label>
        <Select
          value={formData.parentId}
          onValueChange={(value) =>
            onFormChange((prev: any) => ({ ...prev, parentId: value }))
          }
        >
          <SelectTrigger>
            <SelectValue placeholder="Select parent folder" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="null">No Parent</SelectItem>
            {folders.map((folder: Folder) => (
              <SelectItem key={folder.id} value={folder.id.toString()}>
                {folder.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <DialogFooter>
        <Button variant="outline" onClick={() => window.history.back()}>
          Cancel
        </Button>
        <Button onClick={onSubmit} disabled={!formData.name || isSubmitting}>
          {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
          Create Folder
        </Button>
      </DialogFooter>
    </div>
  );
}

// Edit File Form Component
function EditFileForm({
  formData,
  onFormChange,
  folders,
  onSubmit,
  isSubmitting,
  selectedFile,
}: any) {
  return (
    <div className="space-y-4">
      {selectedFile && (
        <div className="flex items-center gap-3 p-3 border rounded-lg bg-muted/50">
          {selectedFile.thumbnailUrl ? (
            <img
              src={selectedFile.thumbnailUrl}
              alt={selectedFile.name}
              className="h-12 w-16 object-cover rounded"
            />
          ) : (
            <File className="h-12 w-12 text-muted-foreground" />
          )}
          <div className="flex-1">
            <p className="font-medium">{selectedFile.name}</p>
            <p className="text-sm text-muted-foreground">
              {formatFileSize(selectedFile.fileSize)} • {selectedFile.fileType}
            </p>
          </div>
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="edit-name">File Name *</Label>
        <Input
          id="edit-name"
          placeholder="Enter file name"
          value={formData.name}
          onChange={(e) =>
            onFormChange((prev: any) => ({ ...prev, name: e.target.value }))
          }
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="edit-description">Description</Label>
        <Textarea
          id="edit-description"
          placeholder="Enter file description"
          value={formData.description}
          onChange={(e) =>
            onFormChange((prev: any) => ({
              ...prev,
              description: e.target.value,
            }))
          }
          rows={3}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="edit-folder">Folder</Label>
        <Select
          value={formData.folderId}
          onValueChange={(value) =>
            onFormChange((prev: any) => ({ ...prev, folderId: value }))
          }
        >
          <SelectTrigger>
            <SelectValue placeholder="Select folder" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="null">No Folder</SelectItem>
            {folders.map((folder: Folder) => (
              <SelectItem key={folder.id} value={folder.id.toString()}>
                {folder.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <DialogFooter>
        <Button variant="outline" onClick={() => window.history.back()}>
          Cancel
        </Button>
        <Button onClick={onSubmit} disabled={!formData.name || isSubmitting}>
          {isSubmitting && <Loader2 className=" h-4 w-4 animate-spin" />}
          Update File
        </Button>
      </DialogFooter>
    </div>
  );
}

// Helper function
function formatFileSize(bytes: number) {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
}
