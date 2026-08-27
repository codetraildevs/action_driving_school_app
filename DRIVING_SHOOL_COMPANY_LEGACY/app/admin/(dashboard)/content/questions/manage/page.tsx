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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { Plus, Trash2, Loader2, ArrowLeft, FileQuestion, Pen } from "lucide-react";
import { toast } from "sonner";
import Link from "next/link";

interface Test {
  id: number;
  title: string;
}

interface Question {
  id: number;
  questionText: string;
  questionType: string;
  imageUrl?: string;
  options: {
    id: number;
    text: string;
    isCorrect: boolean;
  }[];
}

interface TestQuestion {
  id: number;
  question: Question;
}

export default function ManageTestQuestionsPage() {
  const [tests, setTests] = useState<Test[]>([]);
  const [selectedTest, setSelectedTest] = useState<number | null>(null);
  const [testQuestions, setTestQuestions] = useState<Question[]>([]);
  const [availableQuestions, setAvailableQuestions] = useState<Question[]>([]);
  const [selectedQuestions, setSelectedQuestions] = useState<number[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [availablePage, setAvailablePage] = useState(1);
  const [availableTotalPages, setAvailableTotalPages] = useState(1);
  const [isLoadingMoreAvailable, setIsLoadingMoreAvailable] = useState(false);

  useEffect(() => {
    fetchTests();
  }, []);

  const fetchTests = async () => {
    try {
      const data = await apiClient.get<{ data: Test[] }>("/api/tests");
      setTests(data.data || []);
      if (data.data.length > 0) {
        handleTestChange(data.data[0].id);
      }
    } catch (error) {
      toast.error("Failed to fetch tests");
    }
  };

  const fetchTestQuestions = async (testId: number) => {
    try {
      const data = await apiClient.get<any>(`/api/tests/${testId}/questions`);
      setTestQuestions(data.data.questions || []);
    } catch (error) {
      toast.error("Failed to fetch test questions");
    }
  };

  // Paginated: loads 50 questions at a time with "Load more" (previously
  // every question was returned at once).
  const fetchAvailableQuestions = async (targetPage = 1, append = false) => {
    try {
      if (append) setIsLoadingMoreAvailable(true);
      const params = new URLSearchParams({
        page: String(targetPage),
        pageSize: "50",
      });
      const data = await apiClient.get<{
        data: Question[];
        totalPages?: number;
      }>(`/api/questions?${params.toString()}`);
      const list = data.data || [];
      setAvailableQuestions((prev) =>
        append ? [...prev, ...list] : list
      );
      setAvailableTotalPages(data.totalPages ?? 1);
      setAvailablePage(targetPage);
    } catch (error) {
      toast.error("Failed to fetch available questions");
    } finally {
      if (append) setIsLoadingMoreAvailable(false);
    }
  };

  const handleTestChange = async (testId: number) => {
    setSelectedTest(testId);
    setSelectedQuestions([]);
    setIsLoading(true);
    await Promise.all([fetchTestQuestions(testId), fetchAvailableQuestions()]);
    setIsLoading(false);
  };

  const handleAddQuestions = async () => {
    if (!selectedTest || selectedQuestions.length === 0) return;

    try {
      const questionsToAdd = availableQuestions.filter((q) =>
        selectedQuestions.includes(q.id)
      );

      setTestQuestions((prev) => [...prev, ...questionsToAdd]);

      setSelectedQuestions([]);

      await apiClient.post(`/api/tests/${selectedTest}/questions`, {
        questionIds: selectedQuestions,
      });

      toast.success("Questions added to test successfully");

      fetchAvailableQuestions();
    } catch (error) {
      if (selectedTest) {
        fetchTestQuestions(selectedTest);
      }
      toast.error("Failed to add questions to test");
    }
  };

  const handleRemoveQuestion = async (questionId: number) => {
    if (!selectedTest) return;
    const questionToRemove = testQuestions.find((q) => q.id === questionId);

    try {
      setTestQuestions((prev) => prev.filter((q) => q.id !== questionId));

      await apiClient.delete(
        `/api/tests/${selectedTest}/questions/${questionId}`
      );

      toast.success("Question removed from test successfully");

      fetchAvailableQuestions();
    } catch (error) {
      if (selectedTest && questionToRemove) {
        setTestQuestions((prev) => [...prev, questionToRemove]);
      }
      toast.error("Failed to remove question from test");
    }
  };

  const getAvailableQuestionsList = () => {
    const currentQuestionIds = testQuestions.map((tq) => tq.id);
    return availableQuestions.filter((q) => !currentQuestionIds.includes(q.id));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link href="/admin/content/questions">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            Manage Test Questions
          </h1>
          <p className="text-muted-foreground">
            Add or remove questions from tests
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Select Test</CardTitle>
          <CardDescription>
            Choose a test to manage its questions
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Select onValueChange={(value) => handleTestChange(parseInt(value))}>
            <SelectTrigger className="w-full max-w-sm">
              <SelectValue placeholder="Select a test" />
            </SelectTrigger>
            <SelectContent>
              {tests.map((test) => (
                <SelectItem key={test.id} value={test.id.toString()}>
                  {test.title}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {selectedTest && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Available Questions */}
          <Card>
            <CardHeader>
              <CardTitle>Available Questions</CardTitle>
              <CardDescription>
                Select questions to add to the test
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="max-h-96 overflow-y-auto space-y-2">
                  {getAvailableQuestionsList().reverse().map((question, idx) => (
                    <label
                      key={question.id}
                      className="flex items-center space-x-2 p-3 border rounded-lg"
                      htmlFor={question.id.toString()}
                    >
                      <input
                        id={question.id.toString()}
                        type="checkbox"
                        checked={selectedQuestions.includes(question.id)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedQuestions((prev) => [
                              ...prev,
                              question.id,
                            ]);
                          } else {
                            setSelectedQuestions((prev) =>
                              prev.filter((id) => id !== question.id)
                            );
                          }
                        }}
                      />
                      <div className="flex-1">
                        <p className="text-sm font-medium line-clamp-2">
                          {question.questionText}
                        </p>
                        <Badge variant="outline" className="mt-1">
                          {question.questionType === "mcq"
                            ? "MCQ"
                            : "True/False"}
                        </Badge>
                      </div>
                      {question.imageUrl !== null && (
                        <img
                          src={question.imageUrl}
                          alt={question.questionText}
                          className="h-[50px] w-[50px] rounded-md"
                        />
                      )}
                      <Badge>{idx+1}</Badge>
                    </label>
                  ))}
                </div>

                {availablePage < availableTotalPages && (
                  <div className="flex justify-center">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        fetchAvailableQuestions(availablePage + 1, true)
                      }
                      disabled={isLoadingMoreAvailable}
                    >
                      {isLoadingMoreAvailable ? (
                        <Loader2 className="h-4 w-4 animate-spin mr-2" />
                      ) : null}
                      Load more
                    </Button>
                  </div>
                )}

                <Button
                  onClick={handleAddQuestions}
                  disabled={selectedQuestions.length === 0}
                  className="w-full"
                >
                  <Plus className="mr-2 h-4 w-4" />
                  Add Selected Questions ({selectedQuestions.length})
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Current Test Questions */}
          <Card>
            <CardHeader>
              <CardTitle>Test Questions</CardTitle>
              <CardDescription>
                {testQuestions.length} question
                {testQuestions.length !== 1 ? "s" : ""} in this test
              </CardDescription>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin" />
                </div>
              ) : testQuestions.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  No questions in this test yet
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Question</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead className="text-right">Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {testQuestions.map((testQuestion) => (
                      <TableRow key={testQuestion.id}>
                        <TableCell className="max-w-md">
                          <div className="line-clamp-2 text-sm">
                            {testQuestion.questionText}
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">
                            {testQuestion.questionType === "mcq"
                              ? "MCQ"
                              : "True/False"}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right flex items-center justify-center">
                           <Link
                            href={`/admin/content/questions/edit/${testQuestion.id}`}
                         
                          > <Button
                            variant="outline"
                            size="icon-lg"
                           
                          >
                             <Pen className="h-4 w-4" />
                          </Button>
                           
                          </Link>
                          <Button
                            variant="ghost"
                             size="icon-lg"
                            onClick={() =>
                              handleRemoveQuestion(testQuestion.id)
                            }
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
        </div>
      )}
    </div>
  );
}
