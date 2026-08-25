import { NextRequest, NextResponse } from "next/server";

import { verifyToken } from "@/lib/auth/jwt";
import { isAdminRoleName } from "@/lib/auth/roles";
import { prisma } from "@/lib/prismaDB";
import { RequestStatus } from "@/lib/generated/prisma";
import { Prisma } from "@/lib/generated/prisma";

const userListSelect = {
  id: true,
  firstName: true,
  middleName: true,
  lastName: true,
  email: true,
  phoneNumber: true,
  isActive: true,
  createdAt: true,
  role: {
    select: {
      id: true,
      roleName: true,
    },
  },
  language: {
    select: {
      id: true,
      nativeName: true,
    },
  },
  userTestAccess: {
    select: {
      maxTest: true,
      expiresAt: true,
      status: true,
    },
  },
  devices: {
    take: 1,
    select: {
      physicalAddress: true,
      name: true,
      manufacturer: true,
    },
  },
} as const;

// GET all users (for listing, server-side paginated)
export async function GET(request: NextRequest) {
  try {
    const authHeader = request.headers.get("authorization");
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Missing or malformed token" },
        { status: 401 },
      );
    }
    const token = authHeader.substring(7);

    const payload = await verifyToken(token);
    if (!payload || !payload.userId) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Invalid or expired token" },
        { status: 401 },
      );
    }

    // Check if admin
    const admin = await prisma.user.findUnique({
      where: { id: payload.userId },
      include: { role: true },
    });

    if (!isAdminRoleName(admin?.role.roleName)) {
      return NextResponse.json({ error: "Forbidden" }, { status: 403 });
    }
    // check if request has from arg
    const from = request.nextUrl.searchParams.get("from");
    if (from === "irembo") {
      // Paginated list of users who have submitted Irembo requests.
      // Pending counts are computed in SQL via filtered _count aggregates so
      // we never fetch every request row into memory (previously this loaded
      // ALL users + their pending requests with no pagination -> very slow).
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

      // Only users who have submitted at least one Irembo request
      const where: Prisma.UserWhereInput = {
        OR: [
          { iremboDrivingLicenseRequests: { some: {} } },
          { iremboSpecialRequests: { some: {} } },
        ],
      };

      if (search) {
        where.AND = {
          OR: [
            { firstName: { contains: search } },
            { middleName: { contains: search } },
            { lastName: { contains: search } },
            { email: { contains: search } },
            { phoneNumber: { contains: search } },
          ],
        };
      }

      const [users, total] = await Promise.all([
        prisma.user.findMany({
          where,
          select: {
            id: true,
            firstName: true,
            middleName: true,
            lastName: true,
            email: true,
            phoneNumber: true,
            isActive: true,
            createdAt: true,
            role: {
              select: {
                id: true,
                roleName: true,
              },
            },
            language: {
              select: {
                id: true,
                nativeName: true,
              },
            },
            userTestAccess: {
              select: {
                maxTest: true,
                expiresAt: true,
                status: true,
              },
            },
            devices: {
              take: 1,
              select: {
                physicalAddress: true,
                name: true,
                manufacturer: true,
              },
            },
            _count: {
              select: {
                iremboDrivingLicenseRequests: {
                  where: { status: RequestStatus.PENDING },
                },
                iremboSpecialRequests: {
                  where: { status: RequestStatus.PENDING },
                },
              },
            },
          },
          orderBy: {
            createdAt: "desc",
          },
          skip: (page - 1) * pageSize,
          take: pageSize,
        }),
        prisma.user.count({ where }),
      ]);

      const usersResponse = users.map((user) => {
        const {
          _count,
          ...userWithoutCount
        } = user;

        return {
          ...userWithoutCount,
          numberOfPendingIremboDrivingLicenseRequests:
            _count.iremboDrivingLicenseRequests,
          numberOfPendingIremboSpecialRequests:
            _count.iremboSpecialRequests,
        };
      });

      return NextResponse.json({
        success: true,
        data: usersResponse,
        total,
        page,
        pageSize,
        totalPages: Math.max(1, Math.ceil(total / pageSize)),
      });
    }

    // Pagination + filters (from the Users page)
    const pageParam = parseInt(
      request.nextUrl.searchParams.get("page") || "1",
      10,
    );
    const pageSizeParam = parseInt(
      request.nextUrl.searchParams.get("pageSize") || "25",
      10,
    );
    const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1;
    const pageSize =
      Number.isFinite(pageSizeParam) && pageSizeParam > 0
        ? Math.min(pageSizeParam, 100)
        : 25;
    const search = (request.nextUrl.searchParams.get("search") || "").trim();
    const tab = request.nextUrl.searchParams.get("tab") || "all";

    const where: Prisma.UserWhereInput = {};

    if (search) {
      where.OR = [
        { firstName: { contains: search } },
        { middleName: { contains: search } },
        { lastName: { contains: search } },
        { email: { contains: search } },
        { phoneNumber: { contains: search } },
      ];
    }

    if (tab === "active") {
      where.isActive = true;
    } else if (tab === "inactive") {
      where.isActive = false;
    } else if (tab === "new" || tab === "old") {
      const oneWeekAgo = new Date();
      oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);
      if (tab === "new") {
        where.createdAt = { gt: oneWeekAgo };
      } else {
        where.createdAt = { lte: oneWeekAgo };
      }
    }

    const [users, total] = await Promise.all([
      prisma.user.findMany({
        where,
        select: userListSelect,
        orderBy: {
          createdAt: "desc",
        },
        skip: (page - 1) * pageSize,
        take: pageSize,
      }),
      prisma.user.count({ where }),
    ]);

    return NextResponse.json({
      success: true,
      data: users,
      total,
      page,
      pageSize,
      totalPages: Math.max(1, Math.ceil(total / pageSize)),
    });
  } catch (error) {
    console.error("Fetch users error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 },
    );
  }
}
