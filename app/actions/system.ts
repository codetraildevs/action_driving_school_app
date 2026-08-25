'use server';

import { PrismaClient } from '@/lib/generated/prisma';
import { revalidatePath } from 'next/cache';
import { cookies } from 'next/headers';
import { getCurrentUser } from '@/lib/auth/jwt';
import { hasPermission, PERMISSIONS } from '@/lib/auth/permissions';

import { prisma } from "@/lib/prismaDB";

async function verifySystemPermission() {
  const cookieStore = cookies();
  const token = (await cookieStore).get('auth_token');
  
  if (!token) {
    throw new Error('Unauthorized');
  }

  const user = await getCurrentUser({ 
    headers: new Headers(), 
    cookies: cookieStore 
  } as any);
  
  if (!user || !hasPermission(user.permissions, PERMISSIONS.SETTINGS_WRITE)) {
    throw new Error('Forbidden: Insufficient permissions');
  }

  return user;
}

export async function updateSystemSetting(
  settingKey: string,
  settingValue: string,
  description?: string
) {
  try {
    const admin = await verifySystemPermission();

    // First, try to find the existing setting by settingKey
    const existingSetting = await prisma.systemSetting.findFirst({
      where: { settingKey }
    });

    if (existingSetting) {
      // Update existing setting
      await prisma.systemSetting.update({
        where: { id: existingSetting.id },
        data: { 
          settingValue, 
          description: description || existingSetting.description 
        }
      });
    } else {
      // Create new setting
      await prisma.systemSetting.create({
        data: {
          settingKey,
          settingValue,
          description: description || '',
        }
      });
    }

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'SYSTEM_SETTING_UPDATE',
        description: `System setting "${settingKey}" updated`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/settings');
    return { success: true };
  } catch (error) {
    console.error('Update system setting error:', error);
    return { error: 'Failed to update system setting' };
  }
}

export async function createUserRole(
  roleName: string,
  description: string
) {
  try {
    const admin = await verifySystemPermission();

    const role = await prisma.userRole.create({
      data: {
        roleName,
        description,
      }
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'ROLE_CREATE',
        description: `Role "${roleName}" created`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/roles');
    return { success: true, data: role };
  } catch (error) {
    console.error('Create role error:', error);
    return { error: 'Failed to create role' };
  }
}

export async function handleDataDeletionRequest(
  requestId: number,
  action: 'approve' | 'reject',
  reason?: string
) {
  try {
    const admin = await verifySystemPermission();

    const request = await prisma.dataDeletionRequest.findUnique({
      where: { id: requestId },
      include: { user: true }
    });

    if (!request) {
      return { error: 'Request not found' };
    }

    if (action === 'approve') {
      // Start deletion process
      await prisma.dataDeletionRequest.update({
        where: { id: requestId },
        data: {
          status: 'processing',
          confirmedAt: new Date(),
        }
      });

      // TODO: Implement actual data deletion logic
      // This should be done asynchronously
      // await deleteUserData(request.userId);

      // For now, just mark as completed
      await prisma.dataDeletionRequest.update({
        where: { id: requestId },
        data: { status: 'completed' }
      });

    } else {
      await prisma.dataDeletionRequest.update({
        where: { id: requestId },
        data: {
          status: 'cancelled',
          reason: reason || 'Rejected by admin',
        }
      });
    }

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'DATA_DELETION_PROCESSED',
        description: `Data deletion request ${action}d for user ${request.userId}`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/data-requests');
    return { success: true };
  } catch (error) {
    console.error('Handle deletion request error:', error);
    return { error: 'Failed to process deletion request' };
  }
}

export async function exportUserData(userId: number) {
  try {
    const admin = await verifySystemPermission();

    // Fetch all user data
    const userData = await prisma.user.findUnique({
      where: { id: userId },
      include: {
        addresses: true,
        devices: true,
        userSubscription: {
          include: { subscriptionPlan: true }
        },
        readingSessions: true,
        testAttempts: true,
        testResults: true,
        bookmarks: true,
        notifications: true,
        ratings: true,
        transactions: true,
      }
    });

    if (!userData) {
      return { error: 'User not found' };
    }

    // Remove sensitive data
    const { password, ...userDataWithoutPassword } = userData;

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'USER_DATA_EXPORT',
        description: `Data exported for user ${userId}`,
        userId: admin.userId,
      }
    });

    return { success: true, data: userDataWithoutPassword };
  } catch (error) {
    console.error('Export user data error:', error);
    return { error: 'Failed to export user data' };
  }
}
