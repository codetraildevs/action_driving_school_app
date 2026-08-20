/**
 * Local development seed for DRIVINGSCHOOL2 (isolated MariaDB on port 3307).
 *
 * Run with:
 *   DATABASE_URL="mysql://driving:driving123@127.0.0.1:3307/driving_school_local" node prisma/seed-local.js
 *
 * Creates: roles 1-10, languages (en/fr/rw), timezones, an ADMIN user and a
 * REGULAR user with known credentials, plus sample content for the admin
 * dashboard (subscriptions, Irembo requests, tests, learning materials).
 *
 * Admin credentials (local only):
 *   phone: +250780000001  password: Admin@123456
 * Regular user:
 *   phone: +250780000002  password: User@123456
 */
const bcrypt = require("bcryptjs");
const { PrismaClient } = require("../lib/generated/prisma");

const prisma = new PrismaClient();

async function main() {
  console.log("Seeding local database...");

  // 1. Roles (ids 1-10) — names match lib/auth/roles.ts (admin / super_admin)
  const roles = [
    [1, "super_admin", "Full access to the entire platform"],
    [2, "admin", "Platform administrator"],
    [3, "content_manager", "Manages learning content"],
    [4, "teacher", "Driving instructor"],
    [5, "student", "Standard app user"],
    [6, "premium_user", "User with premium subscription"],
    [7, "free_user", "User on the free tier"],
    [8, "moderator", "Moderates content and activity"],
    [9, "support_staff", "Customer support"],
    [10, "guest", "Guest visitor"],
  ];
  for (const [id, name, desc] of roles) {
    await prisma.userRole.upsert({
      where: { id },
      update: { roleName: name, description: desc },
      create: { id, roleName: name, description: desc },
    });
  }
  console.log("  roles: ok");

  // 2. Languages
  const languages = [
    ["en", "English", "English"],
    ["fr", "French", "Français"],
    ["rw", "Kinyarwanda", "Kinyarwanda"],
  ];
  for (const [code, name, native] of languages) {
    await prisma.language.upsert({
      where: { languageCode: code },
      update: { languageName: name, nativeName: native, isActive: true },
      create: { languageCode: code, languageName: name, nativeName: native, isActive: true },
    });
  }
  console.log("  languages: ok");

  // 3. Timezones (no unique field on Timezone, so findFirst + create)
  const timezones = [
    ["Africa/Kigali", "+02:00", 120, "RW", "Rwanda", "Africa/Kigali", false],
    ["UTC", "+00:00", 0, "US", "Coordinated Universal Time", "UTC", false],
    ["Europe/Brussels", "+01:00", 60, "BE", "Belgium", "Europe/Brussels", false],
  ];
  for (const [name, offset, minutes, cc, cname, region, dst] of timezones) {
    const existing = await prisma.timezone.findFirst({ where: { timezoneName: name } });
    if (!existing) {
      await prisma.timezone.create({
        data: {
          timezoneName: name,
          utcOffset: offset,
          offsetInMinutes: minutes,
          countryCode: cc,
          countryName: cname,
          region,
          isDst: dst,
        },
      });
    }
  }
  console.log("  timezones: ok");

  const en = await prisma.language.findUnique({ where: { languageCode: "en" } });
  const kigali = await prisma.timezone.findFirst({ where: { timezoneName: "Africa/Kigali" } });
  if (!en || !kigali) throw new Error("Missing language/timezone seed");

  // 4. Admin user (role 2) — known credentials for local testing
  const adminPassword = await bcrypt.hash("Admin@123456", 10);
  const admin = await prisma.user.upsert({
    where: { phoneNumber: "+250780000001" },
    update: {},
    create: {
      firstName: "System",
      middleName: "",
      lastName: "Admin",
      email: "admin@drivingschool.local",
      phoneNumber: "+250780000001",
      isActive: true,
      roleId: 2,
      languageId: en.id,
      timezoneId: kigali.id,
      password: adminPassword,
    },
  });
  console.log("  admin user: ok (phone +250780000001 / Admin@123456)");

  // 5. Regular user (role 5) with a device so login works (device binding)
  const userPassword = await bcrypt.hash("User@123456", 10);
  const regular = await prisma.user.upsert({
    where: { phoneNumber: "+250780000002" },
    
    update: {},
    create: {
      firstName: "Test",
      middleName: "",
      lastName: "Student",
      email: "student@drivingschool.local",
      phoneNumber: "+250780000002",
      isActive: true,
      roleId: 5,
      languageId: en.id,
      timezoneId: kigali.id,
      password: userPassword,
    },
  });
  await prisma.device.upsert({
    where: { physicalAddress: "test-device-123" },
    update: { userId: regular.id },
    create: {
      physicalAddress: "test-device-123",
      manufacturer: "Test",
      model: "Local",
      name: "Local Test Device",
      userId: regular.id,
    },
  });
  await prisma.userTestAccess.upsert({
    where: { userId: regular.id },
    update: {},
    create: {
      userId: regular.id,
      maxTest: 5,
      expiresAt: new Date(Date.now() + 30 * 86400000),
      status: "ACTIVE",
    },
  });
  console.log("  regular user: ok (phone +250780000002 / User@123456)");

  // 6. Sample data for the admin dashboard / requests tab
  const plan = await prisma.subscriptionPlan.upsert({
    where: { planName: "Premium" },
    update: {},
    create: { planName: "Premium", amount: 5000, duration: 30 },
  });
  await prisma.userSubscription.upsert({
    where: { userId: regular.id },
    update: {},
    create: {
      userId: regular.id,
      subscriptionPlanId: plan.id,
      startDate: new Date(),
      endDate: new Date(Date.now() + 30 * 86400000),
    },
  });

  // Sample rows below are guarded with findFirst so the seed is idempotent.
  const hasDriving = await prisma.iremboDrivingLicenseRequest.findFirst({
    where: { referenceId: "LOCAL-DL-0001" },
  });
  if (!hasDriving) {
    await prisma.iremboDrivingLicenseRequest.create({
      data: {
        userId: regular.id,
        category: "B",
        licenseType: "LEARNER",
        applicationType: "NEW",
        referenceId: "LOCAL-DL-0001",
        applicantName: "Test Student",
        applicantPhoneNumber: "+250780000002",
        applicantNationalId: "1199000000000000",
        address: "Kigali, Rwanda",
        status: "PENDING",
        completionPercentage: 40,
        currentStep: "payment",
        message: "Waiting for payment confirmation",
        paymentStatus: "PENDING",
        paymentAmount: 500,
      },
    });
  }

  const hasSpecial = await prisma.iremboSpecialRequest.findFirst({
    where: { referenceId: "LOCAL-SP-0001" },
  });
  if (!hasSpecial) {
    await prisma.iremboSpecialRequest.create({
      data: {
        userId: regular.id,
        serviceName: "Criminal Record Certificate",
        category: "police",
        referenceId: "LOCAL-SP-0001",
        applicantName: "Test Student",
        applicantPhone: "+250780000002",
        nationalId: "1199000000000000",
        description: "Criminal record for job application",
        status: "PROCESSING",
        completionPercentage: 70,
        currentStep: "processing",
        message: "Being processed by Irembo",
      },
    });
  }

  const hasTest = await prisma.test.findFirst({ where: { testNumber: 1 } });
  if (!hasTest) {
    await prisma.test.create({
      data: {
        title: "Theory Test 1",
        description: "Basic road signs and rules",
        testNumber: 1,
        totalMarks: 100,
        passMarks: 60,
        duration: 60,
        isFree: true,
      },
    });
  }

  const hasMaterial = await prisma.learningMaterial.findFirst({
    where: { title: "Road Signs Handbook (EN)" },
  });
  if (!hasMaterial) {
    await prisma.learningMaterial.create({
      data: {
        title: "Road Signs Handbook (EN)",
        description: "Official road signs reference",
        filePath: "/uploads/learning-materials/road-signs-en.pdf",
        fileType: "pdf",
        isPublic: true,
      },
    });
  }

  console.log("  sample data: ok");
  console.log("Seeding complete.");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
