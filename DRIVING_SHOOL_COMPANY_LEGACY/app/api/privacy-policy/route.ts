// app/api/privacy-policy/route.ts
 import { NextRequest, NextResponse } from "next/server";

import { prisma } from "@/lib/prismaDB";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const appVersion = searchParams.get("version") || "latest";
    const language = searchParams.get("language") || "en";
    const version = searchParams.get("v"); // Specific version request

    let policy;

    if (version) {
      // Get specific version
      policy = await prisma.privacyPolicy.findFirst({
        where: {
          version,
          language,
          ...(appVersion !== "latest" && { appVersion })
        }
      });
    } else if (appVersion === "latest") {
      // Get latest active policy
      policy = await prisma.privacyPolicy.findFirst({
        where: {
          isActive: true,
          language
        },
        orderBy: { createdAt: "desc" }
      });
    } else {
      // Get policy for specific app version
      policy = await prisma.privacyPolicy.findFirst({
        where: {
          appVersion,
          language,
          isActive: true
        }
      });
    }

    if (!policy) {
      return NextResponse.json(
        { error: "Privacy policy not found" },
        { status: 404 }
      );
    }

    return NextResponse.json({ 
      data: {
        id: policy.id,
        version: policy.version,
        title: policy.title,
        content: policy.content,
        appVersion: policy.appVersion,
        language: policy.language,
        createdAt: policy.createdAt,
        updatedAt: policy.updatedAt
      }
    });
  } catch (error) {
    console.error("Error fetching privacy policy:", error);
    return NextResponse.json(
      { error: "Failed to fetch privacy policy" },
      { status: 500 }
    );
  }
}