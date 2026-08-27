// app/api/questions/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { Prisma, QuestionType } from "@/lib/generated/prisma";
import { prisma } from "@/lib/prismaDB";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const testId = searchParams.get("testId");

    // Optional server-side pagination + search. When no page param is sent we
    // keep returning everything (backward compatible) so external callers are
    // unaffected; the admin console pages pass page/pageSize to stay bounded.
    const pageParam = parseInt(searchParams.get("page") || "", 10);
    const pageSizeParam = parseInt(
      searchParams.get("pageSize") || "50",
      10,
    );
    const search = (searchParams.get("search") || "").trim();

    const hasPagination = Number.isFinite(pageParam) && pageParam > 0;
    const page = hasPagination ? pageParam : 1;
    const pageSize =
      Number.isFinite(pageSizeParam) && pageSizeParam > 0
        ? Math.min(pageSizeParam, 100)
        : 50;

    const where: Prisma.QuestionWhereInput = testId
      ? {
          testQuestions: {
            some: {
              testId: parseInt(testId),
            },
          },
        }
      : {};
    if (search) {
      where.OR = [{ questionText: { contains: search } }];
    }

    const [questions, total] = await Promise.all([
      prisma.question.findMany({
        where,
        select: {
          id: true,
          questionText: true,
          questionType: true,
          imageUrl: true,
          createdAt: true,
          options: {
            select: {
              id: true,
              questionId: true,
              text: true,
              isCorrect: true,
              imageUrl: true,
              questionOptionTranslations: {
                select: {
                  languageId: true,
                  text: true,
                },
              },
            },
          },
          questionTranslations: {
            select: {
              languageId: true,
              questionText: true,
            },
          },
          testQuestions: {
            select: {
              test: {
                select: {
                  title: true,
                },
              },
            },
          },
        },
        orderBy: { createdAt: "desc" },
        ...(hasPagination
          ? { skip: (page - 1) * pageSize, take: pageSize }
          : {}),
      }),
      hasPagination ? prisma.question.count({ where }) : Promise.resolve(0),
    ]);

    return NextResponse.json({
      data: questions,
      ...(hasPagination
        ? {
            total,
            page,
            pageSize,
            totalPages: Math.max(1, Math.ceil(total / pageSize)),
          }
        : {}),
    });
  } catch (error) {
    console.error("Error fetching questions:", error);
    return NextResponse.json(
      { error: "Failed to fetch questions" },
      { status: 500 }
    );
  }
}

const createQuestionHandler = withPermission(PERMISSIONS.TEST_CREATE)(
  async (request: NextRequest) => {
  try {
    const { file, questionText, questionType, options, testIds } =
      await request.json();

    if (!questionText || !questionType || !options || !Array.isArray(options)) {
      return NextResponse.json(
        { error: "Question text, type, and options are required" },
        { status: 400 }
      );
    }

    // Validate options
    const correctOptions = options.filter((opt) => opt.isCorrect);
    if (correctOptions.length === 0) {
      return NextResponse.json(
        { error: "At least one correct option is required" },
        { status: 400 }
      );
    }

    if (questionType === "true_false" && options.length !== 2) {
      return NextResponse.json(
        { error: "True/False questions must have exactly 2 options" },
        { status: 400 }
      );
    }

   
    const resp = await prisma.$transaction(

      async (tx) => {
         let imagePath = null;
        if (file && parseInt(file)) {
          const fileTouse = await tx.file.findFirst({
            where: { id: parseInt(file) },
          });

          imagePath = fileTouse?.filePath;
        }

        const question = await tx.question.create({
          data: {
            questionText,
            imageUrl: imagePath,
            questionType: questionType as QuestionType,
            options: {
              create: options.map((opt) => ({
                text: opt.text,
                isCorrect: opt.isCorrect,
                imageUrl:opt.imageUrl!==null?opt.imageUrl:null
              })),
            },
            testQuestions:
              testIds && Array.isArray(testIds)
                ? {
                    create: testIds.map((testId: number) => ({
                      testId: testId,
                    })),
                  }
                : undefined,
          },
          include: {
            options: true,
            testQuestions: {
              include: {
                test: {
                  select: {
                    title: true,
                  },
                },
              },
            },
          },
        });

        return NextResponse.json({ data: question }, { status: 201 });
      },
      { maxWait: 60000, timeout: 60000 }
    );
    return resp;
  } catch (error) {
    console.error("Error creating question:", error);
    return NextResponse.json(
      { error: "Failed to create question" },
      { status: 500 }
    );
  }
  }
);

export const POST = createQuestionHandler;
