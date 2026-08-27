export const dynamic = 'force-dynamic';

import { NextRequest } from "next/server";
import { prisma } from "@/lib/prismaDB";
import { UserRequestStatus } from "@/lib/generated/prisma";

async function getNotificationsData() {
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
           // Cap each poll: this endpoint is hit by every open admin console
           // every 5s, so loading all pending rows + user data each poll is
           // wasteful once the queue grows.
           take: 100,
         });

    return {
      success: true,
      data:  pendingRequests,
      count: 0,
      error: "Failed to check expiring invoices",
    };
  } catch (error) {
    console.error("Error checking expiring invoices:", error);
    return {
      success: false,
      count: 0,
      error: "Failed to check expiring invoices",
    };
  }
}

export async function GET(req: NextRequest) {
  let taskRunning = false;

  const stream = new ReadableStream({
    start(controller) {
      const encoder = new TextEncoder();

      controller.enqueue(
        encoder.encode(`data: ${JSON.stringify({ message: "Connected!" })}\n\n`)
      );

      const interval = setInterval(() => {
        if (!taskRunning) {
          taskRunning = true;
          getNotificationsData().then((data) => {
            taskRunning = false;
            controller.enqueue(
              encoder.encode(`data: ${JSON.stringify(data)}\n\n`)
            );
          });
        }
      }, 5000);

      req.signal.addEventListener("abort", () => {
        clearInterval(interval);
        controller.close();
      });
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache, no-transform",
      Connection: "keep-alive",
      "X-Accel-Buffering": "no",
    },
  });
}
