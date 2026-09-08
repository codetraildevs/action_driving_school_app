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
// Read operations are safe to retry — they have no side effects, so a retry
// after a transient DB blip cannot corrupt data. Writes are deliberately NOT
// retried: a timeout may have committed server-side, and retrying could
// duplicate rows (e.g. double-register a user or double-accept a request).
const RETRYABLE_OPERATIONS = new Set([
  "findMany",
  "findUnique",
  "findFirst",
  "findUniqueOrThrow",
  "findFirstOrThrow",
  "count",
  "aggregate",
  "groupBy",
]);

const READ_RETRY_ATTEMPTS = 3;
const READ_RETRY_BASE_DELAY_MS = 300;

// Signals a transient connection/pool problem worth retrying: pool timeout,
// refused/terminated connections, or Prisma's connection error codes.
function isTransientDbError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error);
  return (
    /pool timeout/.test(message) ||
    /failed to retrieve a connection/.test(message) ||
    /ECONNREFUSED/.test(message) ||
    /ECONNRESET/.test(message) ||
    /connect ETIMEDOUT/.test(message) ||
    /P1001/.test(message) || // Can't reach DB
    /P1002/.test(message) || // Timed out
    /P2028/.test(message) // Transaction API error (includes pool timeouts)
  );
}

function createPrismaClient(): PrismaClient {
  try {
    const url = process.env.DATABASE_URL;
    if (!url) {
      throw new Error("DATABASE_URL is not set");
    }
    const adapter = new PrismaMariaDb({
      ...parseDatabaseUrl(url),
      // MySQL 8.x creates users with the caching_sha2_password plugin by
      // default. Over a plain (non-TLS) connection that plugin needs the
      // server's RSA public key to exchange the password; the driver refuses
      // to fetch it by default and every query dies with
      // ER_CANNOT_RETRIEVE_RSA_KEY -> "pool timeout" -> 500s. The connection
      // is loopback-only (DATABASE_URL points at localhost), so allowing the
      // key retrieval is safe here.
      allowPublicKeyRetrieval: true,
    });
    const base = new PrismaClient({ adapter });

    // Retry transient failures on read operations (with backoff) so the app
    // rides through brief DB restarts instead of 500ing every request.
    return base.$extends({
      query: {
        $allModels: {
          async $allOperations({ operation, args, query }) {
            if (!RETRYABLE_OPERATIONS.has(operation)) {
              return query(args);
            }
            let lastError: unknown;
            for (let attempt = 1; attempt <= READ_RETRY_ATTEMPTS; attempt++) {
              try {
                return await query(args);
              } catch (error) {
                lastError = error;
                if (attempt >= READ_RETRY_ATTEMPTS || !isTransientDbError(error)) {
                  throw error;
                }
                await new Promise((resolve) =>
                  setTimeout(resolve, READ_RETRY_BASE_DELAY_MS * attempt)
                );
              }
            }
            throw lastError;
          },
        },
      },
    }) as unknown as PrismaClient;
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
