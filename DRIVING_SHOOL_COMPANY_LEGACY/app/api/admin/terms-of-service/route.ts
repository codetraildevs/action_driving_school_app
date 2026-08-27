// app/api/admin/terms-of-service/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getTermsHandler = withPermission(PERMISSIONS.SETTINGS_READ)(
  async () => {
  try {
    const terms = await prisma.termsOfService.findMany({
      orderBy: { createdAt: "desc" },
      include: {
        _count: {
          select: {
            acceptances: true
          }
        }
      }
    });

    return NextResponse.json({ data: terms });
  } catch (error) {
    console.error("Error fetching terms of service:", error);
    return NextResponse.json(
      { error: "Failed to fetch terms of service" },
      { status: 500 }
    );
  }
  }
);

export const GET = getTermsHandler;

const createTermsHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (request: NextRequest) => {
  try {
    const { version, title, content, isActive, appVersion, language } = await request.json();

    if (!version || !title || !content || !appVersion) {
      return NextResponse.json(
        { error: "Version, title, content, and app version are required" },
        { status: 400 }
      );
    }

    // Check if version already exists for this app version and language
    const existingTerms = await prisma.termsOfService.findFirst({
      where: {
        version,
        appVersion,
        language: language || "en"
      }
    });

    if (existingTerms) {
      return NextResponse.json(
        { error: "Terms version already exists for this app version and language" },
        { status: 400 }
      );
    }

    // If setting as active, deactivate all other terms for this language
    if (isActive) {
      await prisma.termsOfService.updateMany({
        where: { 
          isActive: true,
          language: language || "en"
        },
        data: { isActive: false }
      });
    }

    const terms = await prisma.termsOfService.create({
      data: {
        version,
        title,
        content,
        isActive: Boolean(isActive),
        appVersion,
        language: language || "en"
      },
      include: {
        _count: {
          select: {
            acceptances: true
          }
        }
      }
    });

    return NextResponse.json({ data: terms }, { status: 201 });
  } catch (error) {
    console.error("Error creating terms of service:", error);
    return NextResponse.json(
      { error: "Failed to create terms of service" },
      { status: 500 }
    );
  }
  }
);

export const POST = createTermsHandler;