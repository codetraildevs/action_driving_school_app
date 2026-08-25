import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { PrismaClient, UserTestAccessStatus } from "@/lib/generated/prisma";
import { z } from "zod";

import { prisma } from "@/lib/prismaDB";

const updatePlanSchema = z.object({
  tests: z.number().optional(),
  expiresAt: z.string(),
  status: z.string(),
});

const updatePlanHandler = withPermission(PERMISSIONS.SUBSCRIPTION_MANAGE)(
  async (req, { params, user }) => {
    try {
      const userId = parseInt(params.id);
      const body = await req.json();
      const validated = updatePlanSchema.parse(body);

      const plan = await prisma.userTestAccess.upsert({
        where: { userId },
        create: {
          userId,
          maxTest: validated.tests ||0,
          status: validated.status as UserTestAccessStatus,
          expiresAt: new Date(validated.expiresAt),
        },
        update: {
          maxTest: validated.tests,
          status: validated.status as UserTestAccessStatus,
          expiresAt: new Date(validated.expiresAt),
        },
      });

      // Log activity
      await prisma.userActivity.create({
        data: {
          activityType: "SUBSCRIPTION_PLAN_UPDATE",
          description: `Subscription plan "${plan.maxTest}" updated`,
          userId: user.userId,
        },
      });

      return NextResponse.json({
        success: true,
        data: plan,
      });
    } catch (error) {
      if (error instanceof z.ZodError ) {
        console.log(error)
        return NextResponse.json(
          { error: "Validation error", details: error.errors },
          { status: 400 }
        );
      }
      return NextResponse.json(
        { error: "Internal server error" },
        { status: 500 }
      );
    }
  }
);

export const PUT = updatePlanHandler;
