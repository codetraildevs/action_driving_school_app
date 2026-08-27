import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

// Lists EVERY user's subscription requests (names, phones, emails) — this is
// the admin console's User Requests page data source, gated to admins.
const getUserRequestsHandler = withPermission(PERMISSIONS.SUBSCRIPTION_READ)(
  async (request: NextRequest) => {
  try {
    // Paginated + searchable (previously loaded every request row with no
    // limit, which slowed down the User Requests page as data grew).
    const pageParam = parseInt(
      request.nextUrl.searchParams.get("page") || "1",
      10,
    );
    const pageSizeParam = parseInt(
      request.nextUrl.searchParams.get("pageSize") || "50",
      10,
    );
    const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1;
    const pageSize =
      Number.isFinite(pageSizeParam) && pageSizeParam > 0
        ? Math.min(pageSizeParam, 100)
        : 50;
    const search = (request.nextUrl.searchParams.get("search") || "").trim();

    const where: any = {};
    if (search) {
      where.OR = [
        { user: { firstName: { contains: search } } },
        { user: { middleName: { contains: search } } },
        { user: { lastName: { contains: search } } },
        { user: { email: { contains: search } } },
        { user: { phoneNumber: { contains: search } } },
      ];
    }

    const select = {
      id: true,
      userId: true,
      requestedTests: true,
      requestedDays: true,
      requestedExpiresAt: true,
      createdAt: true,
      status: true,
      user: {
        select: {
          id: true,
          firstName: true,
          middleName: true,
          lastName: true,
          phoneNumber: true,
          email: true,
          Pendinglanguage: {
            select: {
              nativeName: true,
            },
          },
        },
      },
    } as const;

    const [subscriptionPlans, total] = await Promise.all([
      prisma.userSubscriptionRequest.findMany({
        select,
        where,
        orderBy: { createdAt: "desc" },
        skip: (page - 1) * pageSize,
        take: pageSize,
      }),
      prisma.userSubscriptionRequest.count({ where }),
    ]);

    return NextResponse.json({
      success: true,
      data: subscriptionPlans,
      total,
      page,
      pageSize,
      totalPages: Math.max(1, Math.ceil(total / pageSize)),
    });
  } catch (error) {
    console.error("Get subscription plans error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
  }
);

export const GET = getUserRequestsHandler;
