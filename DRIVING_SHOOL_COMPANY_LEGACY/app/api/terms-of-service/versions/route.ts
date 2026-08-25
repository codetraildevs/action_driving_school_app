// app/api/terms-of-service/versions/route.ts
 import { NextRequest, NextResponse } from "next/server";

import { prisma } from "@/lib/prismaDB";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const language = searchParams.get("language") || "en";

    const versions = await prisma.termsOfService.findMany({
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
    console.error("Error fetching terms of service versions:", error);
    return NextResponse.json(
      { error: "Failed to fetch terms of service versions" },
      { status: 500 }
    );
  }
}