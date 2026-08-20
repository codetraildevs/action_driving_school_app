// app/api/terms-of-service/route.ts
 import { NextRequest, NextResponse } from "next/server";

import { prisma } from "@/lib/prismaDB";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const appVersion = searchParams.get("version") || "latest";
    const language = searchParams.get("language") || "en";
    const version = searchParams.get("v"); // Specific version request

    let terms;

    if (version) {
      // Get specific version
      terms = await prisma.termsOfService.findFirst({
        where: {
          version,
          language,
          ...(appVersion !== "latest" && { appVersion })
        }
      });
    } else if (appVersion === "latest") {
      // Get latest active terms
      terms = await prisma.termsOfService.findFirst({
        where: {
          isActive: true,
          language
        },
        orderBy: { createdAt: "desc" }
      });
    } else {
      // Get terms for specific app version
      terms = await prisma.termsOfService.findFirst({
        where: {
          appVersion,
          language,
          isActive: true
        }
      });
    }

    if (!terms) {
      return NextResponse.json(
        { error: "Terms of service not found" },
        { status: 404 }
      );
    }

    return NextResponse.json({ 
      data: {
        id: terms.id,
        version: terms.version,
        title: terms.title,
        content: terms.content,
        appVersion: terms.appVersion,
        language: terms.language,
        createdAt: terms.createdAt,
        updatedAt: terms.updatedAt
      }
    });
  } catch (error) {
    console.error("Error fetching terms of service:", error);
    return NextResponse.json(
      { error: "Failed to fetch terms of service" },
      { status: 500 }
    );
  }
}

// API to record user acceptance
export async function POST(request: NextRequest) {
  try {
    const { termsOfServiceId, userId, ipAddress, userAgent } = await request.json();

    if (!termsOfServiceId || !userId) {
      return NextResponse.json(
        { error: "Terms of service ID and user ID are required" },
        { status: 400 }
      );
    }

    // Check if terms exist
    const terms = await prisma.termsOfService.findUnique({
      where: { id: termsOfServiceId }
    });

    if (!terms) {
      return NextResponse.json(
        { error: "Terms of service not found" },
        { status: 404 }
      );
    }

    // Record acceptance
    const acceptance = await prisma.termsOfServiceAcceptance.create({
      data: {
        userId,
        termsOfServiceId,
        ipAddress: ipAddress || null,
        userAgent: userAgent || null
      },
      include: {
        user: {
          select: {
            firstName: true,
            lastName: true,
            email: true
          }
        },
        termsOfService: {
          select: {
            version: true,
            title: true
          }
        }
      }
    });

    return NextResponse.json({ data: acceptance }, { status: 201 });
  } catch (error) {
    console.error("Error recording terms acceptance:", error);
    return NextResponse.json(
      { error: "Failed to record terms acceptance" },
      { status: 500 }
    );
  }
}