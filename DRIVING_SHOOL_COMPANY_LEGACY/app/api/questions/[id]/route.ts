// app/api/questions/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { PrismaClient, QuestionType } from "@/lib/generated/prisma";

import { prisma } from "@/lib/prismaDB";

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const question = await prisma.question.findUnique({
      where: { id: parseInt(params.id) },
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
    { params }: { params: { id: string } }
  ) => {
  try {
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
        where: { questionId: parseInt(params.id) }
      }),
      prisma.testQuestion.deleteMany({
        where: { questionId: parseInt(params.id) }
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
      where: { id: parseInt(params.id) },
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
    { params }: { params: { id: string } }
  ) => {
  try {
    await prisma.question.delete({
      where: { id: parseInt(params.id) }
    });

    return NextResponse.json({ success: true });
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