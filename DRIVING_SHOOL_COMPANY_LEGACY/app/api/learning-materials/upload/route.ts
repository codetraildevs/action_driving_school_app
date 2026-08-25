// app/api/learning-materials/upload/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { writeFile, mkdir } from "fs/promises";
import { join } from "path";
 ;
import { generateThumbnail } from "@/lib/thum-utils";

import { prisma } from "@/lib/prismaDB";

const uploadMaterialHandler = withPermission(PERMISSIONS.PDF_UPLOAD)(
  async (request: NextRequest) => {
  try {
    const formData = await request.json();
    const file = formData["fileId"] as any;
    const title = formData["title"] as string;
    const description = formData["description"] as string;
    const isPublic = formData["isPublic"] === "true";

    if (!file || !title) {
      return NextResponse.json(
        { error: "File and title are required" },
        { status: 400 }
      );
    }

    if (file && parseInt(file)) {
      const fileTouse = await prisma.file.findFirst({
        where: { id: parseInt(file) },
      });

      const thumbnailPath = fileTouse?.thumbnailUrl;
      const filePath = fileTouse?.filePath;

      const material = await prisma.learningMaterial.create({
        data: {
          title,
          description: description || "",
          filePath: filePath || "",
          fileType: fileTouse?.fileType ||"",
          isPublic,
          thumbnailUrl: thumbnailPath,
        },
      });
      return NextResponse.json(material, { status: 201 });
    }

    return NextResponse.json(
      { error: "File is required" },
      { status: 400 }
    );
  } catch (error) {
    console.error("Error uploading file:", error);
    return NextResponse.json(
      { error: "Failed to upload file" },
      { status: 500 }
    );
  }
  }
);

export const POST = uploadMaterialHandler;
