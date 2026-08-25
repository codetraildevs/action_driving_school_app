// app/api/admin/system-settings/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const getSettingsHandler = withPermission(PERMISSIONS.SETTINGS_READ)(
  async () => {
  try {
    const settings = await prisma.systemSetting.findMany({
      orderBy: { createdAt: "desc" }
    });

    return NextResponse.json({ data: settings });
  } catch (error) {
    console.error("Error fetching system settings:", error);
    return NextResponse.json(
      { error: "Failed to fetch system settings" },
      { status: 500 }
    );
  }
  }
);

export const GET = getSettingsHandler;

const createSettingHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (request: NextRequest) => {
  try {
    const { settingKey, settingValue, description } = await request.json();

    if (!settingKey || !settingValue) {
      return NextResponse.json(
        { error: "Setting key and value are required" },
        { status: 400 }
      );
    }

    // Check if setting already exists
    const existingSetting = await prisma.systemSetting.findFirst({
      where: { settingKey }
    });

    if (existingSetting) {
      return NextResponse.json(
        { error: "Setting with this key already exists" },
        { status: 400 }
      );
    }

    const setting = await prisma.systemSetting.create({
      data: {
        settingKey,
        settingValue,
        description: description || ""
      }
    });

    return NextResponse.json({ data: setting }, { status: 201 });
  } catch (error) {
    console.error("Error creating system setting:", error);
    return NextResponse.json(
      { error: "Failed to create system setting" },
      { status: 500 }
    );
  }
  }
);

export const POST = createSettingHandler;