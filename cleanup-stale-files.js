const { PrismaClient } = require("./lib/generated/prisma");
const fs = require("fs");
const path = require("path");

const prisma = new PrismaClient();

async function cleanup() {
  const files = await prisma.file.findMany();
  let deleted = 0;
  let kept = 0;

  for (const file of files) {
    const fullPath = path.join(process.cwd(), "public", file.filePath);
    if (!fs.existsSync(fullPath)) {
      await prisma.file.delete({ where: { id: file.id } });
      deleted++;
      console.log("Deleted:", file.name);
    } else {
      kept++;
    }
  }

  console.log("\n--- Summary ---");
  console.log("Deleted (missing files):", deleted);
  console.log("Kept (files exist):", kept);
  await prisma.$disconnect();
}

cleanup().catch((err) => {
  console.error("Error:", err);
  process.exit(1);
});
