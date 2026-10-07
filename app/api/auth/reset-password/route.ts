// POST /api/auth/reset-password — step 3 of the password reset flow.
//
// Consumes a ForgetPasswordRequest identified by the 6-digit code that
// verify-otp already validated, re-checks its TTL inside the same transaction
// that rotates the password, and then:
//   • updates the user's bcrypt password
//   • ends all active Sessions (mobile)
//   • logs a userActivity "password_reset"
//   • deletes the reset request (single use)
//   • best-effort "password changed" notification email
import { NextRequest, NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { prisma } from "@/lib/prismaDB";
import { RESET_TOKEN_TTL_MS } from "@/lib/auth/jwt";
import {
  recordResetFailure,
  resetFailuresExceeded,
  clearResetFailures,
} from "@/lib/auth/reset-guard";
import { sendPasswordChangedEmail } from "@/lib/email";

const resetPasswordSchema = z
  .object({
    token: z.string().regex(/^\d{6}$/, "Invalid code format"),
    resetRequestId: z.coerce.number().int().positive(),
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
    const { token, resetRequestId, newPassword, confirmPassword } =
      resetPasswordSchema.parse(body);

    // Load the pending request by the opaque resetRequestId handle the UI
    // got from forgot-password. The request id alone is NOT sufficient: the
    // 6-digit token must also match, and the TTL must hold, so an attacker
    // who somehow knows/brushes the id (small integers) still cannot reset
    // without the code. Failures are counted in the same brute-force guard
    // as verify-otp — guessing a 6-digit code here is not a free retry loop.
    const forgetRequest = await prisma.forgetPasswordRequest.findUnique({
      where: { id: resetRequestId },
    });

    if (
      !forgetRequest ||
      forgetRequest.token !== token ||
      Date.now() - new Date(forgetRequest.requestAt).getTime() > RESET_TOKEN_TTL_MS ||
      resetFailuresExceeded(`${forgetRequest.id}`)
    ) {
      if (forgetRequest) {
        // Either the token was wrong or the request expired/burned.
        if (Date.now() - new Date(forgetRequest.requestAt).getTime() > RESET_TOKEN_TTL_MS) {
          await prisma.forgetPasswordRequest.delete({ where: { id: forgetRequest.id } }).catch(() => {});
        } else if (forgetRequest.token !== token) {
          recordResetFailure(`${forgetRequest.id}`);
        }
        // resetFailuresExceeded(hits) case: leave as-is; verify-otp deletes.
      }
      return NextResponse.json(
        { success: false, message: "Invalid or expired code." },
        { status: 404 }
      );
    }

    // NOTE: User.email is NOT declared unique in the Prisma schema (only id
    // and phoneNumber are), so an email lookup must be findFirst. The reset
    // request row itself is unique per email, and forgot-password writes the
    // request against the active account's stored email, so this resolves to
    // the one account the code was issued for.
    const user = await prisma.user.findFirst({
      where: { email: forgetRequest.email },
      orderBy: { id: "asc" },
    });

    if (!user || !user.isActive) {
      return NextResponse.json(
        {
          success: false,
          message:
            "Account for this code not found or inactive. Contact an administrator.",
        },
        { status: 404 }
      );
    }


    const result = await prisma.$transaction(async (tx) => {
      // Re-read the request INSIDE the transaction, then re-check that it is
      // still within TTL. Two verifications + reset calls racing each other
      // resolve to exactly one winner: Prisma throws if the row vanished,
      // and here the first commit deletes it.
      const live = await tx.forgetPasswordRequest.findUnique({
        where: { id: resetRequestId },
      });
      if (!live || live.id !== forgetRequest.id) {
        return { status: 404 as const, message: "Invalid or expired code." };
      }
      if (
        Date.now() - new Date(live.requestAt).getTime() > RESET_TOKEN_TTL_MS
      ) {
        await tx.forgetPasswordRequest.delete({ where: { id: live.id } });
        return { status: 404 as const, message: "Code expired. Request a new one and try again." };
      }
      const hashedPassword = await bcrypt.hash(newPassword, 10);
      const fresh = await tx.user.update({
        where: { id: user!.id },
        data: { password: hashedPassword },
      });

      // End all active sessions (force re-login on mobile).
      await tx.session.updateMany({
        where: { userId: user!.id, endedAt: null },
        data: { endedAt: new Date() },
      });

      // Log password change activity.
      await tx.userActivity.create({
        data: {
          userId: user!.id,
          activityType: "password_reset",
          description: "Password was reset successfully",
        },
      });

      // Consume the request (single use).
      await tx.forgetPasswordRequest.delete({ where: { id: live.id } });

      return { status: 200 as const, userId: fresh.id };
    });

    if (result.status !== 200) {
      return NextResponse.json(
        { success: false, message: result.message },
        { status: result.status }
      );
    }

    // Best-effort confirmation email (never fails the reset).
    try {
      await sendPasswordChangedEmail(
        user.email!,
        `${user.firstName}${user.lastName ? " " + user.lastName : ""}`
      );
    } catch {
      // Email is optional; ignore.
    }

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
