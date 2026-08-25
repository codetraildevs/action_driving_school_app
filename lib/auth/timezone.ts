import { prisma } from "@/lib/prismaDB";

/**
 * Resolves the timezone name for a user.
 *
 * The `users` table stores a direct `timezoneId` FK, but many existing rows
 * lack a corresponding record in the `user_timezones` junction table. This
 * helper tries the junction-table relation first (which carries richer data
 * via the nested `timezone` include), and falls back to the direct FK when
 * the junction row is missing.
 */
export async function resolveTimezoneName(
  userTimezone: { timezone?: { timezoneName?: string } } | null | undefined,
  timezoneId: number,
): Promise<string> {
  // Fast path: junction table has data
  const tzName = userTimezone?.timezone?.timezoneName;
  if (tzName) return tzName;

  // Fallback: look up via the direct FK on the users table
  if (timezoneId) {
    const tz = await prisma.timezone.findUnique({
      where: { id: timezoneId },
      select: { timezoneName: true },
    });
    if (tz?.timezoneName) return tz.timezoneName;
  }

  return "UTC";
}
