// app/api/files/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { writeFile, mkdir, unlink } from "fs/promises";
import { join } from "path";
 ;
import { generateThumbnail } from "@/lib/thum-utils";

import { prisma } from "@/lib/prismaDB";

// List files. Filters (folderId, search) are applied server-side, and the
// file manager page uses page/pageSize to stay bounded (without a page param
// the full list is returned, preserving backward compatibility for the file
// pickers used by the questions/tests pages).
export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const folderId = searchParams.get("folderId");
    const type = (searchParams.get("type") || "").trim();
    const search = (searchParams.get("search") || "").trim();

    const pageParam = parseInt(searchParams.get("page") || "", 10);
    const pageSizeParam = parseInt(
      searchParams.get("pageSize") || "50",
      10,
    );
    const hasPagination = Number.isFinite(pageParam) && pageParam > 0;
    const page = hasPagination ? pageParam : 1;
    const pageSize =
      Number.isFinite(pageSizeParam) && pageSizeParam > 0
        ? Math.min(pageSizeParam, 100)
        : 50;

    const where: any = {};
    if (folderId) {
      where.folderId = parseInt(folderId);
    }
    if (type) {
      // type=pdf -> fileType contains "pdf", type=image -> contains "image"
      where.fileType = { contains: type };
    }
    if (search) {
      where.OR = [
        { name: { contains: search } },
        { description: { contains: search } },
      ];
    }

    const [files, total, typeCounts] = await Promise.all([
      prisma.file.findMany({
        where,
        include: {
          folder: {
            select: {
              id: true,
              name: true,
              path: true
            }
          }
        },
        orderBy: { createdAt: "desc" },
        ...(hasPagination
          ? { skip: (page - 1) * pageSize, take: pageSize }
          : {}),
      }),
      hasPagination ? prisma.file.count({ where }) : Promise.resolve(0),
      hasPagination
        ? prisma.file.groupBy({
            by: ["fileType"],
            where,
            _count: { _all: true },
          })
        : Promise.resolve([]),
    ]);

    // Per-type totals so the file manager stats stay accurate while the
    // visible list is paginated.
    const counts = typeCounts.reduce(
      (acc, group) => {
        const type = group.fileType;
        if (type.includes("image")) acc.images += group._count._all;
        else if (type.includes("pdf")) acc.pdfs += group._count._all;
        return acc;
      },
      { images: 0, pdfs: 0 },
    );

    return NextResponse.json({
      data: files,
      ...(hasPagination
        ? {
            total,
            counts,
            page,
            pageSize,
            totalPages: Math.max(1, Math.ceil(total / pageSize)),
          }
        : {}),
    });
  } catch (error) {
    console.error("Error fetching files:", error);
    return NextResponse.json(
      { error: "Failed to fetch files" },
      { status: 500 }
    );
  }
}

// Upload new file (admin-only: only the console's file manager uploads)
const uploadFileHandler = withPermission(PERMISSIONS.PDF_UPLOAD)(
  async (request: NextRequest) => {
  try {
    const formData = await request.formData();
    const file = formData.get("file") as File;
    const name = formData.get("name") as string;
    const description = formData.get("description") as string;
    const folderId = formData.get("folderId") as string;

    if (!file || !name) {
      return NextResponse.json(
        { error: "File and name are required" },
        { status: 400 }
      );
    }

    // Validate file type
    const allowedTypes = [
      "application/pdf",
      "application/vnd.ms-powerpoint",
      "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      "application/msword",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "video/mp4",
      "video/mpeg",
      "audio/mpeg",
      "image/jpeg",
      "image/png",
      "image/gif",
      "text/plain",
    ];

    if (!allowedTypes.includes(file.type)) {
      return NextResponse.json(
        { error: "File type not allowed" },
        { status: 400 }
      );
    }

    // Validate file size (50MB max)
    const maxSize = 50 * 1024 * 1024;
    if (file.size > maxSize) {
      return NextResponse.json(
        { error: "File size too large. Maximum 50MB allowed." },
        { status: 400 }
      );
    }

    // Create uploads directory if it doesn't exist
    const uploadsDir = join(process.cwd(), "public", "uploads", "files");
    await mkdir(uploadsDir, { recursive: true });

    // Generate unique filename
    const timestamp = Date.now();
    const originalName = file.name;
    const fileExtension = originalName.split(".").pop();
    const fileName = `${timestamp}-${Math.random()
      .toString(36)
      .substring(2)}.${fileExtension}`;
    const filePath = join(uploadsDir, fileName);

    // Convert file to buffer and save
    const bytes = await file.arrayBuffer();
    const buffer = Buffer.from(bytes);
    await writeFile(filePath, buffer);

    // Generate thumbnail (always SVG placeholder — format option is ignored)
    const thumbnailPath = await generateThumbnail(
      filePath,
      file.type,
      name,
      `thumb-${timestamp}`,
      {
        width: 300,
        height: 200,
        quality: 85,
      }
    );

    // Save to database
    const fileRecord = await prisma.file.create({
      data: {
        name,
        description: description || "",
        filePath: `/uploads/files/${fileName}`,
        fileType: file.type,
        fileSize: file.size,
        thumbnailUrl: thumbnailPath,
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

    return NextResponse.json({ data: fileRecord }, { status: 201 });
  } catch (error) {
    console.error("Error uploading file:", error);
    return NextResponse.json(
      { error: "Failed to upload file" },
      { status: 500 }
    );
  }
  }
);

export const POST = uploadFileHandler;