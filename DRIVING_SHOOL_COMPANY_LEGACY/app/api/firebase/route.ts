import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prismaDB";

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { deviceId, firebaseToken } = body;
    if (!deviceId || !firebaseToken) {
      return NextResponse.json(
        { error: "Missing required fields" },
        { status: 400 },
      );
    }

    await prisma.firebaseDevice.upsert({
      where: {
        physicalDeviceId: deviceId,
      },
      create: {
        deviceToken: firebaseToken,
        physicalDeviceId: deviceId,
      },
      update: {
        deviceToken: firebaseToken,
        physicalDeviceId: deviceId,
      },
    });

    return NextResponse.json(
      {
        message: `Firebase device created successfully`,
      },
      { status: 201 },
    );
  } catch (error) {
    console.error("Error creating firebase device:", error);
    return NextResponse.json(
      { error: "Failed to create test" },
      { status: 500 },
    );
  }
}
