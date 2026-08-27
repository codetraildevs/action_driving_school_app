import {  NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';
 
import { z } from 'zod';

import { prisma } from "@/lib/prismaDB";

// GET all subscription plans
const getPlansHandler = withPermission(PERMISSIONS.SUBSCRIPTION_READ)(
  async (req, { user }) => {
    try {
      const plans = await prisma.subscriptionPlan.findMany({
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
      });

      return NextResponse.json({
        success: true,
        data: plans
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