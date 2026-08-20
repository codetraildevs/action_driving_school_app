// app/api/admin/privacy-policies/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getPolicyHandler = withPermission(PERMISSIONS.SETTINGS_READ)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const policy = await prisma.privacyPolicy.findUnique({
      where: { id: parseInt(params.id) },
      include: {
        _count: {
          select: {
            acceptances: true
          }
        }
      }
    });

    if (!policy) {
      return NextResponse.json(
        { error: "Privacy policy not found" },
        { status: 404 }
      );
    }

    return NextResponse.json({ data: policy });
  } catch (error) {
    console.error("Error fetching privacy policy:", error);
    return NextResponse.json(
      { error: "Failed to fetch privacy policy" },
      { status: 500 }
    );
  }
  }
);

export const GET = getPolicyHandler;

const updatePolicyHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
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

    // If setting as active, deactivate all other policies for this language
    if (isActive) {
      await prisma.privacyPolicy.updateMany({
        where: { 
          isActive: true,
          language: language || "en",
          id: { not: parseInt(params.id) }
        },
        data: { isActive: false }
      });
    }

    const policy = await prisma.privacyPolicy.update({
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

    return NextResponse.json({ data: policy });
  } catch (error) {
    console.error("Error updating privacy policy:", error);
    return NextResponse.json(
      { error: "Failed to update privacy policy" },
      { status: 500 }
    );
  }
  }
);

export const PUT = updatePolicyHandler;

const deletePolicyHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    // Check if there are acceptances
    const acceptances = await prisma.privacyPolicyAcceptance.count({
      where: { privacyPolicyId: parseInt(params.id) }
    });

    if (acceptances > 0) {
      return NextResponse.json(
        { error: "Cannot delete policy with user acceptances" },
        { status: 400 }
      );
    }

    await prisma.privacyPolicy.delete({
      where: { id: parseInt(params.id) }
    });

    return NextResponse.json({ 
      success: true,
      message: "Privacy policy deleted successfully" 
    });
  } catch (error) {
    console.error("Error deleting privacy policy:", error);
    return NextResponse.json(
      { error: "Failed to delete privacy policy" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deletePolicyHandler;

// Set policy as active
const setActivePolicyHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { isActive } = await request.json();

    if (isActive) {
      // Deactivate all other policies for the same language
      const policy = await prisma.privacyPolicy.findUnique({
        where: { id: parseInt(params.id) }
      });

      if (policy) {
        await prisma.privacyPolicy.updateMany({
          where: { 
            isActive: true,
            language: policy.language,
            id: { not: parseInt(params.id) }
          },
          data: { isActive: false }
        });
      }
    }

    const updatedPolicy = await prisma.privacyPolicy.update({
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

    return NextResponse.json({ data: updatedPolicy });
  } catch (error) {
    console.error("Error updating privacy policy status:", error);
    return NextResponse.json(
      { error: "Failed to update privacy policy status" },
      { status: 500 }
    );
  }
  }
);

export const PATCH = setActivePolicyHandler;