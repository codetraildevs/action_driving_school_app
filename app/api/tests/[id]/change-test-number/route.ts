import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";

import { prisma } from "@/lib/prismaDB";

const changeTestNumberHandler = withPermission(PERMISSIONS.TEST_UPDATE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string; questionId: string } }
  ) => {
  try {
    const testId = parseInt(params.id);
    const questionId = parseInt(params.questionId);

    // Validate IDs
    if (isNaN(testId) || isNaN(questionId)) {
      return NextResponse.json(
        { error: "Invalid test ID or question ID" },
        { status: 400 }
      );
    }

 
    const testQuestion = await prisma.testQuestion.findFirst({
      where: {
        testId: testId,
        questionId: questionId
      }
    });

    if (!testQuestion) {
      return NextResponse.json(
        { error: "Question not found in this test" },
        { status: 404 }
      );
    }

    // Delete using the auto-generated ID
    await prisma.testQuestion.delete({
      where: {
        id: testQuestion.id
      }
    });

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("Error removing question from test:", error);
    return NextResponse.json(
      { error: "Failed to remove question from test" },
      { status: 500 }
    );
  }
  }
);

export const PATCH = changeTestNumberHandler;