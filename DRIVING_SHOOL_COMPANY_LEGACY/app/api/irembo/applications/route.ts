import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/auth/jwt";

import { prisma } from "@/lib/prismaDB";
import { title } from "process";

export async function GET(request: NextRequest) {
  try {
    const authHeader = request.headers.get("authorization");
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Missing or malformed token" },
        { status: 401 }
      );
    }
    const token = authHeader.substring(7);

    const payload = await verifyToken(token);
    if (!payload || !payload.userId) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Invalid or expired token" },
        { status: 401 }
      );
    }

    const userId = payload.userId;

    const applications = await prisma.iremboDrivingLicenseRequest.findMany({
      where: {
        userId,
      },
      orderBy: { createdAt: "desc" },
    });
    const specialApplications = await prisma.iremboSpecialRequest.findMany({
      where: {
        userId,
      },
      orderBy: { createdAt: "desc" },
    });

    if (!applications) {
      return NextResponse.json(
        { success: false, error: "Applications not found" },
        { status: 404 }
      );
    }

    const myApplications = applications
      .map((app) => ({
        id: app.id,
        type: "DRIVING_LICENSE",
        title: `${app.licenseType} ${app.category}`,
        reference: app.referenceId,
        status: app.status,
        date: app.createdAt,
        message: app.message,
        completionPercentage: app.completionPercentage,
        currentStep: app.currentStep,
      }))
      .concat(
        specialApplications.map((app) => ({
          id: app.id,
          type: "SPECIAL",
          title: `Busanza ${app.category}`,
          reference: app.referenceId,
          status: app.status,
          date: app.createdAt,
          message: app.message,
          completionPercentage: app.completionPercentage,
          currentStep: app.currentStep,
        }))
      );

    return NextResponse.json({
      success: true,
      data: myApplications,
      error: null,
    });
  } catch (error) {
    if (error instanceof Error && error.name === "TokenExpiredError") {
      return NextResponse.json(
        { success: false, error: "Session expired" },
        { status: 401 }
      );
    }
    console.error("Profile fetch error:", error);
    return NextResponse.json(
      { success: false, error: "Internal server error" },
      { status: 500 }
    );
  }
}
