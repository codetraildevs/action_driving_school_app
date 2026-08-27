import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { verifyToken } from "@/lib/auth/jwt";

import { prisma } from "@/lib/prismaDB";

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

    const subscriptionPlans = await prisma.subscriptionPlan.findMany({
      include: {
        permissions: true,
        _count: {
          select: {
            userSubscriptions: true,
          },
        },
      },
    });

    return NextResponse.json({ success: true, data: subscriptionPlans });
  } catch (error) {
    console.error("Get subscription plans error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
// app/api/subscriptions/route.ts - POST method
const createPlanHandler = withPermission(PERMISSIONS.SUBSCRIPTION_MANAGE)(
  async (request: NextRequest) => {
  try {
    const { planName, amount, duration, permissions } = await request.json();

    if (!planName || !amount || !duration) {
      return NextResponse.json(
        { error: "Plan name, amount, and duration are required" },
        { status: 400 }
      );
    }

    const plan = await prisma.subscriptionPlan.create({
      data: {
        planName,
        amount: parseFloat(amount),
        duration: parseInt(duration),
        permissions: permissions && permissions.length > 0 ? {
          create: permissions.map((permissionName: string) => ({
            permissionName: permissionName.trim(),
          })),
        } : undefined,
      },
      include: {
        permissions: true,
        _count: {
          select: {
            userSubscriptions: true,
          },
        },
      },
    });

    return NextResponse.json({ data: plan }, { status: 201 });
  } catch (error: any) {
    console.error("Error creating subscription plan:", error);
    
    // Handle unique constraint violation
    if (error.code === 'P2002') {
      return NextResponse.json(
        { error: "A plan with this name already exists" },
        { status: 400 }
      );
    }
    
    return NextResponse.json(
      { error: "Failed to create subscription plan" },
      { status: 500 }
    );
  }
  }
);

export const POST = createPlanHandler;
