import { NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

// Maximum rows fetched per table. The Android admin console expects a bare
// JSON array (no pagination support), so this bounds the query while keeping
// the response shape the app was built against. The two tables together are
// well under this today (a few hundred rows).
const MAX_ROWS = 500;

// All Irembo requests (driving license + special), newest first.
// Returns a BARE JSON array — the Android admin console parses this as
// List<AdminRequest>. Gated behind the same admin-role check as every other
// /api/admin/* route.
const getAllRequestsHandler = withPermission(PERMISSIONS.USER_READ)(async () => {
  const [driving, special] = await Promise.all([
    prisma.iremboDrivingLicenseRequest.findMany({
      orderBy: { updatedAt: "desc" },
      take: MAX_ROWS,
    }),
    prisma.iremboSpecialRequest.findMany({
      orderBy: { updatedAt: "desc" },
      take: MAX_ROWS,
    }),
  ]);

  const merged = [
    ...driving.map((r) => ({
      id: r.id,
      type: "DRIVING_LICENSE",
      title: [r.category, r.licenseType].filter(Boolean).join(" - "),
      status: r.status,
      message: r.message,
      completionPercentage: r.completionPercentage,
      updatedAt: r.updatedAt,
      nationalId: r.applicantNationalId,
      phoneNumber: r.applicantPhoneNumber,
      names: r.applicantName,
      address: r.address,
      referenceId: r.referenceId,
    })),
    ...special.map((r) => ({
      id: r.id,
      type: "SPECIAL",
      title: [r.category, r.serviceName].filter(Boolean).join(" - "),
      status: r.status,
      message: r.message,
      completionPercentage: r.completionPercentage,
      updatedAt: r.updatedAt,
      nationalId: r.nationalId,
      phoneNumber: r.applicantPhone,
      names: r.applicantName,
      address: null,
      referenceId: r.referenceId,
    })),
  ].sort(
    (a, b) =>
      new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
  );

  return NextResponse.json(merged);
});

export const GET = getAllRequestsHandler;
