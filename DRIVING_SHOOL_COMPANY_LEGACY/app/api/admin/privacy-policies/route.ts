// app/api/admin/privacy-policies/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getPoliciesHandler = withPermission(PERMISSIONS.SETTINGS_READ)(
  async () => {
  try {
    const policies = await prisma.privacyPolicy.findMany({
      orderBy: { createdAt: "desc" },
      include: {
        _count: {
          select: {
            acceptances: true
          }
        }
      }
    });

    return NextResponse.json({ data: policies });
  } catch (error) {
    console.error("Error fetching privacy policies:", error);
    return NextResponse.json(
      { error: "Failed to fetch privacy policies" },
      { status: 500 }
    );
  }
  }
);

export const GET = getPoliciesHandler;

const createPolicyHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
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
    const existingPolicy = await prisma.privacyPolicy.findFirst({
      where: {
        version,
        appVersion,
        language: language || "en"
      }
    });

    if (existingPolicy) {
      return NextResponse.json(
        { error: "Policy version already exists for this app version and language" },
        { status: 400 }
      );
    }

    // If setting as active, deactivate all other policies for this language
    if (isActive) {
      await prisma.privacyPolicy.updateMany({
        where: { 
          isActive: true,
          language: language || "en"
        },
        data: { isActive: false }
      });
    }

    const policy = await prisma.privacyPolicy.create({
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

    return NextResponse.json({ data: policy }, { status: 201 });
  } catch (error) {
    console.error("Error creating privacy policy:", error);
    return NextResponse.json(
      { error: "Failed to create privacy policy" },
      { status: 500 }
    );
  }
  }
);

export const POST = createPolicyHandler;