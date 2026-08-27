// app/api/folders/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";

import { prisma } from "@/lib/prismaDB";

export async function GET() {
  try {
    const folders = await prisma.folder.findMany({
      include: {
        _count: {
          select: {
            files: true
          }
        }
      },
      orderBy: { name: "asc" }
    });

    return NextResponse.json({ data: folders });
  } catch (error) {
    console.error("Error fetching folders:", error);
    return NextResponse.json(
      { error: "Failed to fetch folders" },
      { status: 500 }
    );
  }
}

const createFolderHandler = withPermission(PERMISSIONS.PDF_UPLOAD)(
  async (request: NextRequest) => {
  try {
    const { name, parentId } = await request.json();

    if (!name) {
      return NextResponse.json(
        { error: "Folder name is required" },
        { status: 400 }
      );
    }

    // Generate path
    let path = name.toLowerCase().replace(/[^a-z0-9]/g, '-');
    if (parentId) {
      const parent = await prisma.folder.findUnique({
        where: { id: parentId }
      });
      path = `${parent?.path}/${path}`;
    }

    const folder = await prisma.folder.create({
      data: {
        name,
        path,
        parentId: parentId || null,
      },
      include: {
        _count: {
          select: {
            files: true
          }
        }
      }
    });

    return NextResponse.json({ data: folder }, { status: 201 });
  } catch (error) {
    console.error("Error creating folder:", error);
    return NextResponse.json(
      { error: "Failed to create folder" },
      { status: 500 }
    );
  }
  }
);

export const POST = createFolderHandler;