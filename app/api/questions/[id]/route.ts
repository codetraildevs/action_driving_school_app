// app/api/questions/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { PrismaClient, QuestionType } from "@/lib/generated/prisma";

import { prisma } from "@/lib/prismaDB";

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await context.params;
    const question = await prisma.question.findUnique({
      where: { id: parseInt(id) },
      include: {
        options:{
          include:{
            questionOptionTranslations:true
          }
        },
        questionTranslations:true,
        testQuestions: {
          include: {
          
            test: {
              select: {
                id: true,
                title: true
              }
            }
          }
        }
      }
    });

    if (!question) {
      return NextResponse.json(
        { error: "Question not found" },
        { status: 404 }
      );
    }

    return NextResponse.json({ data: question });
  } catch (error) {
    console.error("Error fetching question:", error);
    return NextResponse.json(
      { error: "Failed to fetch question" },
      { status: 500 }
    );
  }
}

const updateQuestionHandler = withPermission(PERMISSIONS.TEST_UPDATE)(
  async (
    request: NextRequest,
    context: { params: Promise<{ id: string }>; user: any }
  ) => {
  try {
    const { id } = await context.params;
    const { file, questionText, questionType, options, testIds } = await request.json();

    if (!questionText || !questionType || !options || !Array.isArray(options)) {
      return NextResponse.json(
        { error: "Question text, type, and options are required" },
        { status: 400 }
      );
    }

    // Validate options
    const correctOptions = options.filter(opt => opt.isCorrect);
    if (correctOptions.length === 0) {
      return NextResponse.json(
        { error: "At least one correct option is required" },
        { status: 400 }
      );
    }

    // Delete existing options and test relations
    await prisma.$transaction([
      prisma.questionOption.deleteMany({
        where: { questionId: parseInt(id) }
      }),
      prisma.testQuestion.deleteMany({
        where: { questionId: parseInt(id) }
      })
    ]);

     
    let MyFilePath = null;
    
    if (file && parseInt(file)) {
      const fileTouse = await prisma.file.findFirst({
        where: { id: parseInt(file) },
      });
 
      MyFilePath = fileTouse?.filePath;
      
    }

    const question = await prisma.question.update({
      where: { id: parseInt(id) },
      data: {
        imageUrl:MyFilePath,
        questionText,
        questionType: questionType as QuestionType,
        options: {
          create: options.map(opt => ({
            text: opt.text,
            isCorrect: opt.isCorrect
          }))
        },
        testQuestions: testIds && Array.isArray(testIds) ? {
          create: testIds.map((testId: number) => ({
            testId: testId
          }))
        } : undefined
      },
      include: {
        options: true,
        testQuestions: {
          include: {
            test: {
              select: {
                title: true
              }
            }
          }
        }
      }
    });

    return NextResponse.json({ data: question });
  } catch (error) {
    console.error("Error updating question:", error);
    return NextResponse.json(
      { error: "Failed to update question" },
      { status: 500 }
    );
  }
  }
);

export const PUT = updateQuestionHandler;

const deleteQuestionHandler = withPermission(PERMISSIONS.TEST_DELETE)(
  async (
    request: NextRequest,
    context: { params: Promise<{ id: string }>; user: any }
  ) => {
  try {
    const { id } = await context.params;
    const questionId = parseInt(id);

    // Clean up related records first
    await prisma.testQuestion.deleteMany({
      where: { questionId },
    });

    // Delete question option translations and options
    const options = await prisma.questionOption.findMany({
      where: { questionId },
      select: { id: true },
    });
    if (options.length > 0) {
      const optionIds = options.map((o) => o.id);
      await prisma.questionOptionTranslation.deleteMany({
        where: { optionId: { in: optionIds } },
      });
      await prisma.questionOption.deleteMany({
        where: { questionId },
      });
    }

    // Delete question translations
    await prisma.questionTranslation.deleteMany({
      where: { questionId },
    });

    // Delete the question
    await prisma.question.delete({
      where: { id: questionId },
    });

    return NextResponse.json({ success: true, message: "Question deleted successfully" });
  } catch (error) {
    console.error("Error deleting question:", error);
    return NextResponse.json(
      { error: "Failed to delete question" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteQuestionHandler;