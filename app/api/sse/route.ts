export const dynamic = 'force-dynamic';

import { NextRequest } from "next/server";
import { prisma } from "@/lib/prismaDB";
import { UserRequestStatus } from "@/lib/generated/prisma";

// Poll cadence. 5s normally; on consecutive failures the poll backs off
// exponentially to 60s so a DB outage doesn't hammer the database (this
// endpoint is polled by every open admin console) or spam the error log.
const BASE_POLL_MS = 5000;
const MAX_POLL_MS = 60000;
const ERROR_LOG_EVERY_MS = 60000;

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
      data: pendingRequests,
      count: pendingRequests.length,
    };
  } catch (error) {
    return {
      success: false,
      data: [],
      count: 0,
      error: "Failed to check expiring invoices",
    };
  }
}

export async function GET(req: NextRequest) {
  // Stream state is shared between the start() and cancel() source methods.
  let closed = false; // client gone / stream torn down
  let timer: ReturnType<typeof setTimeout> | null = null;

  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      const encoder = new TextEncoder();
      let pollMs = BASE_POLL_MS;
      let lastErrorLoggedAt = 0;

      // Enqueue only while the stream is open. A client can disconnect while
      // a poll is in flight; enqueueing on a closed controller throws, and
      // without this guard that throw lands in a .then() as an unhandled
      // rejection ("Invalid state: Controller is already closed").
      const safeEnqueue = (payload: unknown) => {
        if (closed) return;
        try {
          controller.enqueue(
            encoder.encode(`data: ${JSON.stringify(payload)}\n\n`)
          );
        } catch {
          closed = true;
        }
      };

      const runPoll = () => {
        if (closed) return;
        // getNotificationsData never rejects (it catches its own errors and
        // returns a value), but keep a catch-all so no code path in here can
        // ever surface as an unhandled rejection.
        getNotificationsData()
          .then((data) => {
            if (closed) return;
            if (data.success) {
              pollMs = BASE_POLL_MS;
            } else {
              pollMs = Math.min(pollMs * 2, MAX_POLL_MS);
              const now = Date.now();
              if (now - lastErrorLoggedAt >= ERROR_LOG_EVERY_MS) {
                lastErrorLoggedAt = now;
                console.error(
                  `SSE: ${data.error} — backing off polls to ${pollMs}ms`
                );
              }
            }
            safeEnqueue(data);
          })
          .catch(() => {
            // Unreachable today, but if it ever happens: back off and keep
            // the stream alive instead of crashing it.
            if (closed) return;
            pollMs = Math.min(pollMs * 2, MAX_POLL_MS);
          })
          .finally(() => {
            if (!closed) schedule();
          });
      };

      // Chain polls with setTimeout so the backoff actually changes the
      // cadence, and a slow query can never overlap the next poll.
      const schedule = () => {
        if (closed) return;
        timer = setTimeout(runPoll, pollMs);
      };

      const teardown = () => {
        if (closed) return;
        closed = true;
        if (timer) clearTimeout(timer);
        try {
          controller.close();
        } catch {
          // Stream already closed/errored — nothing to do.
        }
      };

      // Client disconnected (Next.js fires abort on req.signal)…
      req.signal.addEventListener("abort", teardown);

      safeEnqueue({ message: "Connected!" });
      schedule();
    },
    cancel() {
      // Consumer cancelled the stream (client disconnect without an abort
      // signal). Do NOT call controller.close() here — the stream is already
      // in the closing state. Just stop timers and mark the stream dead.
      closed = true;
      if (timer) clearTimeout(timer);
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