import { NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

// Per-user Irembo requests. Contains personal data (names, national IDs,
// phone numbers), so it is gated behind the same admin-role check as every
// other /api/admin/* route.
//
// Two response shapes, selected by the presence of a `page` query param:
//   - no `page`  -> BARE JSON array (the Android admin console parses this as
//                   List<AdminRequest>; capped at 500 rows so it stays bounded)
//   - `page`     -> { success, data, total, page, pageSize, totalPages }
//                   (the web console's Load-more pagination)
const getUserRequestsHandler = withPermission(PERMISSIONS.USER_READ)(
  async (req: Request, { params }: { params: { id: string } }) => {
    const id = Number(params.id);

    const url = new URL(req.url);
    const pageParam = url.searchParams.get("page");
    const hasPagination = pageParam !== null && pageParam !== "";

    const page = Math.max(1, parseInt(pageParam || "1", 10) || 1);
    const pageSize = Math.min(
      parseInt(url.searchParams.get("pageSize") || "50", 10) || 50,
      100,
    );

    // The two tables must be paginated as ONE merged, time-ordered stream.
    // Fetching skip/take per table would give up to 2x pageSize rows per page
    // with no global ordering, so we fetch a merged window large enough to
    // cover the requested page, then sort + slice in JS. Data volumes here are
    // tiny (tens of rows per user), so this stays bounded: at most page*pageSize
    // rows per table, and 500 for the bare list.
    const perTableTake = hasPagination ? page * pageSize : 500;

    const [driving, special, drivingTotal, specialTotal] = await Promise.all([
      prisma.iremboDrivingLicenseRequest.findMany({
        where: { userId: id },
        orderBy: { updatedAt: "desc" },
        take: perTableTake,
      }),
      prisma.iremboSpecialRequest.findMany({
        where: { userId: id },
        include: { user: true },
        orderBy: { updatedAt: "desc" },
        take: perTableTake,
      }),
      prisma.iremboDrivingLicenseRequest.count({ where: { userId: id } }),
      prisma.iremboSpecialRequest.count({ where: { userId: id } }),
    ]);

    const allMerged = [
      ...driving.map((r) => ({
        id: r.id,
        type: "DRIVING_LICENSE",
        title: `${r.licenseType} ${r.category}`,
        status: r.status,
        message: r.message,
        nationalId: r?.applicantNationalId,
        phoneNumber: r?.applicantPhoneNumber,
        completionPercentage: r.completionPercentage,
        address: r?.address,
        updatedAt: r.updatedAt,
        names: r?.applicantName,
        referenceId: r.referenceId,
      })),
      ...special.map((r) => ({
        id: r.id,
        type: "SPECIAL",
        title: `BUSANZA ${r.category}`,
        status: r.status,
        message: r.message,
        completionPercentage: r.completionPercentage,
        updatedAt: r.updatedAt,
        nationalId: r?.nationalId,
        phoneNumber: r?.user.phoneNumber,
        names: r?.applicantName,
        address: null,
        referenceId: r.referenceId,
      })),
    ].sort(
      (a, b) =>
        new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    );

    if (!hasPagination) {
      return NextResponse.json(allMerged);
    }

    const total = drivingTotal + specialTotal;
    const start = (page - 1) * pageSize;
    const data = allMerged.slice(start, start + pageSize);

    return NextResponse.json({
      success: true,
      data,
      total,
      page,
      pageSize,
      totalPages: Math.max(1, Math.ceil(total / pageSize)),
    });
  }
);

export const GET = getUserRequestsHandler;
