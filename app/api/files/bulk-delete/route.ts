import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { unlink } from "fs/promises";
import { join } from "path";
import { existsSync } from "fs";
import { prisma } from "@/lib/prismaDB";

const bulkDeleteHandler = withPermission(PERMISSIONS.PDF_DELETE)(
  async (request: NextRequest, context: { user: any }) => {
    try {
      const { ids } = await request.json();

      if (!ids || !Array.isArray(ids) || ids.length === 0) {
        return NextResponse.json(
          { error: "Array of IDs is required" },
          { status: 400 }
        );
      }

      const numericIds = ids.map((id: string | number) => parseInt(String(id)));

      // Fetch all files to delete physical files
      const files = await prisma.file.findMany({
        where: { id: { in: numericIds } },
        select: { filePath: true, thumbnailUrl: true },
      });

      // Delete physical files from disk
      for (const file of files) {
        if (file.filePath) {
          try {
            const fullPath = join(process.cwd(), "public", file.filePath);
            if (existsSync(fullPath)) {
              await unlink(fullPath);
            }
          } catch (err) {
            console.warn("Could not delete physical file:", err);
          }
        }
        if (file.thumbnailUrl) {
          try {
            const thumbPath = join(process.cwd(), "public", file.thumbnailUrl);
            if (existsSync(thumbPath)) {
              await unlink(thumbPath);
            }
          } catch (err) {
            console.warn("Could not delete thumbnail:", err);
          }
        }
      }

      // Delete from database
      await prisma.file.deleteMany({
        where: { id: { in: numericIds } },
      });

      return NextResponse.json({
        message: `${numericIds.length} file(s) deleted successfully`,
        deletedCount: numericIds.length,
      });
    } catch (error) {
      console.error("Error bulk deleting files:", error);
      return NextResponse.json(
        { error: "Failed to delete files" },
        { status: 500 }
      );
    }
  }
);

export const POST = bulkDeleteHandler;
