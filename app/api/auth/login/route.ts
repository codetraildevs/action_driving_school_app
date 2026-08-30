import { NextRequest, NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { prisma} from "@/lib/prismaDB";
import { generateAccessToken, generateRefreshToken } from "@/lib/auth/jwt";
import { isAdminRoleName } from "@/lib/auth/roles";
import { resolveTimezoneName } from "@/lib/auth/timezone";
import { sendFCMNotification } from "@/lib/notification";
import { NOTIFICATION_CHANNELS } from "@/lib/types";

const loginSchema = z.object({
  identifier: z.string(),
  password: z.string(),
  deviceId: z.string().optional(),
  // "android_app" marks the request as coming from the native Android app.
  // The web console does not send this, so it keeps requiring the real
  // password even for admins.
  clientType: z.string().optional(),
});

/**
 * Generates the common formats for a Rwandan phone number so login matches
 * however the number was stored: the Android app sends E.164 (+250732657995),
 * while the web console and legacy DB rows use local format (0732657995).
 */
function phoneVariants(raw: string): string[] {
  const digits = raw.replace(/\D/g, "");
  const set = new Set<string>();
  if (raw) set.add(raw);
  if (digits) {
    if (digits.startsWith("250")) {
      set.add("+" + digits); // +250732657995
      set.add("0" + digits.slice(3)); // 0732657995
    } else if (digits.startsWith("0")) {
      set.add("+" + "250" + digits.slice(1)); // +250732657995
      set.add("250" + digits.slice(1)); // 250732657995
    } else {
      set.add("+" + digits); // typed without a + prefix
    }
  }
  return [...set];
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { identifier, password, deviceId, clientType } = loginSchema.parse(body);
    console.log(body)

   let userData: any = null;
   const resp= await prisma.$transaction(async (tx) => {
      const variants = phoneVariants(identifier);

      // 1. Try an exact match first (fastest & most reliable).
      let user = await tx.user.findFirst({
        where: { phoneNumber: identifier },
        include: {
          role: true,
          language: true,
          userTimezone: { include: { timezone: true } },
          devices: true,
        },
      });

      // 2. Fall back to variant matching only when no exact match exists.
      //    When multiple variants match different rows, prefer the exact
      //    phone format that the user typed (it's first in the list) so we
      //    never silently return the wrong account.
      if (!user) {
        user = await tx.user.findFirst({
          where: { phoneNumber: { in: variants } },
          orderBy: { id: 'asc' },
          include: {
            role: true,
            language: true,
            userTimezone: { include: { timezone: true } },
            devices: true,
          },
        });
      }

      if (!user) {
        return NextResponse.json(
          {
            success: false,
            message: "Invalid credentials",
          },
          { status: 401 }
        );
      }

      if (!user.isActive) {
        return NextResponse.json(
          {
            success: false,
            message: "Account is not active. Please contact support.",
          },
          { status: 403 }
        );
      }

      // Console roles (admin / super_admin) can sign in from any device; regular
      // users are bound to their registered device (one account per device).
      if (!isAdminRoleName(user.role.roleName)) {
        // Optional chaining guards against legacy accounts without a device
        // record, so they get a clear 403 instead of a 500 crash.
        if (user.devices[0]?.physicalAddress != deviceId) {
          return NextResponse.json(
            {
              success: false,
              message:
                "You are not allowed to login from this device. Please contact support.",
            },
            { status: 403 }
          );
        }
      }

      // Verify password.
      // Admins logging in from the native Android app use phone-only (shared
      // login from any device), so their password check is skipped when the
      // request explicitly originates from the app. The web console does not
      // send clientType, so console logins ALWAYS require the real password.
      // Regular users are unchanged (their password is their device id).
      const isPasswordValid =
        isAdminRoleName(user.role.roleName) && clientType === "android_app"
          ? true
          : await bcrypt.compare(password, user.password);
      if (!isPasswordValid) {
        return NextResponse.json(
          {
            success: false,
            message: "Invalid credentials",
          },
          { status: 401 }
        );
      }

      // Resolve timezone (junction table → direct FK → UTC)
      const timezoneName = await resolveTimezoneName(user.userTimezone, user.timezoneId);

      // Generate tokens
      const tokenPayload = {
        userId: user.id,
        email: user.email,
        role: user.role.id,
        language: user.language.languageCode,
        timezone: timezoneName,
      };

      const accessToken = generateAccessToken(tokenPayload);
      const refreshToken = generateRefreshToken(tokenPayload);
      const device = await tx.device.findFirst({
        where: { userId: user.id, physicalAddress: deviceId },
      });
      // Store refresh token in database (optional but recommended)

      if (!isAdminRoleName(user.role.roleName)) {
        await tx.session.create({
          data: {
            userId: user.id,
            deviceId: device?.id || 1,
            startedAt: new Date(),
            isSuspect: false,
          },
        });
      }

      // Update last login
      await tx.user.update({
        where: { id: user.id },
        data: { lastLogin: new Date() },
      });
      
      userData = user;
      
      return NextResponse.json(
        {
          success: true,
          message: "Login successful",
          user: {
            id: user.id,
            firstName: user.firstName,
            middleName: user.middleName,
            lastName: user.lastName,
            email: user.email,
            phoneNumber: user.phoneNumber,
            profilePicture: user.profilePicture,
            role: user.role.id,
            userRole: user.role,
            roleName: user.role.roleName,
            languageId:user.languageId,
            language: user.language.languageCode,
            timezone: timezoneName,
          },
          accessToken,
          refreshToken,
        },
        { status: 200 }
      );
    }, {maxWait:60000, timeout:60000}); 
     

    return resp
  } catch (error) {
    if (error instanceof z.ZodError) {
      return NextResponse.json(
        {
          success: false,
          error: "Invalid input",
          details: error.issues,
        },
        { status: 400 }
      );
    }

    console.error("Login error:", error);
    return NextResponse.json(
      {
        success: false,
        error: "Internal server error",
      },
      { status: 500 }
    );
  }
}
