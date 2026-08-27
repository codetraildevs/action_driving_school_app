// app/api/admin/terms-of-service/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getTermsHandler = withPermission(PERMISSIONS.SETTINGS_READ)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const terms = await prisma.termsOfService.findUnique({
      where: { id: parseInt(params.id) },
      include: {
        _count: {
          select: {
            acceptances: true
          }
        }
      }
    });

    if (!terms) {
      return NextResponse.json(
        { error: "Terms of service not found" },
        { status: 404 }
      );
    }

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

const updateTermsHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { version, title, content, isActive, appVersion, language } = await request.json();

    if (!version || !title || !content || !appVersion) {
      return NextResponse.json(
        { error: "Version, title, content, and app version are required" },
        { status: 400 }
      );
    }

    // If setting as active, deactivate all other terms for this language
    if (isActive) {
      await prisma.termsOfService.updateMany({
        where: { 
          isActive: true,
          language: language || "en",
          id: { not: parseInt(params.id) }
        },
        data: { isActive: false }
      });
    }

    const terms = await prisma.termsOfService.update({
      where: { id: parseInt(params.id) },
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

    return NextResponse.json({ data: terms });
  } catch (error) {
    console.error("Error updating terms of service:", error);
    return NextResponse.json(
      { error: "Failed to update terms of service" },
      { status: 500 }
    );
  }
  }
);

export const PUT = updateTermsHandler;

const deleteTermsHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    // Check if there are acceptances
    const acceptances = await prisma.termsOfServiceAcceptance.count({
      where: { termsOfServiceId: parseInt(params.id) }
    });

    if (acceptances > 0) {
      return NextResponse.json(
        { error: "Cannot delete terms with user acceptances" },
        { status: 400 }
      );
    }

    await prisma.termsOfService.delete({
      where: { id: parseInt(params.id) }
    });

    return NextResponse.json({ 
      success: true,
      message: "Terms of service deleted successfully" 
    });
  } catch (error) {
    console.error("Error deleting terms of service:", error);
    return NextResponse.json(
      { error: "Failed to delete terms of service" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteTermsHandler;

// Set terms as active
const setActiveTermsHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { isActive } = await request.json();

    if (isActive) {
      // Deactivate all other terms for the same language
      const terms = await prisma.termsOfService.findUnique({
        where: { id: parseInt(params.id) }
      });

      if (terms) {
        await prisma.termsOfService.updateMany({
          where: { 
            isActive: true,
            language: terms.language,
            id: { not: parseInt(params.id) }
          },
          data: { isActive: false }
        });
      }
    }

    const updatedTerms = await prisma.termsOfService.update({
      where: { id: parseInt(params.id) },
      data: { isActive: Boolean(isActive) },
      include: {
        _count: {
          select: {
            acceptances: true
          }
        }
      }
    });

    return NextResponse.json({ data: updatedTerms });
  } catch (error) {
    console.error("Error updating terms of service status:", error);
    return NextResponse.json(
      { error: "Failed to update terms of service status" },
      { status: 500 }
    );
  }
  }
);

export const PATCH = setActiveTermsHandler;