"use client";

import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
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
import { Plus, X, Loader2, ArrowLeft, FileQuestion, Globe } from "lucide-react";
import { toast } from "sonner";
import Link from "next/link";
import { getLanguages } from "@/app/actions/getLanguages";
import {
  Language,
  QuestionTranslation,
  QuestionOptionTranslation,
} from "@/lib/generated/prisma";

interface Test {
  id: number;
  title: string;
}

interface QuestionOption {
  id?: number;
  text: string;
  isCorrect: boolean;
  questionOptionTranslations: QuestionOptionTranslation[];
}

interface Question {
  id: number;
  imageUrl?: string | null;
  questionText: string;
  questionType: string;
  options: QuestionOption[];
  questionTranslations: QuestionTranslation[];
  testQuestions: {
    test: {
      id: number;
      title: string;
    };
  }[];
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

interface FormDataType {
  questionText: string;
  questionType: string;
  file: string;
  questionTranslations: QuestionTranslation[];
}

export default function EditQuestionPage() {
  const router = useRouter();
  const params = useParams();
  const questionId = params.id as string;
  const [currentImageUrl, setCurrentImageUrl] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [translateMode, setTranslateMode] = useState(false);
  const [tests, setTests] = useState<Test[]>([]);
  const [selectedTests, setSelectedTests] = useState<number[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [formData, setFormData] = useState<FormDataType>({
    questionText: "",
    questionType: "mcq",
    file: "",
    questionTranslations: [],
  });
  const [selectedLanguage, setSelectedLanguage] = useState<number | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [files, setFiles] = useState<FileItem[]>([]);
  const [options, setOptions] = useState<QuestionOption[]>([
    { text: "", isCorrect: false, questionOptionTranslations: [] },
    { text: "", isCorrect: false, questionOptionTranslations: [] },
    { text: "", isCorrect: false, questionOptionTranslations: [] },
    { text: "", isCorrect: false, questionOptionTranslations: [] },
  ]);

  useEffect(() => {
    if (questionId) {
      fetchQuestion();
      fetchTests();
    }
  }, [questionId]);

  useEffect(() => {
    Promise.all([fetchFiles(), fetchLanguages()]);
  }, []);

  const fetchQuestion = async () => {
    try {
      const data = await apiClient.get<{ data: Question }>(
        `/api/questions/${questionId}`
      );
      const question = data.data;

      setFormData({
        questionText: question.questionText,
        questionType: question.questionType,
        file: "",
        questionTranslations:
          question.questionTranslations.length > 0
            ? question.questionTranslations
            : [],
      });

      setOptions(
        question.options.map((opt) => ({
          id: opt.id,
          text: opt.text,
          isCorrect: opt.isCorrect,
          questionOptionTranslations: opt.questionOptionTranslations || [],
        }))
      );

      const testIds = question.testQuestions.map((tq) => tq.test.id);
      setSelectedTests(testIds);
      if (question.imageUrl) {
        setCurrentImageUrl(question.imageUrl);
      }
    } catch (error) {
      toast.error("Failed to fetch question");
      console.error("Error fetching question:", error);
    } finally {
      setIsLoading(false);
    }
  };

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

  const fetchTests = async () => {
    try {
      const data = await apiClient.get<{ data: Test[] }>("/api/tests");
      setTests(data.data || []);
    } catch (error) {
      toast.error("Failed to fetch tests");
    }
  };

  const fetchLanguages = async () => {
    const fetchedLanguages = await getLanguages();
    if (fetchedLanguages.data) {
      setLanguages(fetchedLanguages.data);
      setSelectedLanguage(fetchedLanguages.data[0].id);
    }
  };

  const handleOptionChange = (
    index: number,
    field: keyof QuestionOption,
    value: string | boolean
  ) => {
    const newOptions = [...options];
    newOptions[index] = { ...newOptions[index], [field]: value };
    setOptions(newOptions);
  };

  const handleOptionTranslationChange = (index: number, value: string) => {
    const newOptions = [...options];
    const option = newOptions[index];

    const translationIndex = option.questionOptionTranslations.findIndex(
      (t) => t.languageId === selectedLanguage
    );

    if (translationIndex >= 0) {
      option.questionOptionTranslations[translationIndex].text = value;
    } else {
      option.questionOptionTranslations.push({
        id: 0,
        optionId: option.id || 0,
        languageId: selectedLanguage!,
        text: value,
        createdAt: new Date(),
        updatedAt: new Date(),
      } as QuestionOptionTranslation);
    }

    setOptions(newOptions);
  };

  const addOption = () => {
    setOptions([
      ...options,
      { text: "", isCorrect: false, questionOptionTranslations: [] },
    ]);
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

    setIsSubmitting(true);

    try {
      if (translateMode) {
        // Save translations
        await apiClient.put(`/api/questions/${questionId}/translate`, {
          questionTranslations: formData.questionTranslations,
          optionTranslations: options.map((opt) => ({
            optionId: opt.id,
            translations: opt.questionOptionTranslations,
          })),
        });
        toast.success("Translations saved successfully");
      } else {
        // Save main question data
        await apiClient.put(`/api/questions/${questionId}`, {
          file: formData.file,
          questionText: formData.questionText,
          questionType: formData.questionType,
          options: options,
          testIds: selectedTests,
        });
        toast.success("Question updated successfully");
      }

      router.push("/admin/content/questions");
    } catch (error) {
      toast.error(
        translateMode ? "Failed to save translations" : "Failed to update question"
      );
      console.error("Error:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between flex-wrap">
        <div className="flex items-center gap-4">
          <Link href="/admin/content/questions">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Edit Question</h1>
            <p className="text-muted-foreground">
              Update question details and options
            </p>
          </div>
        </div>
        <div className="flex flex-col gap-2">
          <div className="flex gap-2">
            <Switch
              id="translate-toggle"
              onCheckedChange={(checked) => setTranslateMode(Boolean(checked))}
            />
            <Label htmlFor="translate-toggle">Translate Mode</Label>
          </div>
          <div>
            {languages.map((language) => (
              <Button
                key={language.id}
                variant={language.id === selectedLanguage ? "outline" : "ghost"}
                className="cursor-pointer"
                onClick={() => setSelectedLanguage(language.id)}
              >
                <img
                  src={`/${language.languageCode}.png`}
                  alt={language.languageName}
                  className="h-[32px] w-[32px] rounded-md"
                />
              </Button>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Question Details</CardTitle>
            <CardDescription>
              Update the question and its options
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
                disabled={translateMode}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select question type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="mcq">Multiple Choice (MCQ)</SelectItem>
                  <SelectItem value="true_false">True/False</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="questionText">Question Text *</Label>
              <Textarea
                id="questionText"
                placeholder="Enter your question here..."
                value={
                  translateMode
                    ? formData.questionTranslations.find(
                        (t) => t.languageId == selectedLanguage
                      )?.questionText || ""
                    : formData.questionText
                }
                onChange={(e) => {
                  if (translateMode) {
                    const existingTranslation = formData.questionTranslations.find(
                      (t) => t.languageId == selectedLanguage
                    );

                    if (existingTranslation) {
                      setFormData((prev) => ({
                        ...prev,
                        questionTranslations: prev.questionTranslations.map(
                          (qt) =>
                            qt.languageId == selectedLanguage
                              ? { ...qt, questionText: e.target.value }
                              : qt
                        ),
                      }));
                    } else {
                      setFormData((prev) => ({
                        ...prev,
                        questionTranslations: [
                          ...prev.questionTranslations,
                          {
                            id: 0,
                            questionId: parseInt(questionId),
                            languageId: selectedLanguage!,
                            questionText: e.target.value,
                            imageUrl: null,
                            createdAt: new Date(),
                            updatedAt: new Date(),
                          } as QuestionTranslation,
                        ],
                      }));
                    }
                  } else {
                    setFormData((prev) => ({
                      ...prev,
                      questionText: e.target.value,
                    }));
                  }
                }}
                rows={4}
              />
            </div>

            {!translateMode && (
              <div className="space-y-6">
                {currentImageUrl.length > 0 && (
                  <div className="flex justify-center items-center">
                    <img
                      src={currentImageUrl}
                      alt="current question image"
                      className="h-[250px] w-[250px] rounded-md"
                    />
                  </div>
                )}
                <Label htmlFor="file">Question Image</Label>
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
                    <SelectItem value="null">No image</SelectItem>
                    {files.map(
                      (file) =>
                        file.fileType.includes("image") && (
                          <SelectItem key={file.id} value={file.id.toString()}>
                            {file.name} -{" "}
                            {(file.fileSize / 1024 / 1024).toFixed(2)} MB
                          </SelectItem>
                        )
                    )}
                  </SelectContent>
                </Select>
              </div>
            )}

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Label>Options *</Label>
                {formData.questionType === "mcq" && !translateMode && (
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
                    <div className="flex-1">
                      <Input
                        placeholder={`Option ${index + 1}`}
                        value={
                          translateMode
                            ? option.questionOptionTranslations.find(
                                (t) => t.languageId === selectedLanguage
                              )?.text || ""
                            : option.text
                        }
                        onChange={(e) =>
                          translateMode
                            ? handleOptionTranslationChange(
                                index,
                                e.target.value
                              )
                            : handleOptionChange(index, "text", e.target.value)
                        }
                      />
                    </div>
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={option.isCorrect}
                        onCheckedChange={() => handleCorrectOptionChange(index)}
                        disabled={translateMode}
                      />
                      <Label className="text-sm">Correct</Label>
                    </div>
                    {options.length > 2 && !translateMode && (
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

              {formData.questionType === "true_false" &&
                options.length !== 2 && (
                  <div className="text-sm text-amber-600 bg-amber-50 p-3 rounded-lg">
                    True/False questions must have exactly 2 options. Please add
                    or remove options to continue.
                  </div>
                )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Test Assignment</CardTitle>
            <CardDescription>Assign this question to tests</CardDescription>
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
                      disabled={translateMode}
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

            <div className="pt-4 border-t">
              <div className="text-sm text-muted-foreground">
                <FileQuestion className="h-4 w-4 inline mr-1" />
                This question is used in {selectedTests.length} test
                {selectedTests.length !== 1 ? "s" : ""}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="flex justify-end gap-4">
        <Link href="/admin/content/questions">
          <Button variant="outline">Cancel</Button>
        </Link>
        <Button onClick={handleSubmit} disabled={isSubmitting}>
          {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {translateMode ? (
            <>
              <Globe className="h-4 w-4 mr-2" /> Save Translation
            </>
          ) : (
            "Update Question"
          )}
        </Button>
      </div>
    </div>
  );
}