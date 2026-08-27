import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/auth/jwt";
import { prisma } from "@/lib/prismaDB";
import { UserTestAccessStatus } from "@/lib/generated/prisma";

export async function PUT(request: NextRequest) {
  try {
    // 1. Authentication
    const authHeader = request.headers.get("authorization");
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
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

    // 2. Get Query Params (Language ID)
    const { searchParams } = new URL(request.url);
    const currentUserLanguageId = searchParams.get("languageId");

    // 3. Execute Transaction
    const transactionResult = await prisma.$transaction(async (tx) => {
      
      // A. Fetch the CURRENT existing access to get details for the request
      const currentAccess = await tx.userTestAccess.findUnique({
        where: { userId },
      });

      if (!currentAccess) {
        throw new Error("NO_SUBSCRIPTION_FOUND");
      }

      // // // B. Update the UserTestAccess status to PENDING
      // const updatedSubscription = await tx.userTestAccess.update({
      //   where: { userId },
      //   data: {
      //     status: UserTestAccessStatus.PENDING,
      //   },
      // });

      // C. Update User Language (if provided)
      if (currentUserLanguageId) {
        const userLanguage = await tx.language.findFirst({
          where: { id: Number(currentUserLanguageId) },
        });

        if (userLanguage) {
          await tx.user.update({
            where: { id: userId },
            data: { pendingLanguageId: userLanguage.id },
          });
        }
      }

      // D. Calculate Remaining Days for the Request
      // We calculate how much time is left so the request reflects the saved value
      const now = new Date();
      const expiresAt = new Date(currentAccess.expiresAt);
      const diffTime = expiresAt.getTime() - now.getTime();
      // Convert ms to days, ensuring at least 0 if expired
      const remainingDays = Math.max(0, Math.ceil(diffTime / (1000 * 60 * 60 * 24)));

      // E. Create the UserSubscriptionRequest (Snapshotting the current state)
      const subscriptionRequest = await tx.userSubscriptionRequest.create({
        data: {
          userId,
          requestedTests: currentAccess.maxTest, // Save the current max test
          requestedExpiresAt: currentAccess.expiresAt, // Save the expiration date
          requestedDays: remainingDays, // Save the remaining days
          userTestAccessId: currentAccess.id,
        },
      });

      return { subscriptionRequest };
    });

    return NextResponse.json({ 
      success: true, 
      message: "Subscription put to sleep and request created successfully",
      data: transactionResult 
    });

  } catch (error: any) {
    console.error("Sleep subscription error:", error);

    if (error.message === "NO_SUBSCRIPTION_FOUND") {
      return NextResponse.json(
        { success: false, error: "No active subscription found to sleep" },
        { status: 404 }
      );
    }

    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
