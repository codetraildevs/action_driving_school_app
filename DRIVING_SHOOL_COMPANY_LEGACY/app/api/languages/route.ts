import { NextResponse } from "next/server";
import { prisma } from "@/lib/prismaDB";
import { SupportedLanguage } from "@/lib/generated/prisma";

export async function GET() {
  try {
    const languages = await prisma.language.findMany({
      where: {
        languageCode: {
          in: [...Object.values(SupportedLanguage)],
        },
      },
      orderBy: { languageCode: "asc" },
    });

    return NextResponse.json(
      { success: true, data: languages },
      { status: 200 }
    );
  } catch (error) {
    console.error("Error fetching languages:", error);
    return NextResponse.json(
      { success: false, error: "Failed to fetch languages" },
      { status: 500 }
    );
  }
}
