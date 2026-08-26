// app/api/files/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { unlink } from "fs/promises";
import { join } from "path";
import { existsSync } from "fs";

import { prisma } from "@/lib/prismaDB";

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await context.params;
    const file = await prisma.file.findUnique({
      where: { id: parseInt(id) },
      include: {
        folder: {
          select: {
            id: true,
            name: true,
            path: true
          }
        }
      }
    });

    if (!file) {
      return NextResponse.json(
        { error: "File not found" },
        { status: 404 }
      );
    }

    return NextResponse.json({ data: file });
  } catch (error) {
    console.error("Error fetching file:", error);
    return NextResponse.json(
      { error: "Failed to fetch file" },
      { status: 500 }
    );
  }
}

const updateFileHandler = withPermission(PERMISSIONS.PDF_UPDATE)(
  async (
    request: NextRequest,
    context: { params: Promise<{ id: string }>; user: any }
  ) => {
  try {
    const { id } = await context.params;
    const { name, description, folderId } = await request.json();

    if (!name) {
      return NextResponse.json(
        { error: "Name is required" },
        { status: 400 }
      );
    }

    const file = await prisma.file.update({
      where: { id: parseInt(id) },
      data: {
        name,
        description: description || "",
        folderId: folderId ? parseInt(folderId) : null,
      },
      include: {
        folder: {
          select: {
            id: true,
            name: true,
            path: true
          }
        }
      }
    });

    return NextResponse.json({ data: file });
  } catch (error) {
    console.error("Error updating file:", error);
    return NextResponse.json(
      { error: "Failed to update file" },
      { status: 500 }
    );
  }
  }
);

export const PUT = updateFileHandler;

const deleteFileHandler = withPermission(PERMISSIONS.PDF_DELETE)(
  async (
    request: NextRequest,
    context: { params: Promise<{ id: string }>; user: any }
  ) => {
  try {
    const { id } = await context.params;
    const file = await prisma.file.findUnique({
      where: { id: parseInt(id) }
    });

    if (!file) {
      return NextResponse.json(
        { error: "File not found" },
        { status: 404 }
      );
    }

    // Delete physical file from disk
    const fileFullPath = join(process.cwd(), "public", file.filePath);
    try {
      if (existsSync(fileFullPath)) {
        await unlink(fileFullPath);
      }
    } catch (error) {
      console.warn("Could not delete physical file:", error);
    }

    // Delete thumbnail from disk if exists
    if (file.thumbnailUrl) {
      const thumbFullPath = join(process.cwd(), "public", file.thumbnailUrl);
      try {
        if (existsSync(thumbFullPath)) {
          await unlink(thumbFullPath);
        }
      } catch (error) {
        console.warn("Could not delete thumbnail:", error);
      }
    }

    // Delete from database
    await prisma.file.delete({
      where: { id: parseInt(id) }
    });

    return NextResponse.json({ success: true, message: "File deleted successfully" });
  } catch (error) {
    console.error("Error deleting file:", error);
    return NextResponse.json(
      { error: "Failed to delete file" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteFileHandler;