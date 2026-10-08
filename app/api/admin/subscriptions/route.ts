import {  NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';
 
import { z } from 'zod';

import { prisma } from "@/lib/prismaDB";
import { UserTestAccessStatus } from "@/lib/generated/prisma";

// GET all subscription plans
const getPlansHandler = withPermission(PERMISSIONS.SUBSCRIPTION_READ)(
  async (req, { user }) => {
    try {
      // Per-plan "subscriber" counts read from user_subscriptions are legacy:
      // that table is no longer written by the register/accept flows, and
      // plans have no link to user_test_access, so a truthful per-plan figure
      // is not derivable. Return the real platform-wide access-ledger count
      // instead and let the UI present it once at page level.
      const [plans, activeLearningAccess] = await Promise.all([
        prisma.subscriptionPlan.findMany({
          include: {
            permissions: true,
            _count: {
              select: {
                userSubscriptions: true,
                transactions: true,
              }
            }
          },
          orderBy: { amount: 'asc' }
        }),
        // Same definition as the admin dashboard metric: mirrors the access
        // gate in app/api/tests/[id]/questions/route.ts (status only;
        // expiresAt is display-only and never enforced when serving content).
        prisma.userTestAccess?.count({
          where: {
            status: {
              in: [UserTestAccessStatus.ACTIVE, UserTestAccessStatus.PENDING],
            },
          },
        }),
      ]);

      return NextResponse.json({
        success: true,
        data: plans,
        activeLearningAccess,
      });
    } catch (error) {
      console.error('Fetch plans error:', error);
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      );
    }
  }
);

// POST - Create subscription plan
const createPlanSchema = z.object({
  planName: z.string().min(1),
  amount: z.number().positive(),
  duration: z.number().positive(), // in days
  permissionIds: z.array(z.number()).optional(),
});

const createPlanHandler = withPermission(PERMISSIONS.SUBSCRIPTION_MANAGE)(
  async (req, { user }) => {
    try {
      const body = await req.json();
      const validated = createPlanSchema.parse(body);
      const { permissionIds, ...planData } = validated;

      const plan = await prisma.subscriptionPlan.create({
        data: planData,
      });

      // Add permissions if provided
      if (permissionIds && permissionIds.length > 0) {
        await prisma.permission.updateMany({
          where: { id: { in: permissionIds } },
          data: { subscriptionPlanId: plan.id }
        });
      }

      // Log activity
      await prisma.userActivity.create({
        data: {
          activityType: 'SUBSCRIPTION_PLAN_CREATE',
          description: `Subscription plan "${plan.planName}" created`,
          userId: user.userId,
        }
      });

      return NextResponse.json({
        success: true,
        data: plan
      }, { status: 201 });
    } catch (error) {
      if (error instanceof z.ZodError) {
        return NextResponse.json(
          { error: 'Validation error', details: error.errors },
          { status: 400 }
        );
      }
      console.error('Create plan error:', error);
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      );
    }
  }
);

export const GET = getPlansHandler;
export const POST = createPlanHandler;