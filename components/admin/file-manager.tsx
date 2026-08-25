// components/file-manager.tsx
"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { Plus, Upload, Folder, File, Image, Search, Edit, Trash2, Loader2, X, FolderPlus } from "lucide-react";
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

interface FileManagerProps {
  onFileSelect?: (file: FileItem) => void;
  allowedFileTypes?: string[];
  multiple?: boolean;
  selectedFiles?: FileItem[];
}

export function FileManager({ 
  onFileSelect, 
  allowedFileTypes = [], 
  multiple = false,
  selectedFiles = [] 
}: FileManagerProps) {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedFolder, setSelectedFolder] = useState<number | null>(null);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isFolderModalOpen, setIsFolderModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [dragActive, setDragActive] = useState(false);

  const [uploadForm, setUploadForm] = useState({
    name: "",
    description: "",
    folderId: ""
  });

  const [folderForm, setFolderForm] = useState({
    name: "",
    parentId: ""
  });

  useEffect(() => {
    fetchFiles();
    fetchFolders();
  }, [selectedFolder]);

  const fetchFiles = async () => {
    try {
      const params = new URLSearchParams();
      if (selectedFolder) params.append('folderId', selectedFolder.toString());
      if (searchQuery) params.append('search', searchQuery);
      
      const data = await apiClient.get<{ data: FileItem[] }>(`/api/files?${params}`);
      setFiles(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch files");
    } finally {
      setIsLoading(false);
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
        parentId: folderForm.parentId ? parseInt(folderForm.parentId) : null
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

  const handleDeleteFile = async (fileId: number) => {
    if (!confirm("Are you sure you want to delete this file?")) return;

    try {
      await apiClient.delete(`/api/files/${fileId}`);
      toast.success("File deleted successfully");
      fetchFiles();
    } catch (error) {
      toast.error("Failed to delete file");
    }
  };

  const getFileIcon = (fileType: string) => {
    if (fileType.includes("image")) return <Image className="h-8 w-8" />;
    if (fileType.includes("pdf")) return <File className="h-8 w-8" />;
    if (fileType.includes("video")) return <File className="h-8 w-8" />;
    if (fileType.includes("audio")) return <File className="h-8 w-8" />;
    return <File className="h-8 w-8" />;
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const filteredFiles = files.filter(file => 
    allowedFileTypes.length === 0 || allowedFileTypes.includes(file.fileType)
  );

  const isSelected = (file: FileItem) => {
    return selectedFiles.some(f => f.id === file.id);
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h3 className="text-lg font-semibold">File Manager</h3>
          <p className="text-sm text-muted-foreground">
            Manage your files and folders
          </p>
        </div>
        <div className="flex gap-2 self-start sm:self-auto flex-wrap">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsFolderModalOpen(true)}
          >
            <FolderPlus className="h-4 w-4 mr-2" />
            New Folder
          </Button>
          <Button
            size="sm"
            onClick={() => setIsUploadModalOpen(true)}
          >
            <Upload className="h-4 w-4 mr-2" />
            Upload File
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col md:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search files..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8"
          />
        </div>
        <Select
          value={selectedFolder?.toString() || ""}
          onValueChange={(value) => setSelectedFolder(value ? parseInt(value) : null)}
        >
          <SelectTrigger className="w-full md:w-48">
            <SelectValue placeholder="All Folders" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">All Folders</SelectItem>
            {folders.map(folder => (
              <SelectItem key={folder.id} value={folder.id.toString()}>
                {folder.name} ({folder._count.files})
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Files Grid */}
      {isLoading ? (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-8 w-8 animate-spin" />
        </div>
      ) : filteredFiles.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Folder className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">No files found</p>
            <Button 
              variant="outline" 
              className="mt-4"
              onClick={() => setIsUploadModalOpen(true)}
            >
              <Upload className="h-4 w-4 mr-2" />
              Upload your first file
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {filteredFiles.map((file) => (
            <Card 
              key={file.id} 
              className={`cursor-pointer transition-all hover:shadow-md ${
                isSelected(file) ? 'ring-2 ring-primary' : ''
              }`}
              onClick={() => onFileSelect?.(file)}
            >
              <CardContent className="p-4">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <h4 className="font-medium text-sm line-clamp-1">{file.name}</h4>
                    <p className="text-xs text-muted-foreground mt-1">
                      {formatFileSize(file.fileSize)}
                    </p>
                  </div>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-6 w-6"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteFile(file.id);
                    }}
                  >
                    <Trash2 className="h-3 w-3 text-red-500" />
                  </Button>
                </div>

                <div className="aspect-video bg-muted rounded-lg flex items-center justify-center mb-3">
                  {file.thumbnailUrl ? (
                    <img
                      src={file.thumbnailUrl}
                      alt={file.name}
                      className="w-full h-full object-cover rounded-lg"
                    />
                  ) : (
                    getFileIcon(file.fileType)
                  )}
                </div>

                <div className="flex items-center justify-between">
                  <Badge variant="outline" className="text-xs">
                    {file.fileType.split('/')[1]?.toUpperCase() || 'FILE'}
                  </Badge>
                  {file.folder && (
                    <Badge variant="secondary" className="text-xs">
                      {file.folder.name}
                    </Badge>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

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

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="folderName">Folder Name</Label>
              <Input
                id="folderName"
                placeholder="Enter folder name"
                value={folderForm.name}
                onChange={(e) => setFolderForm(prev => ({ ...prev, name: e.target.value }))}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="parentFolder">Parent Folder (Optional)</Label>
              <Select
                value={folderForm.parentId || "null"}
                onValueChange={(value) => setFolderForm(prev => ({ ...prev, parentId: value }))}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select parent folder" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="null">No Parent</SelectItem>
                  {folders.map(folder => (
                    <SelectItem key={folder.id} value={folder.id.toString()}>
                      {folder.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsFolderModalOpen(false)}
            >
              Cancel
            </Button>
            <Button onClick={handleCreateFolder} disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create Folder
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// Separate Upload Form Component
function FileUploadForm({ 
  formData, 
  onFormChange, 
  folders, 
  onFileUpload, 
  isSubmitting, 
  dragActive, 
  onDragActiveChange 
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
        onFormChange((prev:any) => ({ ...prev, name: file.name.split('.')[0] }));
      }
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      if (!formData.name) {
        onFormChange((prev:any) => ({ ...prev, name: file.name.split('.')[0] }));
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
          onChange={(e) => onFormChange((prev:any) => ({ ...prev, name: e.target.value }))}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <Textarea
          id="description"
          placeholder="Enter file description"
          value={formData.description}
          onChange={(e) => onFormChange((prev:any) => ({ ...prev, description: e.target.value }))}
          rows={3}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="folder">Folder (Optional)</Label>
        <Select
          value={formData.folderId}
          onValueChange={(value) => onFormChange((prev:any) => ({ ...prev, folderId: value }))}
        >
          <SelectTrigger>
            <SelectValue placeholder="Select folder" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">No Folder</SelectItem>
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
            dragActive ? "border-primary bg-primary/5" : "border-muted-foreground/25"
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
                <X className="h-4 w-4 mr-1" />
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
        <Button
          variant="outline"
          onClick={() => window.history.back()}
        >
          Cancel
        </Button>
        <Button 
          onClick={() => selectedFile && onFileUpload(selectedFile)}
          disabled={!selectedFile || !formData.name || isSubmitting}
        >
          {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Upload File
        </Button>
      </DialogFooter>
    </div>
  );
}