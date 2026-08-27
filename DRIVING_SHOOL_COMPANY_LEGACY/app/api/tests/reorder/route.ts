// app/api/tests/reorder/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const reorderTestsHandler = withPermission(PERMISSIONS.TEST_UPDATE)(
  async (request: NextRequest) => {
  try {
    // Parse request body
    const { testOrders } = await request.json();

    if (!testOrders || !Array.isArray(testOrders) || testOrders.length === 0) {
      return NextResponse.json(
        { error: "testOrders array is required" },
        { status: 400 }
      );
    }

    // Validate testOrders structure
    for (const order of testOrders) {
      if (!order.id || typeof order.testNumber !== "number") {
        return NextResponse.json(
          { error: "Each test order must have an id and testNumber" },
          { status: 400 }
        );
      }
    }

    // Alternative swapping approach
    const result = await prisma.$transaction(
      async (tx) => {
        // Use a single temporary holder value
        const TEMP_HOLDER = -999999;

        // Update each test one by one to avoid conflicts
        const results = [];

        for (const order of testOrders) {
          // First, check if the target testNumber is currently occupied
          const occupyingTest = await tx.test.findFirst({
            where: {
              testNumber: order.testNumber,
              id: { not: order.id },
            },
          });

          if (occupyingTest) {
            // If occupied, move the occupying test to temporary holder first
            await tx.test.update({
              where: { id: occupyingTest.id },
              data: { testNumber: TEMP_HOLDER },
            });
          }

          // Now update the current test to its desired position
          const updatedTest = await tx.test.update({
            where: { id: order.id },
            data: { testNumber: order.testNumber },
            include: {
              _count: {
                select: {
                  testQuestions: true,
                },
              },
            },
          });

          results.push(updatedTest);

          if (occupyingTest) {
            const newHome = testOrders.find((t) => t.id === occupyingTest.id);
            if (newHome) {
              await tx.test.update({
                where: { id: occupyingTest.id },
                data: { testNumber: newHome.testNumber },
              });
            }
          }
        }

        return results;
      },
      { maxWait: 60000, timeout: 60000 }
    );

    return NextResponse.json({
      success: true,
      data: result,
      message: "Test order updated successfully",
    });
  } catch (error) {
    console.error("Reorder tests error:", error);
    return NextResponse.json(
      { error: "Failed to update test order" },
      { status: 500 }
    );
  }
  }
);

export const PUT = reorderTestsHandler;
