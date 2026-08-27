'use server';

import { PrismaClient } from '@/lib/generated/prisma';
import { revalidatePath } from 'next/cache';
import { getCurrentUser } from '@/lib/auth/jwt';
import { cookies } from 'next/headers';
import { hasPermission, PERMISSIONS } from '@/lib/auth/permissions';
import bcrypt from 'bcryptjs';

import { prisma } from "@/lib/prismaDB";

async function verifyAdmin() {
  const cookieStore = cookies();
  const token = (await cookieStore).get('auth_token');
  
  if (!token) {
    throw new Error('Unauthorized');
  }

  const user = await getCurrentUser({ 
    headers: new Headers(), 
    cookies: cookieStore 
  } as any);
  
  if (!user || !hasPermission(user.permissions, PERMISSIONS.USER_UPDATE)) {
    throw new Error('Forbidden: Insufficient permissions');
  }

  return user;
}

export async function updateUserStatus(userId: number, isActive: boolean) {
  try {
    const admin = await verifyAdmin();

    // Prevent self-deactivation
    if (userId === admin.userId && !isActive) {
      return { error: 'Cannot deactivate your own account' };
    }

    await prisma.user.update({
      where: { id: userId },
      data: { isActive }
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'USER_STATUS_UPDATE',
        description: `User ${userId} ${isActive ? 'activated' : 'deactivated'}`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/users');
    return { success: true };
  } catch (error) {
    console.error('Update user status error:', error);
    return { error: error instanceof Error ? error.message : 'Failed to update user status' };
  }
}

export async function bulkDeleteUsers(userIds: number[]) {
  try {
    const admin = await verifyAdmin();

    if (!hasPermission(admin.permissions, PERMISSIONS.USER_DELETE)) {
      return { error: 'Insufficient permissions' };
    }

    // Prevent self-deletion
    if (userIds.includes(admin.userId)) {
      return { error: 'Cannot delete your own account' };
    }

    // Soft delete by deactivating
    await prisma.user.updateMany({
      where: { id: { in: userIds } },
      data: { isActive: false }
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'BULK_USER_DELETE',
        description: `${userIds.length} users deactivated`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/users');
    return { success: true, count: userIds.length };
  } catch (error) {
    console.error('Bulk delete users error:', error);
    return { error: 'Failed to delete users' };
  }
}

export async function assignRoleToUser(userId: number, roleId: number) {
  try {
    const admin = await verifyAdmin();

    await prisma.user.update({
      where: { id: userId },
      data: { roleId }
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'ROLE_ASSIGNMENT',
        description: `Role ${roleId} assigned to user ${userId}`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/users');
    return { success: true };
  } catch (error) {
    console.error('Assign role error:', error);
    return { error: 'Failed to assign role' };
  }
}

export async function resetUserPassword(userId: number, newPassword?: string) {
  try {
    const admin = await verifyAdmin();

    // Generate random password if not provided
    const password = newPassword || Math.random().toString(36).slice(-10);
    const hashedPassword = await bcrypt.hash(password, 12);

    await prisma.user.update({
      where: { id: userId },
      data: { password: hashedPassword }
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'PASSWORD_RESET_ADMIN',
        description: `Password reset for user ${userId}`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/users');
    return { success: true, temporaryPassword: password };
  } catch (error) {
    console.error('Reset password error:', error);
    return { error: 'Failed to reset password' };
  }
}
