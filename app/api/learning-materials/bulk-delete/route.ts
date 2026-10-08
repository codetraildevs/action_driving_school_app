import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const bulkDeleteHandler = withPermission(PERMISSIONS.PDF_DELETE)(
  async (request: NextRequest, { user }) => {
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

      // Audit trail for the Audit Log page (non-fatal).
      try {
        await prisma.userActivity.create({
          data: {
            userId: user.userId,
            activityType: "MATERIAL_BULK_DELETE",
            description: `Bulk deleted ${numericIds.length} learning material(s)`,
          },
        });
      } catch (logError) {
        console.error("Audit log write failed:", logError);
      }

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
