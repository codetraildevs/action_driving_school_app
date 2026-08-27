// app/api/admin/ratings/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getRatingHandler = withPermission(PERMISSIONS.USER_READ)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const rating = await prisma.rating.findUnique({
      where: { id: parseInt(params.id) },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
            email: true,
            phoneNumber: true
          }
        },
        pdf: {
          select: {
            id: true,
            title: true,
            author: true,
            description: true
          }
        }
      }
    });

    if (!rating) {
      return NextResponse.json(
        { error: "Rating not found" },
        { status: 404 }
      );
    }

    return NextResponse.json({ data: rating });
  } catch (error) {
    console.error("Error fetching rating:", error);
    return NextResponse.json(
      { error: "Failed to fetch rating" },
      { status: 500 }
    );
  }
  }
);

export const GET = getRatingHandler;

const deleteRatingHandler = withPermission(PERMISSIONS.USER_DELETE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    await prisma.rating.delete({
      where: { id: parseInt(params.id) }
    });

    return NextResponse.json({ 
      success: true,
      message: "Rating deleted successfully" 
    });
  } catch (error) {
    console.error("Error deleting rating:", error);
    return NextResponse.json(
      { error: "Failed to delete rating" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteRatingHandler;