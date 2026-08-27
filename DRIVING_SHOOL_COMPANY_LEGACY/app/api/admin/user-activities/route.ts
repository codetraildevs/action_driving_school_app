// app/api/admin/user-activities/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getActivitiesHandler = withPermission(PERMISSIONS.USER_READ)(
  async (request: NextRequest) => {
  try {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get("page") || "1");
    const limit = parseInt(searchParams.get("limit") || "20");
    const userId = searchParams.get("userId");
    const activityType = searchParams.get("type");
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");

    const skip = (page - 1) * limit;

    const where: any = {};

    if (userId) {
      where.userId = parseInt(userId);
    }

    if (activityType) {
      where.activityType = {
        contains: activityType,
        mode: 'insensitive'
      };
    }

    if (startDate || endDate) {
      where.createdAt = {};
      if (startDate) where.createdAt.gte = new Date(startDate);
      if (endDate) where.createdAt.lte = new Date(endDate);
    }

    const [activities, total] = await Promise.all([
      prisma.userActivity.findMany({
        where,
        include: {
          user: {
            select: {
              id: true,
              firstName: true,
              lastName: true,
              email: true,
              phoneNumber: true
            }
          }
        },
        orderBy: { createdAt: "desc" },
        skip,
        take: limit,
      }),
      prisma.userActivity.count({ where })
    ]);

    return NextResponse.json({
      data: activities,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error("Error fetching user activities:", error);
    return NextResponse.json(
      { error: "Failed to fetch user activities" },
      { status: 500 }
    );
  }
  }
);

export const GET = getActivitiesHandler;

const deleteActivitiesHandler = withPermission(PERMISSIONS.USER_DELETE)(
  async (request: NextRequest) => {
  try {
    const { ids } = await request.json();

    if (!ids || !Array.isArray(ids)) {
      return NextResponse.json(
        { error: "Activity IDs array is required" },
        { status: 400 }
      );
    }

    await prisma.userActivity.deleteMany({
      where: {
        id: { in: ids }
      }
    });

    return NextResponse.json({ 
      success: true,
      message: `${ids.length} activities deleted successfully` 
    });
  } catch (error) {
    console.error("Error deleting user activities:", error);
    return NextResponse.json(
      { error: "Failed to delete user activities" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteActivitiesHandler;