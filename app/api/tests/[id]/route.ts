import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await context.params;
    const test = await prisma.test.findUnique({
      where: { id: parseInt(id) },
      include: {
        testTranslations: true,

        _count: {
          select: {
            testQuestions: true,
          },
        },
      },
    });

    if (!test) {
      return NextResponse.json({ error: "Test not found" }, { status: 404 });
    }

    return NextResponse.json({ data: test });
  } catch (error) {
    console.error("Error fetching test:", error);
    return NextResponse.json(
      { error: "Failed to fetch test" },
      { status: 500 }
    );
  }
}

const updateTestHandler = withPermission(PERMISSIONS.TEST_UPDATE)(
  async (
    request: NextRequest,
    context: { params: Promise<{ id: string }>; user: any }
  ) => {
  try {
    const { id } = await context.params;
    const {
      title,
      description,
      totalMarks,
      passMarks,
      duration,
      isFree,
      imageUrl,
    } = await request.json();

    if (!title || !totalMarks || !passMarks || !duration) {
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

    const test = await prisma.test.update({
      where: { id: parseInt(id) },
      data: {
        title,
        description,
        totalMarks: parseInt(totalMarks),
        passMarks: parseInt(passMarks),
        duration: parseInt(duration),
        imageUrl,
        isFree,
      },
      include: {
        testTranslations: true,
        _count: {
          select: {
            testQuestions: true,
          },
        },
      },
    });    return NextResponse.json({ data: test });
  } catch (error) {
    console.error("Error updating test:", error);
    return NextResponse.json(
      { error: "Failed to update test" },
      { status: 500 }
    );
  }
  }
);

export const PUT = updateTestHandler;

const deleteTestHandler = withPermission(PERMISSIONS.TEST_DELETE)(
  async (
    request: NextRequest,
    context: { params: Promise<{ id: string }>; user: any }
  ) => {
  try {
    const { id } = await context.params;
    const testId = parseInt(id);

    // Clean up related records first
    await prisma.testQuestion.deleteMany({ where: { testId } });
    await prisma.testTranslation.deleteMany({ where: { testId } });

    // Use deleteMany to avoid P2025 if test was already cascade-deleted
    const result = await prisma.test.deleteMany({ where: { id: testId } });

    if (result.count === 0) {
      return NextResponse.json({ error: "Test not found" }, { status: 404 });
    }

    return NextResponse.json({ success: true, message: "Test deleted successfully" });
  } catch (error) {
    console.error("Error deleting test:", error);
    return NextResponse.json(
      { error: "Failed to delete test" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteTestHandler;
