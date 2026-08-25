// app/questions/create/page.tsx
"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
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
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { Plus, X, Loader2, ArrowLeft } from "lucide-react";
import { toast } from "sonner";
import Link from "next/link";

interface Test {
  id: number;
  title: string;
}

interface QuestionOption {
  text: string;
  isCorrect: boolean;
  imageUrl: string | null;
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
export default function CreateQuestionPage() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [tests, setTests] = useState<Test[]>([]);
  const [selectedTests, setSelectedTests] = useState<number[]>([]);

  const [formData, setFormData] = useState({
    questionText: "",
    questionType: "mcq",
    file: "",
  });

  const [options, setOptions] = useState<QuestionOption[]>([
    { text: "", isCorrect: false, imageUrl: null },
    { text: "", isCorrect: false, imageUrl: null },
    { text: "", isCorrect: false, imageUrl: null },
    { text: "", isCorrect: false, imageUrl: null },
  ]);

  const [searchQuery, setSearchQuery] = useState("");
  const [files, setFiles] = useState<FileItem[]>([]);
  const [currentImageUrl, setCurrentImageUrl] = useState("");
  const fetchFiles = async () => {
    try {
      const params = new URLSearchParams();

      if (searchQuery) params.append("search", searchQuery);

      const data = await apiClient.get<{ data: FileItem[] }>(
        `/api/files?${params}`
      );
      setFiles(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch files");
    } finally {
      setIsLoading(false);
    }
  };
  useEffect(() => {
    fetchFiles();
    fetchTests();
  }, []);

  const fetchTests = async () => {
    try {
      const data = await apiClient.get<{ data: Test[] }>("/api/tests");
      setTests(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch tests");
    }
  };

  const handleOptionChange = (
    index: number,
    field: keyof QuestionOption,
    value: string | boolean
  ) => {
    let formattedValue = value;
    let isCorrect = false;
    const valuePortion = value.toString().trim().slice(0, 3).trim();
    if (valuePortion.startsWith("(") && valuePortion.endsWith(")")) {
      formattedValue = value.toString().slice(1);
      isCorrect = true;
    }
    const newOptions = [...options];
    newOptions[index] = {
      ...newOptions[index],
      [field]: formattedValue,
      isCorrect,
    };
    setOptions(newOptions);
  };

  const addOption = () => {
    setOptions([...options, { text: "", isCorrect: false, imageUrl: null }]);
  };

  const removeOption = (index: number) => {
    if (options.length > 2) {
      const newOptions = options.filter((_, i) => i !== index);
      setOptions(newOptions);
    } else {
      toast.error("Questions must have at least 2 options");
    }
  };

  const handleCorrectOptionChange = (index: number) => {
    const newOptions = options.map((opt, i) => ({
      ...opt,
      isCorrect: i === index,
    }));
    setOptions(newOptions);
  };

  const handleOptionImageChange = (index: number, imageUrl: string) => {
    const cleanedUrl = imageUrl.includes("null") ? null : imageUrl;
    const newOptions = options.map((opt, i) => ({
      ...opt,
      imageUrl: i === index ? cleanedUrl : opt.imageUrl,
    }));
    setOptions(newOptions);
  };
  const resetForm = () => {
    setFormData({
      questionText: "",
      questionType: "mcq",
      file: "",
    });
    setOptions([
      { text: "", isCorrect: false, imageUrl: null },
      { text: "", isCorrect: false, imageUrl: null },
      { text: "", isCorrect: false, imageUrl: null },
      { text: "", isCorrect: false, imageUrl: null },
    ]);
  };

  const handleSubmit = async () => {
    if (!formData.questionText.trim()) {
      toast.error("Question text is required");
      return;
    }

    if (options.some((opt) => !opt.text.trim())) {
      toast.error("All options must have text");
      return;
    }

    if (!options.some((opt) => opt.isCorrect)) {
      toast.error("At least one correct option is required");
      return;
    }

    if (formData.questionType === "true_false" && options.length !== 2) {
      toast.error("True/False questions must have exactly 2 options");
      return;
    }

    setIsLoading(true);

    try {
      await apiClient.post("/api/questions", {
        file: formData.file,
        questionText: formData.questionText,
        questionType: formData.questionType,
        options: options,
        testIds: selectedTests,
      });

      toast.success("Question created successfully");
      resetForm();
      // router.push("/admin/content/questions/");
    } catch (error) {
      toast.error("Failed to create question");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div className="inline-flex">
          {" "}
          <Link href="/admin/content/questions/">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              Create Question
            </h1>
            <p className="text-muted-foreground">
              Add a new question to the question bank
            </p>
          </div>
        </div>
        <div className="flex justify-end gap-4">
          <Link href="/admin/content/questions">
            <Button variant="outline">Cancel</Button>
          </Link>
          <Button onClick={handleSubmit} disabled={isLoading}>
            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Create Question
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Question Details</CardTitle>
            <CardDescription>
              Enter the question and its options
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="questionType">Question Type</Label>
              <Select
                value={formData.questionType}
                onValueChange={(value) =>
                  setFormData((prev) => ({ ...prev, questionType: value }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select question type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="mcq">Multiple Choice (MCQ)</SelectItem>
                  {/* <SelectItem value="true_false">True/False</SelectItem> */}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="questionText">Question Text *</Label>
              <Textarea
                id="questionText"
                placeholder="Enter your question here..."
                value={formData.questionText}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    questionText: e.target.value,
                  }))
                }
                rows={4}
              />
            </div>
            <div className="space-y-6">
              {currentImageUrl.length > 0 && (
                <div className="flex justify-center items-center">
                  {" "}
                  <img
                    src={currentImageUrl}
                    alt="current question image"
                    className="h-[250px] w-[250px] rounded-md"
                  />
                </div>
              )}
              <Label htmlFor="file">Question Image *</Label>
              <Select
                value={formData.file?.toString() || ""}
                onValueChange={(value) => {
                  setFormData((prev) => ({ ...prev, file: value }));
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
                        <SelectItem
                          key={file.id}
                          value={file.id.toString()}
                          className="flex justify-betweeen items-center"
                        >
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

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Label>Options *</Label>
                {formData.questionType === "mcq" && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={addOption}
                  >
                    <Plus className="h-4 w-4 mr-1" />
                    Add Option
                  </Button>
                )}
              </div>

              <div className="space-y-3">
                {options.map((option, index) => (
                  <div key={index} className="flex items-center gap-3">
                    {option?.imageUrl !== null && (
                      <div className="flex justify-center items-center">
                        {" "}
                        <img
                          src={option.imageUrl}
                          alt="current question image"
                          className="h-[150px] w-[150px] rounded-md"
                        />
                      </div>
                    )}
                    <div className="flex-1">
                      <Input
                        placeholder={`Option ${index + 1}`}
                        value={option.text}
                        onChange={(e) =>
                          handleOptionChange(index, "text", e.target.value)
                        }
                      />
                    </div>
                    <div className="flex items-center gap-2">
                      <Switch
                        id={option.text + (index + 2)}
                        checked={option.isCorrect}
                        onCheckedChange={() => handleCorrectOptionChange(index)}
                      />
                      <Label
                        className="text-sm"
                        htmlFor={option.text + (index + 2)}
                      >
                        Correct
                      </Label>
                    </div>
                    <div className="space-y-6">
                      <Label htmlFor="file">Image *</Label>
                      <Select
                        value={option.imageUrl || ""}
                        onValueChange={(value) =>
                          handleOptionImageChange(index, value)
                        }
                      >
                        <SelectTrigger className="w-full">
                          <SelectValue placeholder="Select File" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value={"null"}>No image</SelectItem>
                          {files.map(
                            (file) =>
                              file.fileType.includes("image") && (
                                <SelectItem
                                  key={file.id}
                                  value={file.filePath}
                                  className="flex justify-betweeen items-center"
                                >
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
                                    {(file.fileSize / 1024 / 1024).toFixed(2)}{" "}
                                    MB
                                  </span>
                                </SelectItem>
                              )
                          )}
                        </SelectContent>
                      </Select>
                    </div>
                    {options.length > 2 && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() => removeOption(index)}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Test Assignment</CardTitle>
            <CardDescription>
              Assign this question to tests (optional)
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label>Select Tests</Label>
              <div className="space-y-2 max-h-60 overflow-y-auto">
                {tests.map((test) => (
                  <div key={test.id} className="flex items-center space-x-2">
                    <Switch
                      checked={selectedTests.includes(test.id)}
                      onCheckedChange={(checked) => {
                        if (checked) {
                          setSelectedTests((prev) => [...prev, test.id]);
                        } else {
                          setSelectedTests((prev) =>
                            prev.filter((id) => id !== test.id)
                          );
                        }
                      }}
                    />
                    <Label className="text-sm font-normal">{test.title}</Label>
                  </div>
                ))}
              </div>
            </div>

            {selectedTests.length > 0 && (
              <div className="space-y-2">
                <Label>Selected Tests</Label>
                <div className="flex flex-wrap gap-1">
                  {selectedTests.map((testId) => {
                    const test = tests.find((t) => t.id === testId);
                    return test ? (
                      <Badge key={testId} variant="secondary">
                        {test.title}
                      </Badge>
                    ) : null;
                  })}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
