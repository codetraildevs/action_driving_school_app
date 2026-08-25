'use server';

import { PrismaClient } from '@/lib/generated/prisma';
import { cookies } from 'next/headers';
import { getCurrentUser } from '@/lib/auth/jwt';
import { hasPermission, PERMISSIONS } from '@/lib/auth/permissions';

import { prisma } from "@/lib/prismaDB";

async function verifyAnalyticsPermission() {
  const cookieStore = cookies();
  const token = (await cookieStore).get('auth_token');
  
  if (!token) {
    throw new Error('Unauthorized');
  }

  const user = await getCurrentUser({ 
    headers: new Headers(), 
    cookies: cookieStore 
  } as any);
  
  if (!user || !hasPermission(user.permissions, PERMISSIONS.ANALYTICS_VIEW)) {
    throw new Error('Forbidden: Insufficient permissions');
  }

  return user;
}

export async function getUserActivityLog(
  userId: number,
  options?: {
    limit?: number;
    offset?: number;
    activityType?: string;
  }
) {
  try {
    await verifyAnalyticsPermission();

    const activities = await prisma.userActivity.findMany({
      where: {
        userId,
        ...(options?.activityType ? { activityType: options.activityType } : {}),
      },
      take: options?.limit || 50,
      skip: options?.offset || 0,
      orderBy: { createdAt: 'desc' }
    });

    return { success: true, data: activities };
  } catch (error) {
    console.error('Get activity log error:', error);
    return { error: 'Failed to fetch activity log' };
  }
}

export async function getLoginAttempts(userId?: number, limit = 50) {
  try {
    await verifyAnalyticsPermission();

    const attempts = await prisma.loginAttempts.findMany({
      where: userId ? { userId } : {},
      take: limit,
      include: {
        user: {
          select: {
            id: true,
            email: true,
            firstName: true,
            lastName: true,
          }
        },
        device: true,
      },
      orderBy: { createdAt: 'desc' }
    });

    return { success: true, data: attempts };
  } catch (error) {
    console.error('Get login attempts error:', error);
    return { error: 'Failed to fetch login attempts' };
  }
}

export async function getSuspiciousSessions() {
  try {
    await verifyAnalyticsPermission();

    const sessions = await prisma.session.findMany({
      where: { isSuspect: true, endedAt: null },
      include: {
        user: {
          select: {
            id: true,
            email: true,
            firstName: true,
            lastName: true,
          }
        },
        device: true,
      },
      orderBy: { startedAt: 'desc' }
    });

    return { success: true, data: sessions };
  } catch (error) {
    console.error('Get suspicious sessions error:', error);
    return { error: 'Failed to fetch suspicious sessions' };
  }
}

export async function terminateSession(sessionId: number) {
  try {
    const admin = await verifyAnalyticsPermission();

    await prisma.session.update({
      where: { id: sessionId },
      data: { endedAt: new Date() }
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'SESSION_TERMINATED',
        description: `Session ${sessionId} terminated by admin`,
        userId: admin.userId,
      }
    });

    return { success: true };
  } catch (error) {
    console.error('Terminate session error:', error);
    return { error: 'Failed to terminate session' };
  }
}