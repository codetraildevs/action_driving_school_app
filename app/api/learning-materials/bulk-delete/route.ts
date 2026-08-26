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

      // Delete associated user learning materials first
      await prisma.userLearningMaterial.deleteMany({
        where: { learningMaterialId: { in: numericIds } },
      });

      // Delete the learning materials
      await prisma.learningMaterial.deleteMany({
        where: { id: { in: numericIds } },
      });

      return NextResponse.json({
        message: `${numericIds.length} material(s) deleted successfully`,
        deletedCount: numericIds.length,
      });
    } catch (error) {
      console.error("Error bulk deleting learning materials:", error);
      return NextResponse.json(
        { error: "Failed to delete learning materials" },
        { status: 500 }
      );
    }
  }
);

export const POST = bulkDeleteHandler;
