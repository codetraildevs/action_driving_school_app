 import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/auth/jwt";

import { prisma } from "@/lib/prismaDB";
import { resolveTimezoneName } from "@/lib/auth/timezone";

export async function GET(request: NextRequest) {
  try {
    
    const authHeader = request.headers.get('authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json({ success: false, error: 'Unauthorized: Missing or malformed token' }, { status: 401 });
    }
    const token = authHeader.substring(7);

     
    const payload = await verifyToken(token);
    if (!payload || !payload.userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized: Invalid or expired token' }, { status: 401 });
    }
    
   
    const userId = payload.userId;

 
    const user = await prisma.user.findUnique({
      where: {
        id: userId,
      },
      include: {
        role: true,
        language: true,
        userTimezone: {
          include: {
            timezone: true,
          },
        },
        userTestAccess: true
      },
    });

    if (!user) {
      return NextResponse.json(
        { success: false, error: "User not found" },
        { status: 404 }
      );
    }

    // Resolve timezone (junction table → direct FK → UTC)
    const timezoneName = await resolveTimezoneName(user.userTimezone, user.timezoneId);

    const userProfile = {
      id: user.id,
      firstName: user.firstName,
      middleName: user.middleName,
      lastName: user.lastName,
      email: user.email,
      phoneNumber: user.phoneNumber,
      profilePicture: user.profilePicture,
      role: user.roleId,
      roleName: user.role.roleName,
      languageId:user.languageId,
      language: user.language.languageCode,
      timezone: timezoneName,
      createdAt: user.createdAt.toISOString(),
      userTestAccess:user.userTestAccess
    };
    console.log(userProfile)

    return NextResponse.json({
      success: true,
      data: userProfile,
      error: null,
    });
    
  } catch (error) {
    if (error instanceof Error && error.name === 'TokenExpiredError') {
      // This is where your Android app will get a 401 and trigger the refresh logic
      return NextResponse.json({ success: false, error: "Session expired" }, { status: 401 });
    }
    console.error("Profile fetch error:", error);
    return NextResponse.json(
      { success: false, error: "Internal server error" },
      { status: 500 }
    );
  }
}