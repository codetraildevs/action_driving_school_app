// app/api/privacy-policy/versions/route.ts
 import { NextRequest, NextResponse } from "next/server";

import { prisma } from "@/lib/prismaDB";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const language = searchParams.get("language") || "en";

    const versions = await prisma.privacyPolicy.findMany({
      where: {
        language
      },
      select: {
        id: true,
        version: true,
        title: true,
        appVersion: true,
        language: true,
        isActive: true,
        createdAt: true,
        updatedAt: true
      },
      orderBy: { createdAt: "desc" }
    });

    return NextResponse.json({ data: versions });
  } catch (error) {
    console.error("Error fetching privacy policy versions:", error);
    return NextResponse.json(
      { error: "Failed to fetch privacy policy versions" },
      { status: 500 }
    );
  }
}