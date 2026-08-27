import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prismaDB";

interface RouteParams {
  params: {
    applicationNumber: string;
  };
}

export async function GET(request: NextRequest, { params }: RouteParams) {
  try {
    const { applicationNumber } = params;

    let requestData: any = await prisma.iremboDrivingLicenseRequest.findUnique({
      where: {
        referenceId: applicationNumber,
      },
      include: {
        user: true,
        },
      });
    

    if (!requestData) {
      requestData = await prisma.iremboSpecialRequest.findUnique({
        where: {
          referenceId: applicationNumber,
        },
        include: {
          user: true
        },
      });
    }

    if (!requestData) {
      const response = {
        success: false,
        error: "Driving license request not found",
      };
      return NextResponse.json(response, { status: 404 });
    }

    // Special requests have no licenseType/applicationType; label them clearly.
    const isDriving = !!requestData.licenseType;
    const title = isDriving
      ? `${requestData.applicationType}-${requestData.category}-${requestData.licenseType}`
      : `Busanza ${requestData.category}`;

    const response = {
      success: true,
      data: {
        id: requestData.id,
        title,
        reference: requestData.referenceId,
        status: requestData.status,
        date: requestData.createdAt,
        message: requestData.message,
        completionPercentage: requestData.completionPercentage,
        currentStep: requestData.currentStep,
        user: requestData.user,
        type: isDriving ? "DRIVING_LICENSE" : "SPECIAL",
        updatedAt: requestData.updatedAt,
        nationalId: requestData?.nationalId,
        phoneNumber: requestData?.user.phoneNumber,
        address: null,
        referenceId: requestData.referenceId,
      },
    };

    return NextResponse.json(response);
  } catch (error) {
    console.error("GET /api/irembo/driving/[id] error:", error);
    const response = {
      success: false,
      error: "Failed to fetch driving license request",
    };
    return NextResponse.json(response, { status: 500 });
  }
}
