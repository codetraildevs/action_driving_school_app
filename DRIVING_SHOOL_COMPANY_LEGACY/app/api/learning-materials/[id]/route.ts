// app/api/learning-materials/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";

import { prisma } from "@/lib/prismaDB";

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const material = await prisma.learningMaterial.findUnique({
      where: { id: parseInt(params.id) },
      include: {
        userLearningMaterials: {
          include: {
            user: {
              select: { firstName: true, lastName: true, email: true },
            },
          },
        },
      },
    });

    if (!material) {
      return NextResponse.json(
        { error: "Learning material not found" },
        { status: 404 }
      );
    }

    return NextResponse.json(material);
  } catch (error) {
    console.error("Error fetching learning material:", error);
    return NextResponse.json(
      { error: "Failed to fetch learning material" },
      { status: 500 }
    );
  }
}

// app/api/learning-materials/route.ts
const createMaterialHandler = withPermission(PERMISSIONS.PDF_UPLOAD)(
  async (request: NextRequest) => {
  try {
    const body = await request.json();

    const {
      title,
      description,
      filePath,
      fileType,
      thumbnailUrl,
      isPublic = true,
    } = body;

    if (!title || !filePath || !fileType) {
      return NextResponse.json(
        { error: "Title, filePath, and fileType are required" },
        { status: 400 }
      );
    }

    const material = await prisma.learningMaterial.create({
      data: {
        title,
        description,
        filePath,
        fileType,
        thumbnailUrl,
        isPublic,
      },
      include: {
        userLearningMaterials: true,
      },
    });

    return NextResponse.json(material, { status: 201 });
  } catch (error) {
    console.error("Error creating learning material:", error);
    return NextResponse.json(
      { error: "Failed to create learning material" },
      { status: 500 }
    );
  }
  }
);

export const POST = createMaterialHandler;

// app/api/learning-materials/[id]/route.ts
const updateMaterialHandler = withPermission(PERMISSIONS.PDF_UPDATE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const body = await request.json();

    const {
      file,
      title,
      description,
      filePath,
      fileType,
      thumbnailUrl,
      isPublic,
    } = body;

    const existingMaterial = await prisma.learningMaterial.findUnique({
      where: { id: parseInt(params.id) },
    });

    if (!existingMaterial) {
      return NextResponse.json(
        { error: "Learning material not found" },
        { status: 404 }
      );
    }

    let thumbnailPath = thumbnailUrl;
    let MyFilePath = filePath;
    let myFileType = fileType;
    if (file && parseInt(file)) {
      const fileTouse = await prisma.file.findFirst({
        where: { id: parseInt(file) },
      });

      thumbnailPath = fileTouse?.thumbnailUrl;
      MyFilePath = fileTouse?.filePath;
      myFileType = fileTouse?.fileType;
    }
    const updatedMaterial = await prisma.learningMaterial.update({
      where: { id: parseInt(params.id) },
      data: {
        ...(title && { title }),
        ...(description && { description }),
        ...(MyFilePath && { filePath: MyFilePath }),
        ...(myFileType && { fileType: myFileType }),
        ...(thumbnailPath !== undefined && { thumbnailUrl: thumbnailPath }),
        ...(isPublic !== undefined && { isPublic }),
      },
      include: {
        userLearningMaterials: {
          include: {
            user: {
              select: { firstName: true, lastName: true, email: true },
            },
          },
        },
      },
    });

    return NextResponse.json(updatedMaterial);
  } catch (error) {
    console.error("Error updating learning material:", error);
    return NextResponse.json(
      { error: "Failed to update learning material" },
      { status: 500 }
    );
  }
  }
);

export const PUT = updateMaterialHandler;

// app/api/learning-materials/[id]/route.ts
const deleteMaterialHandler = withPermission(PERMISSIONS.PDF_DELETE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const existingMaterial = await prisma.learningMaterial.findUnique({
      where: { id: parseInt(params.id) },
    });

    if (!existingMaterial) {
      return NextResponse.json(
        { error: "Learning material not found" },
        { status: 404 }
      );
    }

    // Delete associated user learning materials first
    await prisma.userLearningMaterial.deleteMany({
      where: { learningMaterialId: parseInt(params.id) },
    });

    // Then delete the learning material
    await prisma.learningMaterial.delete({
      where: { id: parseInt(params.id) },
    });

    return NextResponse.json({
      message: "Learning material deleted successfully",
    });
  } catch (error) {
    console.error("Error deleting learning material:", error);
    return NextResponse.json(
      { error: "Failed to delete learning material" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteMaterialHandler;
