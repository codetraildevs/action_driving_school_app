import { NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const translateTestHandler = withPermission(PERMISSIONS.TEST_UPDATE)(
  async (
    req: Request,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { langId, title, description, imageUrl } = await req.json();

    if (!langId || !title || !description) {
      return NextResponse.json(
        { success: false, error: "Missing lang or content" },
        { status: 400 }
      );
    }

    const testId = Number(params.id);

    const test = await prisma.test.findUnique({
      where: { id: testId },
    });

    if (!test) {
      return NextResponse.json(
        { success: false, error: "Test not found" },
        { status: 404 }
      );
    }

    const translation = await prisma.$transaction(async (tx) => {
      await prisma.testTranslation.upsert({
        where: {
          testId_languageId: {
            testId,
            languageId: langId,
          },
        },
        create: {
          testId,
          languageId: langId,
          title,
          description,
          imageUrl,
        },
        update: {
          title,
          description,
          imageUrl,
        },
      });
      const translatedTest = await tx.test.findUnique({
        where: { id: testId },
        include: {
          testTranslations: true,
        },
      });
      return translatedTest;
    }, {maxWait:60000, timeout:60000});

    return NextResponse.json(
      { success: true, data: translation },
      { status: 200 }
    );
  } catch (error) {
    console.error("PUT translate error:", error);
    return NextResponse.json(
      { success: false, error: "Failed to update translation" },
      { status: 500 }
    );
  }
  }
);

export const PUT = translateTestHandler;
