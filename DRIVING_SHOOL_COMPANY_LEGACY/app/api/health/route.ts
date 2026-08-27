import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prismaDB";

// Never cache: every probe must reflect live DB + app state.
export const dynamic = "force-dynamic";

/**
 * Lightweight health probe for uptime monitors (UptimeRobot) and the
 * server-side cron healthcheck.js. Only does a SELECT 1 against the DB —
 * no heavy queries, no auth (listed in middleware publicRoutes).
 *
 * Returns:
 *   200 { status: "ok", db: "ok", ... }  when the DB is reachable
 *   503 { status: "degraded", db: "error", ... } when the DB is down
 */
export async function GET(_request: NextRequest) {
  const started = Date.now();
  try {
    await prisma.$queryRaw`SELECT 1 as ok`;
    return NextResponse.json({
      status: "ok",
      db: "ok",
      uptimeSec: Math.round(process.uptime()),
      responseTimeMs: Date.now() - started,
      timestamp: new Date().toISOString(),
    });
  } catch (e) {
    return NextResponse.json(
      {
        status: "degraded",
        db: "error",
        error: e instanceof Error ? e.message : String(e),
        responseTimeMs: Date.now() - started,
        timestamp: new Date().toISOString(),
      },
      { status: 503 }
    );
  }
}
