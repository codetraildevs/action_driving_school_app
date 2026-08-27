'use server';

import { PrismaClient } from '@/lib/generated/prisma';
import { cookies } from 'next/headers';
import { getCurrentUser } from '@/lib/auth/jwt';
import { hasPermission, PERMISSIONS } from '@/lib/auth/permissions';
import { sendNotificationToUser, sendNotificationToRole, broadcastNotification } from '@/lib/notifications/websocket';
import { sendPushNotificationToUser, sendPushNotificationToMultipleUsers } from '@/lib/notifications/firebase';

import { prisma } from "@/lib/prismaDB";

async function verifyAdminPermission() {
  const cookieStore = cookies();
  const token = (await cookieStore).get('auth_token');
  
  if (!token) {
    throw new Error('Unauthorized');
  }

  const user = await getCurrentUser({ 
    headers: new Headers(), 
    cookies: cookieStore 
  } as any);
  
  if (!user) {
    throw new Error('Unauthorized');
  }

  return user;
}

export async function sendNotificationToSingleUser(
  userId: number,
  notification: {
    title: string;
    message: string;
    type?: string;
  }
) {
  try {
    const admin = await verifyAdminPermission();

    // Send via WebSocket
    await sendNotificationToUser(userId, notification);

    // Send push notification
    await sendPushNotificationToUser(userId, {
      title: notification.title,
      body: notification.message,
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'NOTIFICATION_SENT',
        description: `Notification sent to user ${userId}`,
        userId: admin.userId,
      }
    });

    return { success: true };
  } catch (error) {
    console.error('Send notification error:', error);
    return { error: 'Failed to send notification' };
  }
}

export async function sendBulkNotification(
  userIds: number[],
  notification: {
    title: string;
    message: string;
    type?: string;
  }
) {
  try {
    const admin = await verifyAdminPermission();

    // Send to multiple users
    await Promise.all(
      userIds.map(userId => sendNotificationToUser(userId, notification))
    );

    // Send push notifications
    await sendPushNotificationToMultipleUsers(
      userIds,
      {
        title: notification.title,
        body: notification.message,
      }
    );

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'BULK_NOTIFICATION_SENT',
        description: `Notification sent to ${userIds.length} users`,
        userId: admin.userId,
      }
    });

    return { success: true, count: userIds.length };
  } catch (error) {
    console.error('Send bulk notification error:', error);
    return { error: 'Failed to send bulk notification' };
  }
}

export async function sendRoleBasedNotification(
  roleId: number,
  notification: {
    title: string;
    message: string;
    type?: string;
  }
) {
  try {
    const admin = await verifyAdminPermission();

    // Send to role
    await sendNotificationToRole(roleId, notification);

    // Get users in role for push notifications
    const users = await prisma.user.findMany({
      where: { roleId, isActive: true },
      select: { id: true }
    });

    await sendPushNotificationToMultipleUsers(
      users.map(u => u.id),
      {
        title: notification.title,
        body: notification.message,
      }
    );

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'ROLE_NOTIFICATION_SENT',
        description: `Notification sent to role ${roleId}`,
        userId: admin.userId,
      }
    });

    return { success: true, count: users.length };
  } catch (error) {
    console.error('Send role notification error:', error);
    return { error: 'Failed to send role notification' };
  }
}

export async function sendSystemWideNotification(notification: {
  title: string;
  message: string;
  type?: string;
}) {
  try {
    const admin = await verifyAdminPermission();

    // Must be super admin
    if (admin.roleId !== 1) {
      return { error: 'Only super admins can send system-wide notifications' };
    }

    // Broadcast to all
    await broadcastNotification(notification);

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'SYSTEM_NOTIFICATION_SENT',
        description: `System-wide notification sent`,
        userId: admin.userId,
      }
    });

    return { success: true };
  } catch (error) {
    console.error('Send system notification error:', error);
    return { error: 'Failed to send system notification' };
  }
}

