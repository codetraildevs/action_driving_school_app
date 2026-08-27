import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
 ;

import { prisma } from "@/lib/prismaDB";

// Schema for validating the OTP verification request
const verifyOtpSchema = z.object({
  email: z.string().email("Invalid email address"),
  otp: z.string().min(1, "OTP is required"), // Your OTP might be a 6-digit code
});

// Define an expiration time for the OTP in minutes
const OTP_EXPIRATION_MINUTES = 15;

export async function POST(request: NextRequest) {
  try {
    // 1. Parse and validate the request body
    const body = await request.json();
    const { email, otp } = verifyOtpSchema.parse(body);

    // 2. Find the password reset request in the database
    // Note: In your schema, the OTP is stored in the 'token' field.
    const passwordResetRequest = await prisma.forgetPasswordRequest.findFirst({
      where: {
        email: email,
        token: otp,
      },
    });

    // 3. Check if the request is valid and active
    if (!passwordResetRequest || !passwordResetRequest.active) {
      return NextResponse.json(
        {
          success: false,
          message: "Invalid or expired OTP. Please request a new one.",
        },
        { status: 400 }
      );
    }

    // 4. Check if the OTP has expired based on its creation time
    const now = new Date();
    const requestTime = new Date(passwordResetRequest.requestAt);
    const timeDifference = (now.getTime() - requestTime.getTime()) / (1000 * 60); // Difference in minutes

    if (timeDifference > OTP_EXPIRATION_MINUTES) {
       // Deactivate the expired token to prevent reuse
       await prisma.forgetPasswordRequest.update({
        where: { id: passwordResetRequest.id },
        data: { active: false },
      });

      return NextResponse.json(
        {
          success: false,
          message: "OTP has expired. Please request a new one.",
        },
        { status: 400 }
      );
    }

    // 5. If verification is successful, return a success response.
    // The response includes the token itself, which the client will use in the
    // final step to reset the password.
    return NextResponse.json(
      {
        success: true,
        message: "OTP verified successfully.",
        data: passwordResetRequest.token,
      },
      { status: 200 }
    );

  } catch (error) {
    if (error instanceof z.ZodError) {
      // Handle validation errors from Zod
      return NextResponse.json(
        {
          success: false,
          error: "Invalid input",
          details: error.issues,
        },
        { status: 400 }
      );
    }

    // Handle other internal errors
    console.error("Verify OTP error:", error);
    return NextResponse.json(
      {
        success: false,
        error: "Internal server error",
      },
      { status: 500 }
    );
  }
}