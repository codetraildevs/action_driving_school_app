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

      // Delete test question associations first
      await prisma.testQuestion.deleteMany({
        where: { questionId: { in: numericIds } },
      });

      // Delete question option translations
      const questionOptions = await prisma.questionOption.findMany({
        where: { questionId: { in: numericIds } },
        select: { id: true },
      });
      const optionIds = questionOptions.map((o) => o.id);

      if (optionIds.length > 0) {
        await prisma.questionOptionTranslation.deleteMany({
          where: { optionId: { in: optionIds } },
        });
        await prisma.questionOption.deleteMany({
          where: { questionId: { in: numericIds } },
        });
      }

      // Delete question translations
      await prisma.questionTranslation.deleteMany({
        where: { questionId: { in: numericIds } },
      });

      // Delete the questions
      await prisma.question.deleteMany({
        where: { id: { in: numericIds } },
      });

      return NextResponse.json({
        message: `${numericIds.length} question(s) deleted successfully`,
        deletedCount: numericIds.length,
      });
    } catch (error) {
      console.error("Error bulk deleting questions:", error);
      return NextResponse.json(
        { error: "Failed to delete questions" },
        { status: 500 }
      );
    }
  }
);

export const POST = bulkDeleteHandler;
