// app/api/admin/user-ratings/[id]/route.ts
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
    const rating = await prisma.userRating.findUnique({
      where: { id: parseInt(params.id) },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
            email: true,
            phoneNumber: true,
            createdAt: true
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
    console.error("Error fetching user rating:", error);
    return NextResponse.json(
      { error: "Failed to fetch user rating" },
      { status: 500 }
    );
  }
  }
);

export const GET = getRatingHandler;

const updateRatingHandler = withPermission(PERMISSIONS.USER_UPDATE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { isVerified } = await request.json();

    const rating = await prisma.userRating.update({
      where: { id: parseInt(params.id) },
      data: {
        isVerified: Boolean(isVerified)
      },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
            email: true
          }
        }
      }
    });

    return NextResponse.json({ data: rating });
  } catch (error) {
    console.error("Error updating user rating:", error);
    return NextResponse.json(
      { error: "Failed to update user rating" },
      { status: 500 }
    );
  }
  }
);

export const PUT = updateRatingHandler;

const deleteRatingHandler = withPermission(PERMISSIONS.USER_DELETE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    await prisma.userRating.delete({
      where: { id: parseInt(params.id) }
    });

    return NextResponse.json({ 
      success: true,
      message: "Rating deleted successfully" 
    });
  } catch (error) {
    console.error("Error deleting user rating:", error);
    return NextResponse.json(
      { error: "Failed to delete user rating" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteRatingHandler;