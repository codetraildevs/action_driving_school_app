/**
 * Backfill missing UserTimezone junction records.
 *
 * The `users.timezoneId` direct FK has been set for a long time, but the
 * `user_timezones` junction table was never populated for existing rows.
 * This causes login / profile / refresh to return timezone: undefined.
 *
 * Run with:
 *   node prisma/backfill-user-timezones.js
 *
 * Reads DATABASE_URL from .env or the environment.
 */
const { PrismaClient } = require("../lib/generated/prisma");

const prisma = new PrismaClient();

async function main() {
  console.log("Starting backfill of UserTimezone records...\n");

  // 1. Find all users that have a timezoneId but NO UserTimezone row
  const usersWithoutTz = await prisma.user.findMany({
    where: {
      userTimezone: null, // no junction row
      timezoneId: { not: 0 },
    },
    select: {
      id: true,
      firstName: true,
      lastName: true,
      phoneNumber: true,
      timezoneId: true,
    },
  });

  if (usersWithoutTz.length === 0) {
    console.log("All users already have a UserTimezone record. Nothing to do.");
    return;
  }

  console.log(`Found ${usersWithoutTz.length} user(s) missing a UserTimezone record:\n`);

  let created = 0;
  let skipped = 0;

  for (const user of usersWithoutTz) {
    try {
      await prisma.userTimezone.create({
        data: {
          userId: user.id,
          timezoneId: user.timezoneId,
        },
      });
      created++;
      console.log(
        `  ✔ Created for user ${user.id} (${user.firstName} ${user.lastName}, ${user.phoneNumber}) → timezoneId ${user.timezoneId}`
      );
    } catch (err) {
      // Unique constraint violated — record already exists (race-safe)
      if (err.code === "P2002") {
        skipped++;
        console.log(
          `  ⏭ Skipped user ${user.id} — record already exists`
        );
      } else {
        console.error(
          `  ✘ Failed for user ${user.id}:`,
          err.message
        );
      }
    }
  }

  console.log(`\nBackfill complete: ${created} created, ${skipped} skipped.`);
}

main()
  .catch((e) => {
    console.error("Backfill failed:", e);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
