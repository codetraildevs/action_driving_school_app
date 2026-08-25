/**
 * Create a test PdfFile record in the database.
 *
 * Run with:
 *   node prisma/create-test-pdf.js
 *
 * Reads DATABASE_URL from .env.
 */
const { PrismaClient } = require("../lib/generated/prisma");

const prisma = new PrismaClient();

async function main() {
  // Find an existing user to be the uploader (admin user id=2)
  const user = await prisma.user.findUnique({ where: { id: 2 } });
  if (!user) {
    console.error("User id=2 not found.");
    process.exit(1);
  }

  // Find a language (English = id 41)
  const language = await prisma.language.findFirst({
    where: { languageCode: "en" },
  });
  if (!language) {
    console.error("English language not found.");
    process.exit(1);
  }

  const pdf = await prisma.pdfFile.create({
    data: {
      title: "Rwanda Driving Handbook 2026",
      filePath: "/uploads/files/test-driving-handbook.pdf",
      author: "Rwanda Transport Board",
      totalPages: 120,
      uploadedBy: user.id,
      uploadedAt: new Date(),
      description: "Official driving handbook for Rwanda covering road signs, traffic rules, and safe driving practices.",
      isPublic: true,
      languageId: language.id,
    },
  });

  console.log("✔ Created PdfFile record:");
  console.log(`  Title: ${pdf.title}`);
  console.log(`  ID: ${pdf.id}`);
  console.log(`  Author: ${pdf.author}`);
  console.log(`  Pages: ${pdf.totalPages}`);
  console.log(`  Public: ${pdf.isPublic}`);
}

main()
  .catch((e) => {
    console.error("Error:", e);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
