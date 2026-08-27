import { NextRequest, NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { z } from "zod";
 ;
import { verifyResetToken } from "@/lib/auth/jwt";

import { prisma } from "@/lib/prismaDB";

const resetPasswordSchema = z
  .object({
    token: z.string(),
    newPassword: z.string().min(6, "Password must be at least 6 characters"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
  });

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { token, newPassword, confirmPassword } =
      resetPasswordSchema.parse(body);

    if (!token) {
      return NextResponse.json(
        {
          success: false,
          message: "Invalid or expired reset token",
        },
        { status: 401 }
      );
    }

    const forgetRequest = await prisma.forgetPasswordRequest.findFirst({
      where: { token: token },
    });

    if (!forgetRequest) {
      return NextResponse.json(
        {
          success: false,
          message: "No reset password request found",
        },
        { status: 404 }
      );
    }

    const user = await prisma.user.findUnique({
      where: { email: forgetRequest.email },
    });
    if (!user || !user.isActive) {
      return NextResponse.json(
        {
          success: false,
          message: "User not found or inactive",
        },
        { status: 404 }
      );
    }

    // Hash new password
    const hashedPassword = await bcrypt.hash(
      newPassword,
      await bcrypt.genSalt(10)
    );

    // Update password
    await prisma.user.update({
      where: { id: user.id },
      data: { password: hashedPassword },
    });

    // End all active sessions (force re-login)
    await prisma.session.updateMany({
      where: {
        userId: user.id,
        endedAt: null,
      },
      data: {
        endedAt: new Date(),
      },
    });

    // Log password change activity
    await prisma.userActivity.create({
      data: {
        userId: user.id,
        activityType: "password_reset",
        description: "Password was reset successfully",
      },
    });

    await prisma.forgetPasswordRequest.delete({
      where: {  email:user.email },
    });

    return NextResponse.json(
      {
        success: true,
        message:
          "Password reset successfully. Please login with your new password.",
      },
      { status: 200 }
    );
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

    console.error("Reset password error:", error);
    return NextResponse.json(
      {
        success: false,
        error: "Internal server error",
      },
      { status: 500 }
    );
  }
}
