// app/api/learning-materials/[id]/download/route.ts
import { NextRequest, NextResponse } from "next/server";
import { readFile, stat } from "fs/promises";
import { join } from "path";
 ;
import { verifyToken } from "@/lib/auth/jwt";

import { prisma } from "@/lib/prismaDB";

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const material = await prisma.learningMaterial.findUnique({
      where: { id: parseInt(params.id) },
    });

    if (!material) {
      return NextResponse.json(
        { error: "Learning material not found" },
        { status: 404 }
      );
    }

    if (!material.isPublic) {
      const authHeader = request.headers.get("authorization");
      if (!authHeader || !authHeader.startsWith("Bearer ")) {
        return NextResponse.json(
          { error: "Unauthorized: Missing or malformed token" },
          { status: 401 }
        );
      }
      const token = authHeader.substring(7);
      const payload = await verifyToken(token);
      if (!payload || !payload.userId) {
        return NextResponse.json(
          { error: "Unauthorized: Invalid or expired token" },
          { status: 401 }
        );
      }
    }

    const filePath = join(process.cwd(), "public", material.filePath);

    try {
      await stat(filePath);
    } catch {
      return NextResponse.json(
        { error: "File not found on server" },
        { status: 404 }
      );
    }

    const fileBuffer = await readFile(filePath);

    // Track download if user is authenticated
    const authHeader = request.headers.get("authorization");
    if (authHeader && authHeader.startsWith("Bearer ")) {
      try {
        const token = authHeader.substring(7);
        const payload = await verifyToken(token);

        if (payload && payload.userId) {
          const userId = payload.userId;

          const existingRecord = await prisma.userLearningMaterial.findFirst({
            where: {
              userId: userId,
              learningMaterialId: material.id,
            },
          });

          if (existingRecord) {
            await prisma.userLearningMaterial.update({
              where: { id: existingRecord.id },
              data: { downloadedAt: new Date() },
            });
          } else {
            await prisma.userLearningMaterial.create({
              data: {
                userId: userId,
                learningMaterialId: material.id,
                downloadedAt: new Date(),
              },
            });
          }
        }
      } catch (error) {
        console.error("Error tracking download:", error);
      }
    }

    // Convert Buffer to array then Blob
    const blob = new Blob([Uint8Array.from(fileBuffer)], {
      type: material.fileType,
    });

    return new NextResponse(blob, {
      headers: {
        "Content-Type": material.fileType,
        "Content-Disposition": `attachment; filename="${encodeURIComponent(
          material.title
        )}.${getFileExtension(material.fileType)}"`,
        "Content-Length": fileBuffer.length.toString(),
        "Cache-Control": "no-cache, no-store, must-revalidate",
        "Access-Control-Expose-Headers": "Content-Disposition",
      },
    });
  } catch (error) {
    console.error("Error downloading file:", error);
    return NextResponse.json(
      { error: "Failed to download file" },
      { status: 500 }
    );
  }
}

// Helper function to get file extension
function getFileExtension(fileType: string): string {
  const extensions: { [key: string]: string } = {
    "application/pdf": "pdf",
    "application/vnd.ms-powerpoint": "ppt",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation":
      "pptx",
    "application/msword": "doc",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
      "docx",
    "video/mp4": "mp4",
    "audio/mpeg": "mp3",
    "image/jpeg": "jpg",
    "image/png": "png",
    "text/plain": "txt",
  };
  return extensions[fileType] || "file";
}
