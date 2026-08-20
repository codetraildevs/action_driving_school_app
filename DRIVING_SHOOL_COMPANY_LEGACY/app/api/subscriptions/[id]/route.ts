import { NextRequest, NextResponse } from 'next/server';
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { verifyToken } from "@/lib/auth/jwt";

import { prisma } from "@/lib/prismaDB";

interface Params {
  params: {
    id: string;
  };
}

// GET - Get specific subscription plan
export async function GET(request: NextRequest, { params }: Params) {
  try {
      const authHeader = request.headers.get('authorization');
     if (!authHeader || !authHeader.startsWith('Bearer ')) {
       return NextResponse.json({ success: false, error: 'Unauthorized: Missing or malformed token' }, { status: 401 });
     }
     const token = authHeader.substring(7);
 
     const payload = await verifyToken(token);
     if (!payload || !payload.userId) {
       return NextResponse.json({ success: false, error: 'Unauthorized: Invalid or expired token' }, { status: 401 });
     }
     
     const userId = payload.userId;

    const subscriptionPlan = await prisma.subscriptionPlan.findUnique({
      where: { id: parseInt(params.id) },
      include: {
        permissions: true,
        userSubscriptions: {
          include: {
            user: {
              select: {
                id: true,
                firstName: true,
                lastName: true,
                email: true
              }
            }
          }
        }
      }
    });

    if (!subscriptionPlan) {
      return NextResponse.json({ error: 'Subscription plan not found' }, { status: 404 });
    }

    return NextResponse.json({ success: true, data: subscriptionPlan });
  } catch (error) {
    console.error('Get subscription plan error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

// PUT - Update subscription plan (admin only)
// app/api/subscriptions/[id]/route.ts - PUT method
const updatePlanHandler = withPermission(PERMISSIONS.SUBSCRIPTION_MANAGE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { planName, amount, duration, permissions } = await request.json();

    if (!planName || !amount || !duration) {
      return NextResponse.json(
        { error: "Plan name, amount, and duration are required" },
        { status: 400 }
      );
    }

    // Use transaction to ensure data consistency
    const plan = await prisma.$transaction(async (tx) => {
      // Delete existing permissions
      await tx.permission.deleteMany({
        where: { subscriptionPlanId: parseInt(params.id) },
      });

      // Update the plan with new permissions
      const updatedPlan = await tx.subscriptionPlan.update({
        where: { id: parseInt(params.id) },
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

      return updatedPlan;
    });    return NextResponse.json({ data: plan });
  } catch (error: any) {
    console.error("Error updating subscription plan:", error);
    
    if (error.code === 'P2002') {
      return NextResponse.json(
        { error: "A plan with this name already exists" },
        { status: 400 }
      );
    }
    
    return NextResponse.json(
      { error: "Failed to update subscription plan" },
      { status: 500 }

    );
  }
  }
);

export const PUT = updatePlanHandler;

const deletePlanHandler = withPermission(PERMISSIONS.SUBSCRIPTION_MANAGE)(
  async (request: NextRequest, { params }: Params) => {
  try {
    // Check if there are active subscriptions
    const activeSubscriptions = await prisma.userSubscription.count({
      where: { subscriptionPlanId: parseInt(params.id) }
    });

    if (activeSubscriptions > 0) {
      return NextResponse.json(
        { error: 'Cannot delete plan with active subscriptions' },
        { status: 400 }
      );
    }

    await prisma.subscriptionPlan.delete({
      where: { id: parseInt(params.id) }
    });

    return NextResponse.json({ 
      success: true, 
      message: 'Subscription plan deleted successfully' 
    });
  } catch (error) {
    console.error('Delete subscription plan error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deletePlanHandler;