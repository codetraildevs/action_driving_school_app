"use server";

import { prisma } from "@/lib/prismaDB";
import { SupportedLanguage } from "@/lib/generated/prisma";

export async function getLanguages() {
  try {
    const languages = await prisma.language.findMany({
      where: {
        languageCode: {
          in: [...Object.values(SupportedLanguage)],
        },
      },
      orderBy: { languageCode: "asc" },
    });

    return { success: true, data: languages };
  } catch (error) {
    console.error("Error fetching languages:", error);
    return { success: false, error: "Failed to fetch languages" };
  }
}
