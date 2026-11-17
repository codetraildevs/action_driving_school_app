/**
 * Publishes the current Play-compliant privacy policy in every supported
 * language while retaining older versions for acceptance/audit history.
 * Safe to run repeatedly during deployment.
 */
/* eslint-disable @typescript-eslint/no-require-imports */
const { PrismaClient } = require("../lib/generated/prisma");
const {
  APP_VERSION,
  POLICY_VERSION,
  policies,
} = require("./privacy-policy-content");

const prisma = new PrismaClient();

async function publishPolicy(language, policy) {
  await prisma.$transaction(async (tx) => {
    const existing = await tx.privacyPolicy.findFirst({
      where: {
        version: POLICY_VERSION,
        appVersion: APP_VERSION,
        language,
      },
    });

    await tx.privacyPolicy.updateMany({
      where: { language, isActive: true },
      data: { isActive: false },
    });

    if (existing) {
      await tx.privacyPolicy.update({
        where: { id: existing.id },
        data: {
          title: policy.title,
          content: policy.content,
          isActive: true,
        },
      });
    } else {
      await tx.privacyPolicy.create({
        data: {
          version: POLICY_VERSION,
          appVersion: APP_VERSION,
          language,
          title: policy.title,
          content: policy.content,
          isActive: true,
        },
      });
    }
  });

  console.log(
    `  ${language}: policy ${POLICY_VERSION} for app ${APP_VERSION} is active`,
  );
}

async function main() {
  console.log("Publishing current privacy policies...");
  for (const [language, policy] of Object.entries(policies)) {
    await publishPolicy(language, policy);
  }
  console.log("Privacy policies updated successfully.");
}

main()
  .catch((error) => {
    console.error("Failed to update privacy policies:", error);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
