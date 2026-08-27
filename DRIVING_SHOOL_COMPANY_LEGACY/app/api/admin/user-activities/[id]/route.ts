// app/api/admin/user-activities/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getActivityHandler = withPermission(PERMISSIONS.USER_READ)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const activity = await prisma.userActivity.findUnique({
      where: { id: parseInt(params.id) },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
            email: true,
            phoneNumber: true,
            lastLogin: true,
            createdAt: true
          }
        }
      }
    });

    if (!activity) {
      return NextResponse.json(
        { error: "Activity not found" },
        { status: 404 }
      );
    }

    return NextResponse.json({ data: activity });
  } catch (error) {
    console.error("Error fetching user activity:", error);
    return NextResponse.json(
      { error: "Failed to fetch user activity" },
      { status: 500 }
    );
  }
  }
);

export const GET = getActivityHandler;

const deleteActivityHandler = withPermission(PERMISSIONS.USER_DELETE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    await prisma.userActivity.delete({
      where: { id: parseInt(params.id) }
    });

    return NextResponse.json({ 
      success: true,
      message: "Activity deleted successfully" 
    });
  } catch (error) {
    console.error("Error deleting user activity:", error);
    return NextResponse.json(
      { error: "Failed to delete user activity" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteActivityHandler;