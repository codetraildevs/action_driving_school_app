// app/api/admin/ratings/route.ts
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
    const pdfId = searchParams.get("pdfId");
    const minRating = searchParams.get("minRating");

    const skip = (page - 1) * limit;

    const where: any = {};

    if (userId) {
      where.userId = parseInt(userId);
    }

    if (pdfId) {
      where.pdfId = parseInt(pdfId);
    }

    if (minRating) {
      where.rating = {
        gte: parseFloat(minRating)
      };
    }

    const [ratings, total] = await Promise.all([
      prisma.rating.findMany({
        where,
        include: {
          user: {
            select: {
              id: true,
              firstName: true,
              lastName: true,
              email: true
            }
          },
          pdf: {
            select: {
              id: true,
              title: true,
              author: true
            }
          }
        },
        orderBy: { createdAt: "desc" },
        skip,
        take: limit,
      }),
      prisma.rating.count({ where })
    ]);

    return NextResponse.json({
      data: ratings,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error("Error fetching ratings:", error);
    return NextResponse.json(
      { error: "Failed to fetch ratings" },
      { status: 500 }
    );
  }
  }
);

export const GET = getRatingsHandler;