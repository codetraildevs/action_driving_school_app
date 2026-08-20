import { NextRequest, NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';

import { prisma } from "@/lib/prismaDB";
const reorderQuestionsHandler = withPermission(PERMISSIONS.TEST_UPDATE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { id } = params;
    const { title, description, totalMarks, passMarks, duration, testNumber, imageUrl, isFree } = await request.json();

    if (!title || !totalMarks || !passMarks || !duration || !testNumber) {
      return NextResponse.json(
        { error: "All fields are required" },
        { status: 400 }
      );
    }

    if (passMarks > totalMarks) {
      return NextResponse.json(
        { error: "Pass marks cannot exceed total marks" },
        { status: 400 }
      );
    }

    // Check if test number already exists for other tests
    const existingTest = await prisma.test.findFirst({
      where: {
        testNumber: parseInt(testNumber),
        NOT: {
          id: parseInt(id)
        }
      }
    });

    if (existingTest) {
      return NextResponse.json(
        { error: "Test number already exists" },
        { status: 400 }
      );
    }

    const test = await prisma.test.update({
      where: { id: parseInt(id) },
      data: {
        title,
        description,
        totalMarks: parseInt(totalMarks),
        passMarks: parseInt(passMarks),
        duration: parseInt(duration),
        testNumber: parseInt(testNumber),
        imageUrl,
        isFree
      },
      include: {
        _count: {
          select: {
            testQuestions: true,
          },
        },
      },
    });

    return NextResponse.json({ data: test });
  } catch (error) {
    console.error("Error updating test:", error);
    return NextResponse.json(
      { error: "Failed to update test" },
      { status: 500 }
    );
  }
  }
);

export const PUT = reorderQuestionsHandler;
