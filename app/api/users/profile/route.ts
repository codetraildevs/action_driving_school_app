 import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/auth/jwt";

import { prisma } from "@/lib/prismaDB";
import { resolveTimezoneName } from "@/lib/auth/timezone";

/**
 * Fields the authenticated user may change on their OWN profile. Anything
 * else sent by a client (id, role, phoneNumber, password, isActive…) is
 * ignored — identity and privileges must never be editable from here.
 */
const EDITABLE_PROFILE_FIELDS = [
  "firstName",
  "middleName",
  "lastName",
  "email",
  "dob",
  "profilePicture",
] as const;

export async function PUT(request: NextRequest) {
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

    const body = await request.json().catch(() => null);
    if (!body || typeof body !== "object") {
      return NextResponse.json({ success: false, error: "Invalid request body" }, { status: 400 });
    }

    // Only copy whitelisted fields; the update is always scoped to the
    // token's own userId — a user can never modify another account.
    const data: Record<string, unknown> = {};
    for (const field of EDITABLE_PROFILE_FIELDS) {
      if (body[field] !== undefined && body[field] !== null) {
        data[field] = body[field];
      }
    }

    if (Object.keys(data).length === 0) {
      return NextResponse.json({ success: false, error: "No editable fields provided" }, { status: 400 });
    }

    await prisma.user.update({
      where: { id: userId },
      data,
    });

    // Re-read the full profile so the response shape matches GET exactly.
    const user = await prisma.user.findUnique({
      where: { id: userId },
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

    const res = NextResponse.json({
      success: true,
      data: userProfile,
      error: null,
    });
    res.headers.set('Cache-Control', 'no-store, private');
    return res;
  } catch (error) {
    console.error("Profile update error:", error);
    return NextResponse.json(
      { success: false, error: "Internal server error" },
      { status: 500 }
    );
  }
}

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
    const res = NextResponse.json({
      success: true,
      data: userProfile,
      error: null,
    });
    // Private, per-user response: browsers/CDNs/proxies must never reuse a
    // cached profile for a different logged-in user.
    res.headers.set('Cache-Control', 'no-store, private');
    return res;
    
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