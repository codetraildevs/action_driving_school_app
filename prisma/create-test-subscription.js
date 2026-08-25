/**
 * Create a test UserSubscription record for an existing user.
 *
 * Run with:
 *   node prisma/create-test-subscription.js
 *
 * Reads DATABASE_URL from .env.
 */
const { PrismaClient } = require("../lib/generated/prisma");

const prisma = new PrismaClient();

async function main() {
  const userId = 2; // Admin user (Alexis)

  // Check if user already has a subscription
  const existing = await prisma.userSubscription.findUnique({
    where: { userId },
  });

  if (existing) {
    console.log(`User ${userId} already has a subscription (id: ${existing.id}). Skipping.`);
    return;
  }

  // Find a plan to assign (Basic Plan = id 2, or fallback to first available)
  let plan = await prisma.subscriptionPlan.findFirst({
    where: { planName: "Basic Plan" },
  });
  if (!plan) {
    plan = await prisma.subscriptionPlan.findFirst();
  }
  if (!plan) {
    console.error("No subscription plans found. Create a plan first.");
    process.exit(1);
  }

  const startDate = new Date();
  const endDate = new Date();
  endDate.setDate(endDate.getDate() + 30);

  const subscription = await prisma.userSubscription.create({
    data: {
      userId,
      subscriptionPlanId: plan.id,
      startDate,
      endDate,
    },
  });

  console.log(`✔ Created UserSubscription for user ${userId}:`);
  console.log(`  Plan: ${plan.planName} (${plan.amount} RWF)`);
  console.log(`  Period: ${startDate.toISOString().slice(0, 10)} → ${endDate.toISOString().slice(0, 10)}`);
  console.log(`  Subscription ID: ${subscription.id}`);
}

main()
  .catch((e) => {
    console.error("Error:", e);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
