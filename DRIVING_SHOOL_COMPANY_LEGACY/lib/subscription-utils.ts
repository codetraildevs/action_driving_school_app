 ;
 

import { prisma } from "@/lib/prismaDB";

export async function checkUserSubscription(userId: number) {
  return await prisma.userSubscription.findUnique({
    where: { userId },
    include: {
      subscriptionPlan: {
        include: {
          permissions: true
        }
      }
    }
  });
}

export async function hasPermission(userId: number, permissionName: string) {
  const userPermission = await prisma.userPermission.findFirst({
    where: {
      userId,
      permission: {
        permissionName
      }
    },
    include: {
      permission: true
    }
  });

  return !!userPermission;
}

export async function getUserPermissions(userId: number) {
  const userPermissions = await prisma.userPermission.findMany({
    where: { userId },
    include: {
      permission: true
    }
  });

  return userPermissions.map(up => up.permission.permissionName);
}

export function isSubscriptionActive(subscription: any | null) {
  if (!subscription) return false;
  
  const createdAt = new Date(subscription.createdAt);
  const duration = subscription.subscriptionPlan.duration;  
  const expiryDate = new Date(createdAt.getTime() + duration * 24 * 60 * 60 * 1000);
  
  return new Date() < expiryDate;
}