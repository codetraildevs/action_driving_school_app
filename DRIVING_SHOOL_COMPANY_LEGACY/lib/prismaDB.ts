import { PrismaClient } from "@/lib/generated/prisma"
import { PrismaMariaDb } from "@prisma/adapter-mariadb"

const globalForPrisma = global as unknown as { prisma: PrismaClient };

/**
 * Parses a mysql:// URL into the config object expected by the mariadb driver
 * (the @prisma/adapter-mariadb adapter does not take a raw connection string).
 */
function parseDatabaseUrl(url: string) {
  const u = new URL(url);
  return {
    host: u.hostname,
    port: u.port ? parseInt(u.port, 10) : 3306,
    user: decodeURIComponent(u.username),
    password: decodeURIComponent(u.password),
    database: u.pathname ? decodeURIComponent(u.pathname.slice(1)) : undefined,
  };
}

/**
 * Uses the mariadb driver adapter (@prisma/adapter-mariadb) so queries run in
 * JavaScript instead of the Rust query engine. The Rust engine panics with
 * "PANIC: timer has gone away" on this shared host (cPanel/CloudLinux), which
 * crashed every DB query. Falls back to the engine-based client if the
 * adapter cannot be initialized for any reason.
 */
function createPrismaClient(): PrismaClient {
  try {
    const url = process.env.DATABASE_URL;
    if (!url) {
      throw new Error("DATABASE_URL is not set");
    }
    const adapter = new PrismaMariaDb(parseDatabaseUrl(url));
    return new PrismaClient({ adapter });
  } catch (e) {
    console.error(
      "Driver adapter init failed, falling back to default client:",
      e instanceof Error ? e.message : e
    );
    return new PrismaClient();
  }
}

export const prisma = globalForPrisma.prisma || createPrismaClient();

if (process.env.NODE_ENV !== "production") globalForPrisma.prisma = prisma;
