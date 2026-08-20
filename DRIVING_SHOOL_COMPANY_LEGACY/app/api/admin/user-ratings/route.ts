// app/api/admin/user-ratings/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getRatingsHandler = withPermission(PERMISSIONS.USER_READ)(
  async (request: NextRequest) => {
  try {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get("page") || "1");
    const limit = parseInt(searchParams.get("limit") || "20");
    const userId = searchParams.get("userId");
    const platform = searchParams.get("platform");
    const minRating = searchParams.get("minRating");
    const isVerified = searchParams.get("isVerified");

    const skip = (page - 1) * limit;

    const where: any = {};

    if (userId) {
      where.userId = parseInt(userId);
    }

    if (platform) {
      where.platform = platform;
    }

    if (minRating) {
      where.rating = {
        gte: parseFloat(minRating)
      };
    }

    if (isVerified) {
      where.isVerified = isVerified === "true";
    }

    const [ratings, total] = await Promise.all([
      prisma.userRating.findMany({
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
      prisma.userRating.count({ where })
    ]);

    // Calculate average rating and stats
    const stats = await prisma.userRating.aggregate({
      where,
      _avg: {
        rating: true
      },
      _count: {
        _all: true
      },
      _min: {
        rating: true
      },
      _max: {
        rating: true
      }
    });

    return NextResponse.json({
      data: ratings,
      stats: {
        average: stats._avg.rating || 0,
        total: stats._count._all,
        min: stats._min.rating || 0,
        max: stats._max.rating || 0
      },
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error("Error fetching user ratings:", error);
    return NextResponse.json(
      { error: "Failed to fetch user ratings" },
      { status: 500 }
    );
  }
  }
);

export const GET = getRatingsHandler;

const deleteRatingsHandler = withPermission(PERMISSIONS.USER_DELETE)(
  async (request: NextRequest) => {
  try {
    const { ids } = await request.json();

    if (!ids || !Array.isArray(ids)) {
      return NextResponse.json(
        { error: "Rating IDs array is required" },
        { status: 400 }
      );
    }

    await prisma.userRating.deleteMany({
      where: {
        id: { in: ids }
      }
    });

    return NextResponse.json({ 
      success: true,
      message: `${ids.length} ratings deleted successfully` 
    });
  } catch (error) {
    console.error("Error deleting user ratings:", error);
    return NextResponse.json(
      { error: "Failed to delete user ratings" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteRatingsHandler;