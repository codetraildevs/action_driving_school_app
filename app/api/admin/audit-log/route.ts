// app/api/admin/audit-log/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";
import { Prisma } from "@/lib/generated/prisma";

/**
 * Advanced audit-log query backing /admin/audit-log.
 *
 * Everything the web admin does that is recorded in user_activities
 * (users, roles, permissions, subscriptions, content, notifications,
 * system settings, data exports…) is surfaced here with full-text search,
 * activity-type / user / date-range filters, pagination and summary stats.
 *
 * Query params:
 *   page         1-based page (default 1)
 *   pageSize     rows per page, clamped 5..100 (default 25)
 *   search       matches description, activity type, user first/last name, email
 *   activityType exact activity-type match
 *   userId       numeric user id
 *   from, to     YYYY-MM-DD inclusive bounds (server local time)
 */
const auditLogHandler = withPermission(PERMISSIONS.ANALYTICS_VIEW)(
  async (request: NextRequest) => {
    try {
      const { searchParams } = new URL(request.url);

      const page = Math.max(1, parseInt(searchParams.get("page") || "1", 10) || 1);
      const pageSize = Math.min(
        100,
        Math.max(5, parseInt(searchParams.get("pageSize") || "25", 10) || 25),
      );
      const search = (searchParams.get("search") || "").trim();
      const activityType = (searchParams.get("activityType") || "").trim();
      const userIdRaw = (searchParams.get("userId") || "").trim();
      const from = (searchParams.get("from") || "").trim();
      const to = (searchParams.get("to") || "").trim();

      const filters: Prisma.UserActivityWhereInput[] = [];

      if (search) {
        filters.push({
          OR: [
            { description: { contains: search } },
            { activityType: { contains: search } },
            { user: { is: { firstName: { contains: search } } } },
            { user: { is: { lastName: { contains: search } } } },
            { user: { is: { email: { contains: search } } } },
          ],
        });
      }

      if (activityType) {
        filters.push({ activityType });
      }

      const userId = parseInt(userIdRaw, 10);
      if (userIdRaw && !isNaN(userId)) {
        filters.push({ userId });
      }

      if (from) {
        const fromDate = new Date(`${from}T00:00:00`);
        if (!isNaN(fromDate.getTime())) filters.push({ createdAt: { gte: fromDate } });
      }
      if (to) {
        const toDate = new Date(`${to}T23:59:59.999`);
        if (!isNaN(toDate.getTime())) filters.push({ createdAt: { lte: toDate } });
      }

      const where: Prisma.UserActivityWhereInput =
        filters.length > 0 ? { AND: filters } : {};

      // Start of today (server local time) for the "events today" pulse.
      const todayStart = new Date();
      todayStart.setHours(0, 0, 0, 0);

      const [entries, total, types, today, uniqueUserGroups] = await Promise.all([
        prisma.userActivity.findMany({
          where,
          skip: (page - 1) * pageSize,
          take: pageSize,
          orderBy: { createdAt: "desc" },
          include: {
            user: {
              select: {
                id: true,
                firstName: true,
                lastName: true,
                email: true,
                role: { select: { roleName: true } },
              },
            },
          },
        }),
        prisma.userActivity.count({ where }),
        // Global catalogue (not filtered) so the type filter always lists
        // every option even while a search hides some of them.
        prisma.userActivity.groupBy({
          by: ["activityType"],
          _count: { _all: true },
          orderBy: { _count: { activityType: "desc" } },
          take: 100,
        }),
        prisma.userActivity.count({ where: { createdAt: { gte: todayStart } } }),
        prisma.userActivity.groupBy({ by: ["userId"], where }),
      ]);

      return NextResponse.json({
        success: true,
        data: entries,
        pagination: {
          page,
          pageSize,
          total,
          pages: Math.ceil(total / pageSize) || 1,
        },
        meta: {
          types: types.map((t) => ({
            activityType: t.activityType,
            count: t._count._all,
          })),
          stats: {
            total,
            today,
            uniqueUsers: uniqueUserGroups.length,
            topAction: types[0]?.activityType ?? null,
          },
        },
      });
    } catch (error) {
      console.error("Error fetching audit log:", error);
      return NextResponse.json(
        { error: "Failed to fetch audit log" },
        { status: 500 },
      );
    }
  },
);

export const GET = auditLogHandler;
