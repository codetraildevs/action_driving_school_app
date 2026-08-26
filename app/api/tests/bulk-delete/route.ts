import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const bulkDeleteHandler = withPermission(PERMISSIONS.PDF_DELETE)(
  async (request: NextRequest) => {
    try {
      const { ids } = await request.json();

      if (!ids || !Array.isArray(ids) || ids.length === 0) {
        return NextResponse.json(
          { error: "Array of IDs is required" },
          { status: 400 }
        );
      }

      const numericIds = ids.map((id: string | number) => parseInt(String(id)));

      // Delete test questions associations first
      await prisma.testQuestion.deleteMany({
        where: { testId: { in: numericIds } },
      });

      // Delete test translations
      await prisma.testTranslation.deleteMany({
        where: { testId: { in: numericIds } },
      });

      // Delete the tests
      await prisma.test.deleteMany({
        where: { id: { in: numericIds } },
      });

      return NextResponse.json({
        message: `${numericIds.length} test(s) deleted successfully`,
        deletedCount: numericIds.length,
      });
    } catch (error) {
      console.error("Error bulk deleting tests:", error);
      return NextResponse.json(
        { error: "Failed to delete tests" },
        { status: 500 }
      );
    }
  }
);

export const POST = bulkDeleteHandler;
