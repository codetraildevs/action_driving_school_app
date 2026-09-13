import { NextRequest, NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { prisma } from "@/lib/prismaDB";
import { UserTestAccessStatus } from "@/lib/generated/prisma";
import { NOTIFICATION_CHANNELS } from "@/lib/types";
import { sendFCMNotification } from "@/lib/notification";

// Self-registration is STRICTLY student-only. There is no admin registration
// path — console roles (admin / super_admin) are provisioned by existing
// admins only. The client-supplied role is intentionally ignored.
const STUDENT_ROLE_ID = 5;

/**
 * Canonical storage format for Rwandan phone numbers: local `07XXXXXXXX`
 * (10 digits, no spaces). Registration normalizes every submission to this
 * ONE format so the same number can never exist as two rows (`07…` AND
 * `+250…`). That duplicate-format pair is exactly what caused the
 * cross-user profile leak — production was deduped on 2026-09-13; this
 * prevents new pairs from forming.
 * Returns null for anything that is not a valid Rwandan number.
 */
function normalizeRwandaPhone(raw: string): string | null {
  const digits = raw.replace(/\D/g, "");
  if (!digits) return null;
  let local: string;
  if (digits.startsWith("250")) {
    local = "0" + digits.slice(3); // +250732657995 → 0732657995
  } else if (digits.startsWith("0")) {
    local = digits; // 0732657995 → unchanged
  } else {
    local = "0" + digits; // 732657995 → 0732657995
  }
  return /^0\d{9}$/.test(local) ? local : null;
}

const loginSchema = z.object({
  firstName: z.string(),
  middleName: z.string().optional(),
  lastName: z.string(),
  dob: z.string().optional(),
  phoneNumber: z.string(),
  isActive: z.boolean().default(true),
  profilePicture: z.string().optional(),
  email: z.string().optional(),
  password: z.string().min(6),
  language: z.string(),
  timezone: z.string(),
  device: z
    .object({
      physicalAddress: z.string(),
      manufacturer: z.string(),
      model: z.string(),
      name: z.string(),
    })
    .optional(),
});

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    // Never log the raw body: it contains the password.

    const {
      firstName,
      middleName,
      lastName,
      dob,
      phoneNumber,
      isActive,
      profilePicture,
      email,
      password,
      language,
      timezone,
      device,
    } = loginSchema.parse(body);

    // Normalize the phone BEFORE any uniqueness check so a number already
    // registered in the other format is correctly detected as taken.
    const normalizedPhone = normalizeRwandaPhone(phoneNumber);
    if (!normalizedPhone) {
      return NextResponse.json(
        {
          success: false,
          message:
            "Invalid Rwandan phone number. Use e.g. 0732657995 or +250732657995.",
        },
        { status: 400 },
      );
    }

    const userAddress = { ...body.address };
    const userDevice = { ...body.device };

    const result = await prisma.$transaction(
      async (tx) => {
        // Check for existing user — the stored format is already canonical,
        // so an exact match is a true duplicate across all formats.
        const existingUser = await tx.user.findFirst({
          where: {
            OR: [{ phoneNumber: normalizedPhone }],
          },
        });
        // Guard: Prisma treats an undefined where-value as "no filter" —
        // a device-less registration would match the FIRST device row in
        // the table and falsely 409 (and send the FCM welcome to a random
        // device). Only run the device check when a device was provided.
        const deviceAddress = typeof userDevice?.physicalAddress === "string" ? userDevice.physicalAddress.trim() : "";
        if (deviceAddress) {
          const existingDevice = await tx.device.findFirst({
            where: { physicalAddress: deviceAddress },
          });

          if (existingDevice) {
            return {
              error: true,
              response: NextResponse.json(
                {
                  success: false,
                  message: "This device already has associated user",
                },
                { status: 409 },
              ),
            };
          }
        }

        if (existingUser) {
          return {
            error: true,
            response: NextResponse.json(
              {
                success: false,
                message: "User with that phone number already exists",
              },
              { status: 409 },
            ),
          };
        }

        // New accounts are always Students — never admins.
        const userRole = await tx.userRole.findFirst({
          where: { id: STUDENT_ROLE_ID },
        });

        if (!userRole) {
          return {
            error: true,
            response: NextResponse.json(
              {
                success: false,
                message: "Student role not found in database",
              },
              { status: 500 },
            ),
          };
        }

        // Validate language exists
        const userLanguage = await tx.language.findFirst({
          where: { languageCode: language },
        });

        if (!userLanguage) {
          return {
            error: true,
            response: NextResponse.json(
              {
                success: false,
                message: "Invalid language specified",
              },
              { status: 400 },
            ),
          };
        }

        // Validate timezone exists
        const userTimezone = await tx.timezone.findFirst({
          where: { timezoneName: timezone },
        });

        if (!userTimezone) {
          return {
            error: true,
            response: NextResponse.json(
              {
                success: false,
                message: "Invalid timezone specified",
              },
              { status: 400 },
            ),
          };
        }

        // Hash password
        const hashedPwd = await bcrypt.hash(password, await bcrypt.genSalt());

        // Create user
        const user = await tx.user.create({
          data: {
            firstName,
            lastName,
            middleName,
            phoneNumber: normalizedPhone,
            isActive: true,
            email,
            roleId: userRole.id,
            languageId: userLanguage.id,
            timezoneId: userTimezone.id,
            profilePicture,
            password: hashedPwd,
          },
        });
        
        const expiresAt = new Date();
        expiresAt.setDate(expiresAt.getDate() + 30);

        await tx.userTestAccess.create({
          data: {
            userId: user.id,
            maxTest: 0,
            expiresAt,
            status: UserTestAccessStatus.ACTIVE,
          },
        });

        // Also create the junction-table record so login/profile can
        // resolve the timezone via the userTimezone relation.
        await tx.userTimezone.create({
          data: {
            userId: user.id,
            timezoneId: userTimezone.id,
          },
        });

        if (userAddress && Object.keys(userAddress).length > 0) {
          await tx.address.create({
            data: {
              userId: user.id,
              ...userAddress,
            },
          });
        }

        const firebaseDevice = deviceAddress
          ? await tx.firebaseDevice.findFirst({
              where: { physicalDeviceId: deviceAddress },
            })
          : null;

        if (firebaseDevice) {
          let notificationMessage;
          let notificationTitle;
          let userLanguageCode = userLanguage.languageCode;
          switch (userLanguageCode) {
            case "fr":
              notificationMessage =
                "Bienvenue sur l'application Action Driving School ! Votre compte a été créé avec succès.";
              notificationTitle = "Bienvenue!";
              break;
            case "es":
              notificationMessage =
                "¡Bienvenido a la aplicación Action Driving School! Su cuenta ha sido creada con éxito.";
              notificationTitle = "¡Bienvenido!";
              break;
            case "rw":
              notificationMessage =
                "Murakaza neza kuri Action Driving School! Gufungura konti byagenze neza.";
              notificationTitle = "Ikaze!";
              break;
            default:
              notificationMessage =
                "Welcome to Action Driving School App! Your account has been successfully created.";
              notificationTitle = "Welcome!";
          }

          sendFCMNotification(
            firebaseDevice.deviceToken,
            notificationTitle,
            notificationMessage,
            {
              channel_id: NOTIFICATION_CHANNELS.GENERAL,
            },
          );
        }

        // Create device if provided
        if (deviceAddress) {
          await tx.device.create({
            data: {
              userId: user.id,
              ...userDevice,
            },
          });
        }
        
        return { error: false, user };
      },
      { maxWait: 60000, timeout: 60000 },
    );

    // Check if transaction returned an error
    if (result.error) {
      return result.response;
    }

    return NextResponse.json(
      {
        success: true,
        message: "New user account created successfully!",
      },
      {
        status: 201,
      },
    );
  } catch (error) {
    if (error instanceof z.ZodError) {
      return NextResponse.json(
        {
          success: false,
          error: "Invalid input",
          details: error.issues,
        },
        { status: 400 },
      );
    }

    console.error("Registration error:", error);
    // Concurrent registrations of the same number lose the unique-index
    // race: surface it as the same 409 the pre-check produces.
    if ((error as { code?: string })?.code === "P2002") {
      return NextResponse.json(
        {
          success: false,
          message: "User with that phone number already exists",
        },
        { status: 409 },
      );
    }
    return NextResponse.json(
      {
        success: false,
        error: "Internal server error",
      },
      { status: 500 },
    );
  }
}