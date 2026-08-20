 import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/auth/jwt";
import { isAdminRoleName } from "@/lib/auth/roles";

import { prisma } from "@/lib/prismaDB";

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
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

    // Check if admin
    const admin = await prisma.user.findUnique({
      where: { id: payload.userId },
      include: { role: true },
    });

    if (!isAdminRoleName(admin?.role.roleName)) {
      return NextResponse.json({ error: "Forbidden" }, { status: 403 });
    }

    const userId = parseInt(params.id);
    const body = await request.json();
    const { subscriptionPlanId } = body;

    if (!subscriptionPlanId) {
      return NextResponse.json(
        { error: "Subscription plan ID is required" },
        { status: 400 }
      );
    }

    // Check if user exists
    const user = await prisma.user.findUnique({
      where: { id: userId },
    });

    if (!user) {
      return NextResponse.json({ error: "User not found" }, { status: 404 });
    }

    // Check if subscription plan exists
    const plan = await prisma.subscriptionPlan.findUnique({
      where: { id: subscriptionPlanId },
    });

    if (!plan) {
      return NextResponse.json(
        { error: "Subscription plan not found" },
        { status: 404 }
      );
    }

     const startDate = new Date();
        const endDate = new Date(startDate);
        endDate.setDate(startDate.getDate() + plan.duration);

    // Update or create user subscription
    const userSubscription = await prisma.userSubscription.upsert({
      where: { userId: userId },
      update: {
        subscriptionPlanId: subscriptionPlanId,
        startDate,
        endDate
      },
      create: {
        userId: userId,
        subscriptionPlanId: subscriptionPlanId,
        startDate,
        endDate
      },
      include: {
        subscriptionPlan: true,
      },
    });

    return NextResponse.json({
      success: true,
      data: userSubscription,
      message: "User subscription updated successfully",
    });
  } catch (error) {
    console.error("Update user subscription error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
