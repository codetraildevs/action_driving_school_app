import { NextRequest, NextResponse } from "next/server";

import { prisma } from "@/lib/prismaDB";
import { verifyToken } from "@/lib/auth/jwt";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get("page") || "1");
    const limit = parseInt(searchParams.get("limit") || "10");
    const search = searchParams.get("search") || "";
    const status = searchParams.get("status");
    const category = searchParams.get("category");
    const userId = searchParams.get("userId");

    const skip = (page - 1) * limit;

    // Build where clause
    const where: any = {};

    if (userId) {
      where.userId = parseInt(userId);
    }

    if (status) {
      where.status = status;
    }

    if (category) {
      where.category = category;
    }

    if (search) {
      where.OR = [
        { applicantName: { contains: search, mode: "insensitive" } },
        { applicantNationalId: { contains: search, mode: "insensitive" } },
        { applicantPhoneNumber: { contains: search, mode: "insensitive" } },
        { requestId: { contains: search, mode: "insensitive" } },
      ];
    }

    // Get total count
    const total = await prisma.iremboDrivingLicenseRequest.count({ where });

    // Get paginated results
    const requests = await prisma.iremboDrivingLicenseRequest.findMany({
      where,
      skip,
      take: limit,
      orderBy: { createdAt: "desc" },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
            phoneNumber: true,
            email: true,
          },
        },
      },
    });

    const response = {
      success: true,
      data: {
        requests,
        pagination: {
          page,
          limit,
          total,
          pages: Math.ceil(total / limit),
        },
      },
    };

    return NextResponse.json(response);
  } catch (error) {
    console.error("GET /api/irembo/driving error:", error);
    const response = {
      success: false,
      error: "Failed to fetch driving license requests",
    };
    return NextResponse.json(response, { status: 500 });
  }
}

// POST - Create new driving license request
export async function POST(request: NextRequest) {
  try {
    const authHeader = request.headers.get("authorization");
    if (!authHeader?.startsWith("Bearer ")) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Missing or malformed token" },
        { status: 401 }
      );
    }

    const token = authHeader.substring(7);
    const payload = await verifyToken(token);

    if (!payload?.userId) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Invalid or expired token" },
        { status: 401 }
      );
    }
    const userId = payload.userId;
    const {
      category,
      licenseType,
      applicationType,
      applicantName,
      applicantPhoneNumber,
      applicantNationalId,
      address,
    } = await request.json();

    // Prevent duplicate active requests: a user may have only one
    // in-progress (PENDING/PROCESSING/ACTION) license request at a time.
    const activeRequest = await prisma.iremboDrivingLicenseRequest.findFirst({
      where: {
        userId,
        status: { in: ["PENDING", "PROCESSING", "ACTION"] },
      },
      select: { referenceId: true },
    });

    if (activeRequest) {
      return NextResponse.json(
        {
          success: false,
          error:
            "You already have a pending driving license request. Please wait for it to be processed before submitting a new one.",
          existingReferenceId: activeRequest.referenceId,
        },
        { status: 409 }
      );
    }

    // Generate unique request ID
    const timestamp = Date.now().toString().slice(-6);
    const random = Math.random().toString(36).substring(2, 6).toUpperCase();

    // Create request
    const newRequest = await prisma.iremboDrivingLicenseRequest.create({
      data: {
        category,
        licenseType,
        applicationType,
        applicantName,
        applicantPhoneNumber,
        applicantNationalId,
        address,
        userId,
      },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
          },
        },
      },
    });

   await prisma.iremboDrivingLicenseRequest.update({where:{id:newRequest.id}, data:{referenceId:`IREMBO_${Date.now()}${newRequest.id}`}})

    const response = {
      success: true,
       data: {
        amount:500,
        currency:"RWF",
        itemName:"Irembo Service Request",
        recipient:"0791105800",
        transactionFee:200
      },
      message: "Driving license request created successfully",
    };

    return NextResponse.json(response, { status: 201 });
  } catch (error) {
    console.error("POST /api/irembo/driving error:", error);
    const response = {
      success: false,
      error: "Failed to create driving license request",
    };
    return NextResponse.json(response, { status: 500 });
  }
}
