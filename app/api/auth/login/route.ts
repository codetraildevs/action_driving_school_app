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
    // Never log the body here: it contains the raw password.

   let userData: any = null;
   const resp= await prisma.$transaction(async (tx) => {
      const variants = phoneVariants(identifier);

      // Collect EVERY account matching the typed number in any supported
      // format, in typed-format priority (exact first, then +250…, then 250…).
      //
      // Why not pick one row before the credential checks? The production
      // database contains duplicate account pairs for the same phone number
      // stored in different formats (legacy 07… rows next to newer +250…
      // rows — see diag-phone-audit.js). Committing to a single row before
      // verifying password/device can authenticate the WRONG person and issue
      // their token — exactly the reported "logged in as A, profile shows B"
      // bug. Identity is only known after the credential checks, so every
      // candidate is evaluated and the first one that passes device +
      // password checks wins.
      const fetchCandidateRows = (phone: string) =>
        tx.user.findMany({
          where: { phoneNumber: phone },
          include: {
            role: true,
            language: true,
            userTimezone: { include: { timezone: true } },
            devices: true,
          },
          orderBy: { id: "asc" },
        });

      const candidates: Awaited<ReturnType<typeof fetchCandidateRows>> = [];
      const seenIds = new Set<number>();
      for (const variant of variants) {
        for (const row of await fetchCandidateRows(variant)) {
          if (!seenIds.has(row.id)) {
            seenIds.add(row.id);
            candidates.push(row);
          }
        }
      }

      if (candidates.length === 0) {
        return NextResponse.json(
          {
            success: false,
            message: "Invalid credentials",
          },
          { status: 401 }
        );
      }

      // Console roles (admin / super_admin) can sign in from any device; regular
      // users are bound to their registered device (one account per device).
      // Device matching considers ALL of the user's registered devices —
      // checking only devices[0] silently ignored every device after the first.
      let user: (typeof candidates)[number] | null = null;
      let sawInactive = false;
      let sawDeviceMismatch = false;
      let sawBadPassword = false;
      for (const candidate of candidates) {
        if (!candidate.isActive) {
          sawInactive = true;
          continue;
        }
        if (!isAdminRoleName(candidate.role.roleName)) {
          const deviceMatch = deviceId && candidate.devices.some((d) => d.physicalAddress === deviceId);
          if (!deviceMatch) {
            continue;
          }
        }
        // Verify password. Admins logging in from the native Android app use
        // phone-only (shared login from any device), so their password check
        // is skipped when the request explicitly originates from the app. The
        // web console does not send clientType, so console logins ALWAYS
        // require the real password. Regular users are unchanged (their
        // password is their device id).
        const isPasswordValid =
          isAdminRoleName(candidate.role.roleName) && clientType === "android_app"
            ? true
            : await bcrypt.compare(password, candidate.password);
        if (!isPasswordValid) {
          sawBadPassword = true;
          continue;
        }
        user = candidate;
        break;
      }

      if (!user) {
        // No candidate passed the credential checks. Preserve the original
        // single-account error precedence: inactive → device → credentials.
        if (sawInactive && candidates.every((c) => !c.isActive)) {
          return NextResponse.json(
            {
              success: false,
              message: "Account is not active. Please contact support.",
            },
            { status: 403 }
          );
        }
        if (sawDeviceMismatch && !sawBadPassword) {
          return NextResponse.json(
            {
              success: false,
              message:
                "You are not allowed to login from this device. Please contact support.",
            },
            { status: 403 }
          );
        }
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
