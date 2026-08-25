"use server";

import { prisma } from "@/lib/prismaDB";
import { UserRequestStatus } from "@/lib/generated/prisma";

export async function getPendingSubscriptionRequests() {
  try {
    const pendingRequests = await prisma.userSubscriptionRequest.findMany({
      where: {
        status: UserRequestStatus.PENDING,
      },
      select: {
        id: true,
        userId: true,
        requestedTests: true,
        requestedDays: true,
        requestedExpiresAt: true,
        createdAt: true,
        status: true,
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
            middleName: true,
            phoneNumber: true,
            email: true,
            Pendinglanguage: {
              select: {
                nativeName: true,
              },
            },
          },
        },
      },
      orderBy: { createdAt: "desc" },
    });

    return { success: true, data: pendingRequests };
  } catch (error) {
    console.error("Get pending subscription requests error:", error);
    return {
      success: false,
      error: "Internal server error",
    };
  }
}
