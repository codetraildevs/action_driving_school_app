import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/auth/jwt";
import { prisma } from "@/lib/prismaDB";
import {
  UserRequestStatus,
  UserTestAccess,
  UserTestAccessStatus,
} from "@/lib/generated/prisma";

export async function GET(request: NextRequest) {
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

    const userSubscription = await prisma.userSubscription.findUnique({
      where: { userId: payload.userId },
      include: {
        subscriptionPlan: {
          include: { permissions: true },
        },
      },
    });

    return NextResponse.json({ success: true, data: userSubscription });
  } catch (error) {
    console.error("Get user subscription error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  let transactionResult;
  
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
    const { searchParams } = new URL(request.url);
    const testNumber = searchParams.get("testNumber");
    const numberOfDays = searchParams.get("days");
    const currentUserLanguageId= searchParams.get("currentLanguageId")

    if (!testNumber || !numberOfDays) {
      return NextResponse.json(
        { success: false, error: "Missing required query parameters" },
        { status: 400 }
      );
    }

    const testAccessNumber = parseInt(testNumber);
    const days = parseInt(numberOfDays);

    if (isNaN(testAccessNumber) || isNaN(days)) {
      return NextResponse.json(
        { success: false, error: "Invalid numeric parameters" },
        { status: 400 }
      );
    }

  
    const test = await prisma.test.findUnique({
      where: { testNumber: testAccessNumber },
      select: { id: true }  
    });

    if (!test) {
      return NextResponse.json(
        { success: false, error: "Test number not found" },
        { status: 404 }
      );
    }

    // Prevent duplicate requests: a user may have only ONE pending request
    // at a time (it stays PENDING until an admin accepts or rejects it).
    // Without this, every tap on a locked test stacks another request row.
    const pendingRequest = await prisma.userSubscriptionRequest.findFirst({
      where: { userId, status: UserRequestStatus.PENDING },
      select: { id: true, requestedTests: true, createdAt: true },
      orderBy: { createdAt: "desc" },
    });

    if (pendingRequest) {
      return NextResponse.json(
        {
          success: false,
          error:
            "You already have a pending request for test access. Please wait for the admin to approve or reject it before requesting again.",
        },
        { status: 409 }
      );
    }
 
    const existingAccess = await prisma.userTestAccess.findUnique({
      where: { userId },
      select: { maxTest: true, expiresAt: true }
    });

    if (existingAccess?.maxTest === testAccessNumber && existingAccess.expiresAt > new Date()) {
      return NextResponse.json(
        {
          success: false,
          error: `You already have access to the ${testAccessNumber} test plan.`,
        },
        { status: 400 }
      );
    }

  
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + days);

    
    transactionResult = await prisma.$transaction(async (tx) => {
      let userTestAccess = await tx.userTestAccess.findUnique({where:{userId}})
      if(!userTestAccess){
        userTestAccess=await tx.userTestAccess.create({data:{ userId,
          maxTest: testAccessNumber,
          expiresAt,
          status: UserTestAccessStatus.PENDING
        }})
       }
       let userLanguage= await tx.language.findFirst({where:{id:Number(currentUserLanguageId)}})
       if(!userLanguage){
        userLanguage= await tx.language.findFirst({where:{languageCode:"rw"}})

       }
       await tx.user.update({where:{id: userId}, data:{pendingLanguageId:userLanguage?.id}})
 
      const subscriptionRequest = await tx.userSubscriptionRequest.create({
        data: {
          userId,
          requestedTests: testAccessNumber,
          requestedExpiresAt:expiresAt,
          requestedDays:days,
          userTestAccessId: userTestAccess.id,
          
        },
      });

      return { userTestAccess, subscriptionRequest };
    });

    return NextResponse.json(
      {
        success: true,
        message: `Your request to access tests up to ${testAccessNumber} has been accepted successfully`,
        data: null,
      },
      { status: 201 }
    );

  } catch (error: any) {
    console.error("Subscribe error:", error);
 
    if (error.code === "P2002") {
      return NextResponse.json(
        {
          success: false,
          error: "Test access already exists for this user",
        },
        { status: 409 }
      );
    }

 
    if (error.message?.includes("Transaction failed")) {
      return NextResponse.json(
        {
          success: false,
          error: "Failed to process subscription request",
        },
        { status: 500 }
      );
    }

    return NextResponse.json(
      {
        success: false,
        error: "Internal server error",
      },
      { status: 500 }
    );
  }
}