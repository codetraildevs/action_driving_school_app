// app/api/admin/system-settings/[id]/route.ts
import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const updateSettingHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { settingKey, settingValue, description } = await request.json();

    if (!settingKey || !settingValue) {
      return NextResponse.json(
        { error: "Setting key and value are required" },
        { status: 400 }
      );
    }

    const setting = await prisma.systemSetting.update({
      where: { id: parseInt(params.id) },
      data: {
        settingKey,
        settingValue,
        description: description || ""
      }
    });

    return NextResponse.json({ data: setting });
  } catch (error) {
    console.error("Error updating system setting:", error);
    return NextResponse.json(
      { error: "Failed to update system setting" },
      { status: 500 }
    );
  }
  }
);

export const PUT = updateSettingHandler;

const deleteSettingHandler = withPermission(PERMISSIONS.SETTINGS_WRITE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    await prisma.systemSetting.delete({
      where: { id: parseInt(params.id) }
    });

    return NextResponse.json({ 
      success: true,
      message: "System setting deleted successfully" 
    });
  } catch (error) {
    console.error("Error deleting system setting:", error);
    return NextResponse.json(
      { error: "Failed to delete system setting" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteSettingHandler;