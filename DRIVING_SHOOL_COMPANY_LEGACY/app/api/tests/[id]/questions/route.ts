import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { UserTestAccessStatus } from "@/lib/generated/prisma";
import { verifyToken } from "@/lib/auth/jwt";
import { prisma } from "@/lib/prismaDB";

interface Params {
  params: {
    id: string;
  };
}

export async function GET(request: NextRequest, { params }: Params) {
  // Authentication
  const authHeader = request.headers.get("authorization");
  try {
    if (!authHeader?.startsWith("Bearer ")) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Missing or malformed token" },
        { status: 401 }
      );
    }

    const token = authHeader.substring(7);
    const payload = await verifyToken(token);

    if (!payload?.userId) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Invalid or expired token" },
        { status: 401 }
      );
    }

    const userId = payload.userId;
    const testId = parseInt(params.id);

    if (isNaN(testId)) {
      return NextResponse.json(
        { success: false, error: "Invalid test number" },
        { status: 400 }
      );
    }

    // Single transaction for all data fetching
    const result = await prisma.$transaction(
      async (tx) => {
        // Get user with role, test access, and subscription in parallel
        const [user, userTestAccess, test] = await Promise.all([
          tx.user.findFirst({
            where: { id: userId },
            include: { role: true },
          }),
          tx.userTestAccess.findFirst({
            where: {
              userId,
              status: {in:[UserTestAccessStatus.ACTIVE, UserTestAccessStatus.PENDING]},
            },
            select: { maxTest: true }, 
          }),
          tx.test.findUnique({
            where: { id: testId },
          }),
        ]);
        // console.log(userTestAccess)

        if (test && user?.role.roleName === "admin") {
          const testQuestions = await tx.testQuestion.findMany({
            where: { testId: test.id },
            include: {
              question: {
                include: {
                  questionTranslations: true,
                  options: {
                    include: {
                      questionOptionTranslations: true,
                    },
                     
                  },
                },
              },
            },
          });

          return {
            test,
            questions: testQuestions.map((tq) => tq.question),
          };
        }

        if (!test || !userTestAccess) {
          throw new Error("TEST_NOT_FOUND");
        }

        const isAdmin = user?.role.roleName === "admin";
        const hasAccess =
          isAdmin ||
          test.isFree ||
          (test.testNumber != null && test.testNumber <= userTestAccess.maxTest);

        if (!hasAccess) {
          throw new Error("ACCESS_DENIED");
        }

        const testQuestions = await tx.testQuestion.findMany({
          where: { testId: test.id },
          include: {
            question: {
              include: {
                questionTranslations: true,
                options: {
                  include: {
                    questionOptionTranslations: true,
                  },
                  
                },
              },
            },
          },
        });

        return {
          test,
          questions: testQuestions.map((tq) => tq.question),
          userAccessLevel: userTestAccess.maxTest,
        };
      },
      {
        maxWait: 60000,
        timeout: 30000,
      }
    );

    return NextResponse.json({
      success: true,
      data: {
        test: result.test,
        questions: result.questions,
      },
    });
  } catch (error: any) {
    console.error("Get test questions error:", error);

    // Handle specific errors
    if (error.message === "TEST_NOT_FOUND") {
      return NextResponse.json(
        { error: "Test not found or access not available" },
        { status: 404 }
      );
    }

    if (error.message === "ACCESS_DENIED") {
      // Get user access level for better error message
      const userId = (await verifyToken(authHeader!.substring(7)))?.userId;
      const userTestAccess = await prisma.userTestAccess.findFirst({
        where: {
          userId,
          status: UserTestAccessStatus.ACTIVE,
        },
        select: { maxTest: true },
      });

      return NextResponse.json(
        {
          error: `Upgrade your access to ${userTestAccess?.maxTest} to access this test`,
        },
        { status: 403 }
      );
    }

    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

const addQuestionsToTestHandler = withPermission(PERMISSIONS.TEST_UPDATE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { questionIds } = await request.json();

    if (!questionIds || !Array.isArray(questionIds)) {
      return NextResponse.json(
        { error: "Question IDs array is required" },
        { status: 400 }
      );
    }

    const testId = parseInt(params.id);

    if (isNaN(testId)) {
      return NextResponse.json({ error: "Invalid test ID" }, { status: 400 });
    }

    // Verify test exists first
    const testExists = await prisma.test.findUnique({
      where: { id: testId },
      select: { id: true },
    });

    if (!testExists) {
      return NextResponse.json({ error: "Test not found" }, { status: 404 });
    }

    // Use transaction for data consistency
    const result = await prisma.$transaction(async (tx) => {
      // Add questions to test with batch operation
      const testQuestions = await tx.testQuestion.createMany({
        data: questionIds.map((questionId) => ({
          testId,
          questionId,
        })),
        skipDuplicates: true,
      });

      return testQuestions;
    });

    return NextResponse.json(
      {
        success: true,
        data: result,
        message: `Successfully added ${result.count} questions to test`,
      },
      { status: 201 }
    );
  } catch (error: any) {
    console.error("Error adding questions to test:", error);

    // Handle specific Prisma errors
    if (error.code === "P2003") {
      return NextResponse.json(
        { error: "One or more questions not found" },
        { status: 404 }
      );
    }

    if (error.code === "P2002") {
      return NextResponse.json(
        { error: "Duplicate questions detected" },
        { status: 409 }
      );
    }

    return NextResponse.json(
      { error: "Failed to add questions to test" },
      { status: 500 }
    );
  }
  }
);

export const POST = addQuestionsToTestHandler;
