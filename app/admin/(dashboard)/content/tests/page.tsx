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

import { Label } from "@/components/ui/label";

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
  FileQuestion,
  GripVertical,
  ArrowUpDown,
  Globe,
  List,
} from "lucide-react";
import { toast } from "sonner";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { TestTranslation, Language } from "@/lib/generated/prisma";
import { getLanguages } from "@/app/actions/getLanguages";
import { format } from "date-fns";
import Link from "next/link";

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
interface Test {
  id: number;
  testNumber: number;
  title: string;
  description: string;
  totalMarks: number;
  passMarks: number;
  duration: number;
  imageUrl: string;
  isFree: Boolean;
  testTranslations: TestTranslation[];
  _count: {
    testQuestions: number;
  };
  createdAt: string;
}

interface TestFormData {
  title: string;

  description: string;
  totalMarks: number;
  passMarks: number;
  duration: number;
  imageUrl?: string | null;
  isFree: Boolean;
}

export default function TestsPage() {
  const [tests, setTests] = useState<Test[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isTranslateDialogOpen, setIsTranslateDialogOpen] = useState(false);
  const [selectedLanguage, setSeletectedLanguage] = useState<number | null>(
    null
  );
  const [languages, setLanguages] = useState<Language[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingTest, setEditingTest] = useState<Test | null>(null);
  const [translatingTest, setTranslatingTest] = useState<Test | null>(null);
  const [files, setFiles] = useState<FileItem[]>([]);
  const [currentImageUrl, setCurrentImageUrl] = useState("");
  const [formData, setFormData] = useState<TestFormData>({
    title: "",

    description: "",
    totalMarks: 100,
    passMarks: 50,
    duration: 60,
    imageUrl: null,
    isFree: false,
  });

  const [dragItemIndex, setDragItemIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [isBulkDeleting, setIsBulkDeleting] = useState(false);

  const fetchTests = async () => {
    try {
      const data = await apiClient.get<{ data: Test[] }>("/api/tests");
      // Sort tests by testNumber for consistent display
      const sortedTests = (data.data || []).sort(
        (a, b) => a.testNumber - b.testNumber
      );
      setTests(sortedTests);
    } catch (error) {
      toast.error("Failed to fetch tests");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!formData.title.trim()) {
      toast.error("Title and test number are required");
      return;
    }

    if (formData.passMarks > formData.totalMarks) {
      toast.error("Pass marks cannot exceed total marks");
      return;
    }

    setIsSubmitting(true);

    try {
      if (editingTest) {
        // Optimistically update the UI for edit
        setTests((prev) =>
          prev.map((test) =>
            test.id === editingTest.id
              ? {
                  ...test,
                  ...formData,
                  imageUrl: formData.imageUrl || test.imageUrl,
                }
              : test
          )
        );

        await apiClient.put(`/api/tests/${editingTest.id}`, formData);
        toast.success("Test updated successfully");
      } else {
        const response = (await apiClient.post("/api/tests", formData)) as any;
        toast.success("Test created successfully");

        // Replace temporary test with actual data from server
        setTests((prev) => {
          return [...prev, response.data];
        });
      }

      setIsDialogOpen(false);
      resetForm();
    } catch (error) {
      // Revert optimistic updates on error
      if (editingTest) {
        fetchTests(); // Refetch to get correct data
      } else {
        setTests((prev) => prev.filter((test) => test.id !== Date.now()));
      }
      toast.error(`Failed to ${editingTest ? "update" : "create"} test`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const translateRow = async (id: number, languageId: number) => {
    setTests((prev) =>
      prev.map((test) =>
        test.id === id
          ? {
              ...test,
              title: test.testTranslations.find(
                (t) => t.languageId == languageId
              )?.title,
              description: test.testTranslations.find(
                (t) => t.languageId == languageId
              )?.description,
              imageUrl: test.testTranslations.find(
                (t) => t.languageId == languageId
              )?.imageUrl,
            }
          : test
      )
    );
  };

  const handleTranslateTest = async () => {
    if (!formData.title.trim()) {
      toast.error("Title and test number are required");
      return;
    }

    if (formData.passMarks > formData.totalMarks) {
      toast.error("Pass marks cannot exceed total marks");
      return;
    }

    setIsSubmitting(true);

    try {
      await apiClient.put(`/api/tests/${translatingTest?.id}/translate`, {
        ...formData,
        langId: selectedLanguage,
      });
      toast.success("Test Translated successfully");
      setTests((prev) =>
        prev.map((test) =>
          test.id === translatingTest?.id
            ? {
                ...test,
                ...formData,

                imageUrl: formData.imageUrl || test.imageUrl,
              }
            : test
        )
      );

      setIsTranslateDialogOpen(false);
      resetForm();
    } catch (error) {
      if (editingTest) {
        fetchTests();
      } else {
        setTests((prev) => prev.filter((test) => test.id !== Date.now()));
      }
      toast.error(`Failed to ${editingTest ? "update" : "create"} test`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (
      !confirm(
        "Are you sure you want to delete this test? This action cannot be undone."
      )
    )
      return;
    const testToDelete = tests.find((test) => test.id === id);

    try {
      // Optimistically update the UI

      setTests((prev) => prev.filter((test) => test.id !== id));

      await apiClient.delete(`/api/tests/${id}`);
      toast.success("Test deleted successfully");
    } catch (error) {
      // Revert on error
      if (testToDelete) {
        setTests((prev) => [...prev, testToDelete].sort((a, b) => a.id - b.id));
      }
      toast.error("Failed to delete test");
    }
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    if (
      !confirm(
        `Are you sure you want to delete ${selectedIds.length} test(s)? This action cannot be undone.`
      )
    )
      return;

    setIsBulkDeleting(true);
    try {
      setTests((prev) => prev.filter((t) => !selectedIds.includes(t.id)));
      setSelectedIds([]);

      await apiClient.post(`/api/tests/bulk-delete`, { ids: selectedIds });
      toast.success(`${selectedIds.length} test(s) deleted successfully`);
    } catch (error) {
      toast.error("Failed to delete tests");
      fetchTests();
    } finally {
      setIsBulkDeleting(false);
    }
  };

  const toggleSelectAll = () => {
    if (selectedIds.length === filteredTests.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(filteredTests.map((t) => t.id));
    }
  };

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  const handleEdit = (test: Test) => {
    setEditingTest(test);
    setFormData({
      title: test.title,
      description: test.description,
      totalMarks: test.totalMarks,
      passMarks: test.passMarks,
      duration: test.duration,
      isFree: test.isFree,
      imageUrl: test.imageUrl,
    });
    setCurrentImageUrl(test.imageUrl || "");
    setIsDialogOpen(true);
  };

  const handleTranslate = (test: Test) => {
    setTranslatingTest(test);
    setFormData({
      title: test.title,
      description: test.description,
      totalMarks: test.totalMarks,
      passMarks: test.passMarks,
      duration: test.duration,
      isFree: test.isFree,
      imageUrl: test.imageUrl,
    });
    setCurrentImageUrl(test.imageUrl || "");
    setIsTranslateDialogOpen(true);
  };

  const handleCreateNew = () => {
    setEditingTest(null);
    setFormData({
      title: "",
      description: "",

      totalMarks: 100,
      passMarks: 50,
      duration: 60,
      isFree: false,
      imageUrl: null,
    });
    setCurrentImageUrl("");
    setIsDialogOpen(true);
  };

  const resetForm = () => {
    setEditingTest(null);
    setFormData({
      title: "",
      description: "",
      totalMarks: 100,
      passMarks: 50,
      duration: 60,

      isFree: false,
      imageUrl: null,
    });
    setCurrentImageUrl("");
  };

  // Drag and Drop Handlers
  const handleDragStart = (e: React.DragEvent, index: number) => {
    setDragItemIndex(index);
    e.dataTransfer.effectAllowed = "move";
  };

  const handleDragOver = (e: React.DragEvent, index: number) => {
    e.preventDefault();
    setDragOverIndex(index);
  };

  const handleDragLeave = () => {
    setDragOverIndex(null);
  };

  const handleDrop = async (e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    setDragOverIndex(null);

    if (dragItemIndex === null || dragItemIndex === dropIndex) return;

    const draggedTest = tests[dragItemIndex];
    const newTests = [...tests];

    // Remove dragged item
    newTests.splice(dragItemIndex, 1);
    // Insert at new position
    newTests.splice(dropIndex, 0, draggedTest);

    // Update test numbers based on new order
    const updatedTests = newTests.map((test, index) => ({
      ...test,
      testNumber: index + 1,
    }));

    // Optimistically update the UI
    setTests(updatedTests);

    try {
      // Send batch update to server
      await apiClient.put("/api/tests/reorder", {
        testOrders: updatedTests.map((test) => ({
          id: test.id,
          testNumber: test.testNumber,
        })),
      });

      toast.success("Test order updated successfully");
    } catch (error) {
      // Revert on error
      fetchTests();
      toast.error("Failed to update test order");
    }

    setDragItemIndex(null);
  };

  const handleDragEnd = () => {
    setDragItemIndex(null);
    setDragOverIndex(null);
  };

  const filteredTests = tests.filter(
    (test) =>
      test.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      test.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const fetchFiles = async () => {
    try {
      const data = await apiClient.get<{ data: FileItem[] }>("/api/files");
      setFiles(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch files");
    }
  };
  const fetchLanguages = async () => {
    const fetchedLanguages = await getLanguages();
    if (fetchedLanguages.data) {
      setLanguages(fetchedLanguages.data);
      setSeletectedLanguage(fetchedLanguages.data[0].id);
    }
  };

  useEffect(() => {
    Promise.all([fetchTests(), fetchFiles(), fetchLanguages()]);
  }, []);

  // Mobile Grid Card Component
  const TestCard = ({ test, index }: { test: Test; index: number }) => (
    <Card
      className={`p-4 relative transition-all duration-200 ${
        dragOverIndex === index ? "border-2 border-blue-500 bg-blue-50" : ""
      } ${dragItemIndex === index ? "opacity-50" : ""}`}
      draggable={true}
      onDragStart={(e) => handleDragStart(e, index)}
      onDragOver={(e) => handleDragOver(e, index)}
      onDragLeave={handleDragLeave}
      onDrop={(e) => handleDrop(e, index)}
      onDragEnd={handleDragEnd}
    >
      <div className="absolute left-2 top-1/2 transform -translate-y-1/2 cursor-move">
        <GripVertical className="h-4 w-4 text-muted-foreground" />
      </div>

      <div className={`space-y-3 ${"ml-4"}`}>
        {test?.imageUrl && (
          <div className="flex justify-center">
            <img
              src={test.imageUrl}
              alt="Test thumbnail"
              className="h-32 w-32 rounded-md object-cover"
            />
          </div>
        )}

        <div className="space-y-2">
          <div className="flex items-center justify-between flex-wrap">
            <h3 className="font-semibold text-lg">{test.title}</h3>
            <Badge variant={test.isFree ? "default" : "secondary"}>
              {test.isFree && "Free"}
            </Badge>
          </div>

          <p className="text-sm text-muted-foreground line-clamp-2">
            {test.description}
          </p>

          <div className="grid grid-cols-2 gap-2 text-sm">
            <div className="flex items-center gap-1">
              <FileQuestion className="h-4 w-4" />
              <span>{test._count.testQuestions} Qs</span>
            </div>
            <div className="flex items-center gap-1">
              <span className="font-medium">#{test.testNumber}</span>
            </div>
            <div>{test.duration} min</div>
            <div>
              {test.passMarks}/{test.totalMarks}
            </div>
          </div>
          <div className="text-xs text-muted-foreground">
            Languages:{" "}
            <div>
              {test?.testTranslations?.map((translation) => (
                <Button variant="secondary" className={`cursor-pointer`}>
                  <img
                    src={`/${
                      languages.find((t) => t.id == translation.languageId)
                        ?.languageCode
                    }.png`}
                    alt=""
                    className="h-[32px] w-[32px] rounded-md"
                  />
                </Button>
              ))}
            </div>
          </div>
          <div className="text-xs text-muted-foreground">
            Created: {format(new Date(test.createdAt), "yyyy-MM-dd HH:mm:ss")}
          </div>
        </div>

        <div className="flex gap-2 pt-2">
          <Button
            variant="ghost"
            size="sm"
            className="flex-1"
            onClick={() => handleTranslate(test)}
          >
            <Globe className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="flex-1"
            onClick={() => handleEdit(test)}
          >
            <Edit className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="flex-1"
            onClick={() => handleDelete(test.id)}
          >
            <Trash2 className="h-4 w-4 text-red-500" />
          </Button>
        </div>
      </div>
    </Card>
  );

  return (
    <div className="space-y-6">
    <div className="flex flex-col sticky top-15 z-10 gap-4 bg-background">
        <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Tests</h1>
          <p className="text-muted-foreground hidden md:block">
            Manage driving theory tests and exams
          </p>
        </div>
        <div className="flex gap-2">
          <Link href="/admin/content/questions/manage">
            <Button variant="outline" size="sm" className="hidden sm:flex">
              <List className="mr-2 h-4 w-4" />
              Manage Tests Questions
            </Button>
          </Link>
          <Button onClick={handleCreateNew}>
            <Plus className="h-4 w-4" />
          <span className="hidden md:block">  Create Test</span>
          </Button>
        </div>
      </div>

      <div className="flex items-center space-x-2">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search tests..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8"
          />
        </div>
      </div>
    </div>

      <Card className="border-none shadow-none">
        <CardHeader>
          <CardTitle>All Tests</CardTitle>
          <CardDescription>
            {filteredTests.length} test{filteredTests.length !== 1 ? "s" : ""}{" "}
            found
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : filteredTests.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No tests found
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
                          checked={selectedIds.length === filteredTests.length && filteredTests.length > 0}
                          onChange={toggleSelectAll}
                          className="rounded"
                        />
                      </TableHead>
                      <TableHead className="w-10"></TableHead>
                      <TableHead>Image</TableHead>
                      <TableHead>Test Number</TableHead>
                      <TableHead>Title</TableHead>
                      <TableHead>Questions</TableHead>
                      <TableHead>Pass Marks</TableHead>
                      <TableHead>Total Marks</TableHead>
                      <TableHead>Duration</TableHead>
                      <TableHead>Languages</TableHead>
                      <TableHead>Is Free</TableHead>
                      <TableHead>Created</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredTests.map((test, index) => (
                      <TableRow
                        key={test.id}
                        className={`transition-all duration-200 ${
                          dragOverIndex === index
                            ? "bg-blue-50 border-2 border-blue-500"
                            : ""
                        } ${dragItemIndex === index ? "opacity-50" : ""}`}
                        draggable={true}
                        onDragStart={(e) => handleDragStart(e, index)}
                        onDragOver={(e) => handleDragOver(e, index)}
                        onDragLeave={handleDragLeave}
                        onDrop={(e) => handleDrop(e, index)}
                        onDragEnd={handleDragEnd}
                      >
                        <TableCell>
                          <input
                            type="checkbox"
                            checked={selectedIds.includes(test.id)}
                            onChange={() => toggleSelect(test.id)}
                            className="rounded"
                          />
                        </TableCell>
                        <TableCell className="cursor-move">
                          <GripVertical className="h-4 w-4 text-muted-foreground" />
                        </TableCell>

                        <TableCell>
                          {test?.imageUrl && (
                            <div className="flex justify-center items-center">
                              <img
                                src={test.imageUrl}
                                alt="Test thumbnail"
                                className="h-16 w-16 rounded-md object-cover"
                              />
                            </div>
                          )}
                        </TableCell>
                        <TableCell className="font-medium">
                          {test.testNumber}
                        </TableCell>
                        <TableCell className="font-medium">
                          {test.title}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <FileQuestion className="h-4 w-4" />
                            <span>{test._count.testQuestions}</span>
                          </div>
                        </TableCell>
                        <TableCell>{test.passMarks}</TableCell>
                        <TableCell>{test.totalMarks}</TableCell>
                        <TableCell>{test.duration} min</TableCell>
                        <TableCell>
                          <div>
                            {test?.testTranslations?.map((translation, idx) => (
                              <Badge
                                key={idx}
                                variant="secondary"
                                className={`cursor-pointer`}
                                onClick={() =>
                                  translateRow(test.id, translation.languageId)
                                }
                              >
                                <img
                                  src={`/${
                                    languages.find(
                                      (t) => t.id == translation.languageId
                                    )?.languageCode
                                  }.png`}
                                  alt=""
                                  className="h-[32px] w-[32px] rounded-md"
                                />
                              </Badge>
                            ))}
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={test.isFree ? "default" : "secondary"}
                          >
                            {test.isFree ? "Yes" : "No"}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {format(
                            new Date(test.createdAt),
                            "yyyy-MM-dd HH:mm:ss"
                          )}
                        </TableCell>

                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              className="flex-1"
                              onClick={() => handleTranslate(test)}
                            >
                              <Globe className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleEdit(test)}
                            >
                              <Edit className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDelete(test.id)}
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
                {filteredTests.map((test, index) => (
                  <TestCard key={test.id} test={test} index={index} />
                ))}
              </div>
            </>
          )}
        </CardContent>
      </Card>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingTest ? "Edit Test" : "Create New Test"}
            </DialogTitle>
            <DialogDescription>
              {editingTest
                ? "Update the test details below."
                : "Fill in the details to create a new test."}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="title">Title *</Label>
              <Input
                id="title"
                placeholder="Enter test title"
                value={formData.title}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, title: e.target.value }))
                }
              />
            </div>

            <div className="flex items-center space-x-2">
              <Switch
                id="isFree"
                checked={Boolean(formData?.isFree) || false}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, isFree: Boolean(checked) })
                }
              />
              <Label htmlFor="isFree">Is Free Test</Label>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Enter test description"
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
              {currentImageUrl && (
                <div className="flex justify-center items-center">
                  <img
                    src={currentImageUrl}
                    alt="Current test thumbnail"
                    className="h-32 w-32 rounded-md object-cover"
                  />
                </div>
              )}
              <Label htmlFor="file">Test Thumbnail</Label>
              <Select
                value={formData.imageUrl?.toString() || ""}
                onValueChange={(value) => {
                  setFormData((prev) => ({ ...prev, imageUrl: value }));
                  const selectedImage = files.find(
                    (f) => f.id === parseInt(value)
                  );
                  if (selectedImage) setCurrentImageUrl(selectedImage.filePath);
                  else {
                    setCurrentImageUrl("");
                  }
                }}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select File" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={"null"}>No image</SelectItem>
                  {files.map(
                    (file) =>
                      file.fileType.includes("image") && (
                        <SelectItem key={file.filePath} value={file.filePath}>
                          {" "}
                          {file.filePath !== null && (
                            <img
                              src={file.filePath}
                              alt="current question image"
                              className="h-[20px] w-[20px] rounded-md"
                            />
                          )}
                          <span>
                            {" "}
                            {file.name} -
                            {(file.fileSize / 1024 / 1024).toFixed(2)} MB
                          </span>
                        </SelectItem>
                      )
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label htmlFor="totalMarks">Total Marks *</Label>
                <Input
                  id="totalMarks"
                  type="number"
                  min="1"
                  max="1000"
                  value={formData.totalMarks}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      totalMarks: parseInt(e.target.value) || 0,
                    }))
                  }
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="passMarks">Pass Marks *</Label>
                <Input
                  id="passMarks"
                  type="number"
                  min="1"
                  max={formData.totalMarks}
                  value={formData.passMarks}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      passMarks: parseInt(e.target.value) || 0,
                    }))
                  }
                />
                <p className="text-xs text-muted-foreground">
                  Max: {formData.totalMarks}
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="duration">Duration (minutes) *</Label>
                <Input
                  id="duration"
                  type="number"
                  min="1"
                  max="480"
                  value={formData.duration}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      duration: parseInt(e.target.value) || 0,
                    }))
                  }
                />
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setIsDialogOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button onClick={handleCreate} disabled={isSubmitting}>
              {isSubmitting && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              {editingTest ? "Update Test" : "Create Test"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={isTranslateDialogOpen}
        onOpenChange={setIsTranslateDialogOpen}
      >
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Translate this test</DialogTitle>
            <DialogDescription>
              {editingTest
                ? "Update the test details below."
                : "Fill in the details to create a new test."}
            </DialogDescription>
            <div>
              {languages.map((language) => (
                <Button
                  variant={
                    language.id === selectedLanguage ? "outline" : "ghost"
                  }
                  className={`cursor-pointer`}
                  onClick={() => setSeletectedLanguage(language.id)}
                >
                  <img
                    src={`/${language.languageCode}.png`}
                    alt=""
                    className="h-[32px] w-[32px] rounded-md"
                  />
                </Button>
              ))}
            </div>
          </DialogHeader>

          <div className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="title">Title *</Label>
              <Input
                id="title"
                placeholder="Enter test title"
                value={formData.title}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, title: e.target.value }))
                }
              />
            </div>

            <div className="flex items-center space-x-2">
              <Switch
                id="isFree"
                checked={Boolean(formData?.isFree) || false}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, isFree: Boolean(checked) })
                }
              />
              <Label htmlFor="isFree">Is Free Test</Label>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Enter test description"
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
              {currentImageUrl && (
                <div className="flex justify-center items-center">
                  <img
                    src={currentImageUrl}
                    alt="Current test thumbnail"
                    className="h-32 w-32 rounded-md object-cover"
                  />
                </div>
              )}
              <Label htmlFor="file">Test Thumbnail</Label>
              <Select
                value={formData.imageUrl?.toString() || ""}
                onValueChange={(value) => {
                  setFormData((prev) => ({ ...prev, imageUrl: value }));
                  const selectedImage = files.find(
                    (f) => f.id === parseInt(value)
                  );
                  if (selectedImage) setCurrentImageUrl(selectedImage.filePath);
                  else {
                    setCurrentImageUrl("");
                  }
                }}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select File" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={"null"}>No image</SelectItem>
                  {files.map(
                    (file) =>
                      file.fileType.includes("image") && (
                        <SelectItem key={file.filePath} value={file.filePath}>
                          {file.name} -{" "}
                          {(file.fileSize / 1024 / 1024).toFixed(2)} MB
                        </SelectItem>
                      )
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* <div className="space-y-2">
                <Label htmlFor="totalMarks">Total Marks *</Label>
                <Input
                  id="totalMarks"
                  type="number"
                  min="1"
                  max="1000"
                  value={formData.totalMarks}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      totalMarks: parseInt(e.target.value) || 0,
                    }))
                  }
                />
              </div> */}
              {/* 
              <div className="space-y-2">
                <Label htmlFor="passMarks">Pass Marks *</Label>
                <Input
                  id="passMarks"
                  type="number"
                  min="1"
                  max={formData.totalMarks}
                  value={formData.passMarks}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      passMarks: parseInt(e.target.value) || 0,
                    }))
                  }
                />
                <p className="text-xs text-muted-foreground">
                  Max: {formData.totalMarks}
                </p>
              </div> */}

              {/* <div className="space-y-2">
                <Label htmlFor="duration">Duration (minutes) *</Label>
                <Input
                  id="duration"
                  type="number"
                  min="1"
                  max="480"
                  value={formData.duration}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      duration: parseInt(e.target.value) || 0,
                    }))
                  }
                />
              </div> */}
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setIsTranslateDialogOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button onClick={handleTranslateTest} disabled={isSubmitting}>
              <Globe className="h-4 w-4" />
              {isSubmitting && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Translate
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
