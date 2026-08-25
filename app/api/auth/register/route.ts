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
    console.log(body);

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

    const userAddress = { ...body.address };
    const userDevice = { ...body.device };

    const result = await prisma.$transaction(
      async (tx) => {
        // Check for existing user
        const existingUser = await tx.user.findFirst({
          where: {
            OR: [{ phoneNumber: phoneNumber }],
          },
        });
        const existingDevice = await tx.device.findFirst({
          where: { physicalAddress: userDevice.physicalAddress },
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
            phoneNumber,
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

        const firebaseDevice = await tx.firebaseDevice.findFirst({
          where: { physicalDeviceId: userDevice.physicalAddress },
        });

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
        if (userDevice && Object.keys(userDevice).length > 0) {
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
    return NextResponse.json(
      {
        success: false,
        error: "Internal server error",
      },
      { status: 500 },
    );
  }
}