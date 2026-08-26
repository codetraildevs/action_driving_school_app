// app/questions/page.tsx
"use client";

import { useEffect, useState, useRef } from "react";
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import { apiClient } from "@/lib/api-client";
import {
  Plus,
  Search,
  Edit,
  Trash2,
  Loader2,
  List,
  CheckCircle2,
  Circle,
  ArrowUpDown,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { toast } from "sonner";
import Link from "next/link";
import {
  Language,
  QuestionTranslation,
  QuestionOptionTranslation,
  QuestionOption,
} from "@/lib/generated/prisma";
import { getLanguages } from "@/app/actions/getLanguages";
import { format } from "date-fns";

interface QuestionOptionWithTranslations extends QuestionOption {
  questionOptionTranslations: QuestionOptionTranslation[];
}
interface Question {
  id: number;
  questionText: string;
  questionType: string;
  imageUrl: string;
  options: QuestionOptionWithTranslations[];
  questionTranslations: QuestionTranslation[];
  testQuestions: {
    test: {
      title: string;
    };
  }[];
  createdAt: string;
}

export default function QuestionsPage() {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [originalQuestions, setOriginalQuestions] = useState<Question[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [selectedLanguage, setSelectedLanguage] = useState<number | null>(null);
  const [isReversed, setIsReversed] = useState(false);
  const [questionNumberInput, setQuestionNumberInput] = useState("");
  const tableRef = useRef<HTMLDivElement>(null);
  const questionRefs = useRef<Map<number, HTMLTableRowElement>>(new Map());
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [isBulkDeleting, setIsBulkDeleting] = useState(false);

  const translateRow = async (id: number, languageId: number) => {
    setQuestions((prev) =>
      prev.map((question) =>
        question.id === id
          ? {
              ...question,
              questionText: question.questionTranslations.find(
                (t) => t.languageId == languageId
              )?.questionText || question.questionText,

              options: [
                ...question.options.map((opt) => {
                  if (opt.questionId == id) {
                    return {
                      ...opt,
                      text: opt.questionOptionTranslations.find(
                        (ot) => ot.languageId == languageId
                      )?.text || opt.text,
                    };
                  }
                  return opt;
                }),
              ],
            }
          : question
      )
    );
  };

  const fetchLanguages = async () => {
    const fetchedLanguages = await getLanguages();
    if (fetchedLanguages.data) {
      setLanguages(fetchedLanguages.data);
      setSelectedLanguage(fetchedLanguages.data[0].id);
    }
  };

  useEffect(() => {
    fetchLanguages();
  }, []);

  // Paginated fetch: replace the list on page 1 / search change, append on
  // "Load more" (previously every question was returned and rendered at once).
  const fetchQuestions = async (targetPage = 1, append = false) => {
    try {
      if (append) setIsLoadingMore(true);
      else setIsLoading(true);
      const params = new URLSearchParams({
        page: String(targetPage),
        pageSize: "50",
      });
      if (debouncedSearch) params.set("search", debouncedSearch);
      const data = await apiClient.get<{
        data: Question[];
        total?: number;
        totalPages?: number;
      }>(`/api/questions?${params.toString()}`);
      const questionsData = data.data || [];
      setQuestions((prev) =>
        append ? [...prev, ...questionsData] : questionsData
      );
      if (!append) setOriginalQuestions(questionsData);
      setTotal(data.total ?? questionsData.length);
      setTotalPages(data.totalPages ?? 1);
      setPage(targetPage);
    } catch (error) {
      toast.error("Failed to fetch questions");
    } finally {
      if (append) setIsLoadingMore(false);
      else setIsLoading(false);
    }
  };

  // Debounce search input, then reload from page 1 (server-side search)
  useEffect(() => {
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      setDebouncedSearch(searchQuery.trim());
    }, 400);
    return () => {
      if (searchTimer.current) clearTimeout(searchTimer.current);
    };
  }, [searchQuery]);

  useEffect(() => {
    fetchQuestions(1, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch]);

  const handleDelete = async (id: number) => {
    if (
      !confirm(
        "Are you sure you want to delete this question? This action cannot be undone."
      )
    )
      return;
    const questionToDelete = questions.find((question) => question.id === id);
    try {
      setQuestions((prev) => prev.filter((question) => question.id !== id));

      await apiClient.delete(`/api/questions/${id}`);
      toast.success("Question deleted successfully");
    } catch (error) {
      // Revert on error
      if (questionToDelete) {
        setQuestions((prev) =>
          [...prev, questionToDelete].sort((a, b) => a.id - b.id)
        );
      }
      toast.error("Failed to delete question");
    }
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    if (
      !confirm(
        `Are you sure you want to delete ${selectedIds.length} question(s)? This action cannot be undone.`
      )
    )
      return;

    setIsBulkDeleting(true);
    try {
      setQuestions((prev) => prev.filter((q) => !selectedIds.includes(q.id)));
      setSelectedIds([]);

      await apiClient.post(`/api/questions/bulk-delete`, { ids: selectedIds });
      toast.success(`${selectedIds.length} question(s) deleted successfully`);
    } catch (error) {
      toast.error("Failed to delete questions");
      fetchQuestions();
    } finally {
      setIsBulkDeleting(false);
    }
  };

  const toggleSelectAll = () => {
    if (selectedIds.length === filteredQuestions.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(filteredQuestions.map((q) => q.id));
    }
  };

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  const getQuestionTypeBadge = (type: string) => {
    const variants = {
      mcq: "default",
      true_false: "secondary",
    } as const;

    const labels = {
      mcq: "MCQ",
      true_false: "True/False",
    };

    return (
      <Badge variant={variants[type as keyof typeof variants]}>
        {labels[type as keyof typeof labels]}
      </Badge>
    );
  };

  const getCorrectOptions = (options: QuestionOption[]) => {
    return options
      .filter((opt) => opt.isCorrect)
      .map((opt) => opt.text)
      .join(", ");
  };

  const toggleReverseList = () => {
    const reversed = !isReversed;
    setIsReversed(reversed);
    
    if (reversed) {
      setQuestions([...questions].reverse());
    } else {
      setQuestions([...originalQuestions]);
    }
  };

  const scrollToQuestion = () => {
    const questionNumber = parseInt(questionNumberInput);
    if (isNaN(questionNumber) || questionNumber < 1 || questionNumber > questions.length) {
      toast.error(`Please enter a valid question number between 1 and ${questions.length}`);
      return;
    }

    const index = questionNumber - 1;
    const question = questions[index];
    const questionElement = questionRefs.current.get(question.id);

    if (questionElement && tableRef.current) {
      // Scroll the table container to the question
      questionElement.scrollIntoView({ 
        behavior: 'smooth', 
        block: 'center' 
      });
      
      // Add highlight effect
      questionElement.classList.add('bg-yellow-50', 'dark:bg-yellow-950');
      setTimeout(() => {
        questionElement.classList.remove('bg-yellow-50', 'dark:bg-yellow-950');
      }, 2000);
      
      setQuestionNumberInput("");
    }
  };

  const handleQuestionNumberKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      scrollToQuestion();
    }
  };

  const filteredQuestions = questions.filter((question) =>
    question.questionText.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Mobile Card Component
  const QuestionCard = ({ question, index }: { question: Question, index: number }) => (
    <Card className="p-4" id={`question-${question.id}`}>
      <div className="space-y-3">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0 justify-center">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-semibold text-muted-foreground bg-gray-100 dark:bg-gray-800 px-2 py-0.5 rounded">
                #{isReversed ? questions.length - index : index + 1}
              </span>
              <h3 className="font-semibold text-sm line-clamp-2 flex-1">
                {question.questionText}
              </h3>
            </div>
            {question.imageUrl !== null && (
              <img
                src={question.imageUrl}
                alt={question.questionText}
                className="h-[50px] w-[50px] rounded-md mt-2"
              />
            )}
            <div className="flex items-center gap-2 mb-2 mt-2">
              {getQuestionTypeBadge(question.questionType)}
              <Badge variant="outline" className="text-xs">
                {question.options.length} options
              </Badge>
            </div>
          </div>
        </div>

        {/* Correct Answer */}
        <div className="space-y-1">
          <div className="flex items-center gap-1 text-xs font-medium text-green-600">
            <CheckCircle2 className="h-3 w-3" />
            Correct: {getCorrectOptions(question.options)}
          </div>
        </div>

        {/* Options Preview */}
        <div className="space-y-1">
          <p className="text-xs font-medium text-muted-foreground">Options:</p>
          <div className="space-y-1">
            {question.options.map((option, optIndex) => (
              <div key={optIndex} className="flex items-center gap-2 text-xs">
                {option.isCorrect ? (
                  <CheckCircle2 className="h-3 w-3 text-green-600" />
                ) : (
                  <Circle className="h-3 w-3 text-gray-400" />
                )}
                <span className="line-clamp-1">{option.text}</span>
                {option.imageUrl !== null && (
                  <img
                    src={option.imageUrl}
                    alt={option.text}
                    className="h-[20px] w-[20px] rounded-md"
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Used in Tests */}
        {question.testQuestions.length > 0 && (
          <div className="space-y-1">
            <p className="text-xs font-medium text-muted-foreground">
              Used in:
            </p>
            <div className="flex flex-wrap gap-1">
              {question.testQuestions.map((tq, index) => (
                <Badge key={index} variant="outline" className="text-xs">
                  {tq.test.title}
                </Badge>
              ))}
            </div>
          </div>
        )}

        <div className="flex items-center justify-between pt-2">
          <div className="text-xs text-muted-foreground">
            {new Date(question.createdAt).toLocaleDateString()}
          </div>
          <div className="flex gap-1">
            <Link href={`/admin/content/questions/edit/${question.id}`}>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                <Edit className="h-3 w-3" />
              </Button>
            </Link>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0"
              onClick={() => handleDelete(question.id)}
            >
              <Trash2 className="h-3 w-3 text-red-500" />
            </Button>
          </div>
        </div>
      </div>
    </Card>
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Questions</h1>
          <p className="text-muted-foreground">
            Manage test questions and answers
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <Link href="/admin/content/questions/manage">
            <Button variant="outline" size="sm" className="hidden sm:flex">
              <List className="mr-2 h-4 w-4" />
              Manage Tests
            </Button>
            <Button variant="outline" size="sm" className="sm:hidden">
              <List className="h-4 w-4" />
            </Button>
          </Link>
          <Link href="/admin/content/questions/create">
            <Button size="sm">
              <Plus className="mr-2 h-4 w-4" />
              <span className="hidden sm:inline">Add Question</span>
              <span className="sm:hidden">Add</span>
            </Button>
          </Link>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row gap-4 sticky top-15 bg-white p-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search questions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8"
          />
        </div>
        
        <div className="flex gap-2">
          <div className="flex items-center space-x-2">
            <Label htmlFor="question-number" className="text-sm whitespace-nowrap">
              Go to Q#
            </Label>
            <Input
              id="question-number"
              placeholder="1"
              value={questionNumberInput}
              onChange={(e) => setQuestionNumberInput(e.target.value.replace(/\D/g, ''))}
              onKeyPress={handleQuestionNumberKeyPress}
              className="w-20"
              type="number"
              min="1"
              max={questions.length}
            />
            <Button 
              size="sm" 
              variant="secondary"
              onClick={scrollToQuestion}
            >
              <ChevronDown className="h-4 w-4" />
            </Button>
          </div>
          
          <Button 
            variant="outline" 
            size="sm"
            onClick={toggleReverseList}
            className="whitespace-nowrap"
          >
            <ArrowUpDown className="mr-2 h-4 w-4" />
            {isReversed ? (
              <>
                <ChevronUp className="mr-1 h-4 w-4" />
                Reverse
              </>
            ) : (
              <>
                <ChevronDown className="mr-1 h-4 w-4" />
                Reverse
              </>
            )}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>All Questions</CardTitle>
          <CardDescription>
            {total} question
            {total !== 1 ? "s" : ""} found
            {isReversed ? " (Reversed)" : ""}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : filteredQuestions.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No questions found
            </div>
          ) : (
            <>
              {/* Desktop Table View */}
              <div className="hidden md:block overflow-auto" ref={tableRef}>
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
                          checked={selectedIds.length === filteredQuestions.length && filteredQuestions.length > 0}
                          onChange={toggleSelectAll}
                          className="rounded"
                        />
                      </TableHead>
                      <TableHead className="w-16">#</TableHead>
                      <TableHead>Question</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Correct Answer</TableHead>
                      <TableHead>Options</TableHead>
                      <TableHead>Used in Tests</TableHead>
                      <TableHead>Languages</TableHead>
                      <TableHead>Created</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredQuestions.reverse().map((question, index) => (
                      <TableRow 
                        key={question.id} 
                        id={`question-${question.id}`}
                        ref={(el) => {
                          if (el) {
                            questionRefs.current.set(question.id, el);
                          } else {
                            questionRefs.current.delete(question.id);
                          }
                        }}
                      >
                        <TableCell>
                          <input
                            type="checkbox"
                            checked={selectedIds.includes(question.id)}
                            onChange={() => toggleSelect(question.id)}
                            className="rounded"
                          />
                        </TableCell>
                        <TableCell className="font-medium">
                          <span className="text-sm font-semibold text-muted-foreground">
                            #{isReversed ? filteredQuestions.length - index : index + 1}
                          </span>
                        </TableCell>
                        <TableCell className="font-medium max-w-md">
                          <div className="line-clamp-2">
                            {question.questionText}
                          </div>
                          {question.imageUrl !== null && (
                            <img
                              src={question.imageUrl}
                              alt={question.questionText}
                              className="h-[50px] w-[50px] rounded-md mt-1"
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {getQuestionTypeBadge(question.questionType)}
                        </TableCell>
                        <TableCell className="max-w-xs">
                          <div className="line-clamp-2 text-green-600 font-medium">
                            {getCorrectOptions(question.options)}
                          </div>
                        </TableCell>
                        <TableCell className="max-w-xs">
                          <div className="grid grid-cols-1 gap-1 max-w-xs w-full">
                            {question.options.map((opt) => (
                              <div
                                key={opt.id}
                                className="flex items-center gap-1 min-w-0"
                              >
                                <span className="truncate flex-1" title={opt.text}>
                                  {opt.text}
                                </span>
                                {opt.imageUrl && (
                                  <img
                                    src={opt.imageUrl}
                                    alt={opt.text}
                                    className="h-[20px] w-[20px] rounded-md flex-shrink-0"
                                  />
                                )}
                              </div>
                            ))}
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap gap-1 max-w-xs">
                            {question.testQuestions.map((tq, index) => (
                              <Badge
                                key={index}
                                variant="outline"
                                className="text-xs"
                              >
                                {tq.test.title}
                              </Badge>
                            ))}
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap gap-1">
                            {question.questionTranslations.map(
                              (translation) => (
                                <Badge
                                  key={translation.languageId}
                                  variant="secondary"
                                  className="cursor-pointer"
                                  onClick={() =>
                                    translateRow(
                                      question.id,
                                      translation.languageId
                                    )
                                  }
                                >
                                  <img
                                    src={`/${
                                      languages.find(
                                        (t) => t.id == translation.languageId
                                      )?.languageCode
                                    }.png`}
                                    alt=""
                                    className="h-6 w-6 rounded-md"
                                  />
                                </Badge>
                              )
                            )}
                          </div>
                        </TableCell>
                        <TableCell>
                          {format(
                            new Date(question.createdAt),
                            "yyyy-MM-dd HH:mm:ss"
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Link
                              href={`/admin/content/questions/edit/${question.id}`}
                            >
                              <Button variant="ghost" size="sm">
                                <Edit className="h-4 w-4" />
                              </Button>
                            </Link>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDelete(question.id)}
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
                {filteredQuestions.reverse().map((question, index) => (
                  <QuestionCard 
                    key={question.id} 
                    question={question} 
                    index={index}
                  />
                ))}
              </div>

              {/* Load more */}
              {page < totalPages && (
                <div className="flex justify-center py-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => fetchQuestions(page + 1, true)}
                    disabled={isLoadingMore}
                  >
                    {isLoadingMore ? (
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                    ) : null}
                    Load more
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}