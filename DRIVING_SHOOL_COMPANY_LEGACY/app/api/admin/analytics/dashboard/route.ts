// app/api/admin/analytics/dashboard/route.ts
import { NextRequest, NextResponse } from "next/server";

import { verifyToken } from "@/lib/auth/jwt";
import { isAdminRoleName } from "@/lib/auth/roles";
import { prisma } from "@/lib/prismaDB";

export async function GET(request: NextRequest) {
  try {
    // Only console roles (admin / super_admin) may read platform analytics.
    const authHeader = request.headers.get("authorization");
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Missing or malformed token" },
        { status: 401 },
      );
    }
    const token = authHeader.substring(7);

    const payload = await verifyToken(token);
    if (!payload || !payload.userId) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Invalid or expired token" },
        { status: 401 },
      );
    }

    const admin = await prisma.user.findUnique({
      where: { id: payload.userId },
      include: { role: true },
    });

    if (!isAdminRoleName(admin?.role.roleName)) {
      return NextResponse.json({ error: "Forbidden" }, { status: 403 });
    }

    // Run the lightweight count queries first, sequentially
    const totalUsers = await prisma.user.count();
    const activeUsers = await prisma.user.count({
      where: {
        lastLogin: {
          gte: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), // last 30 days
        },
      },
    });

    const totalSubscriptions = await prisma.userSubscription.count();
    const totalTests = await prisma.test.count();
    const totalLearningMaterials = await prisma.learningMaterial.count();
    const totalPdfFiles = await prisma.pdfFile.count();

    // Then run heavier queries
    const recentUsers = await prisma.user.findMany({
      take: 5,
      orderBy: { createdAt: "desc" },
      select: {
        id: true,
        firstName: true,
        lastName: true,
        email: true,
        createdAt: true,
      },
    });

    const recentSubscriptions = await prisma.userSubscription.findMany({
      take: 5,
      orderBy: { createdAt: "desc" },
      include: {
        user: {
          select: {
            firstName: true,
            lastName: true,
            email: true,
          },
        },
        subscriptionPlan: {
          select: {
            planName: true,
            amount: true,
          },
        },
      },
    });

    // Aggregate logic (same as before)
    const totalContent =
      totalTests + totalLearningMaterials + totalPdfFiles;

    const recentActivity = [
      ...recentUsers.map((user) => ({
        id: user.id,
        type: "user_registration",
        title: "New User Registration",
        description: `${user.firstName} ${user.lastName} joined the platform`,
        timestamp: user.createdAt,
        user: {
          name: `${user.firstName} ${user.lastName}`,
          email: user.email,
        },
      })),
      ...recentSubscriptions.map((sub) => ({
        id: sub.id,
        type: "subscription",
        title: "New Subscription",
        description: `${sub.user.firstName} ${sub.user.lastName} subscribed to ${sub.subscriptionPlan.planName}`,
        timestamp: sub.createdAt,
        user: {
          name: `${sub.user.firstName} ${sub.user.lastName}`,
          email: sub.user.email,
        },
        subscription: {
          plan: sub.subscriptionPlan.planName,
          amount: sub.subscriptionPlan.amount,
        },
      })),
    ]
      .sort(
        (a, b) =>
          new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      )
      .slice(0, 10);

    const popularContent = await prisma.learningMaterial.findMany({
      take: 5,
      orderBy: {
        userLearningMaterials: { _count: "desc" },
      },
      include: {
        _count: { select: { userLearningMaterials: true } },
      },
    });

    const stats = {
      totalUsers,
      activeUsers,
      totalSubscriptions,
      totalContent,
      totalTests,
      totalLearningMaterials,
      totalPdfFiles,
      recentActivity,
      popularContent: popularContent.map((item) => ({
        id: item.id,
        title: item.title,
        type: "learning_material",
        downloads: item._count.userLearningMaterials,
        fileType: item.fileType,
      })),
    };

    return NextResponse.json({ data: stats });
  } catch (error) {
    console.error("Error fetching dashboard data:", error);
    return NextResponse.json(
      { error: "Failed to fetch dashboard data" },
      { status: 500 }
    );
  }
}
