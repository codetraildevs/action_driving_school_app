// POST /api/auth/verify-otp — step 2 of the password reset flow.
//
// The code the user typed is checked against ForgetPasswordRequest AND against
// its 15-minute TTL (RESET_TOKEN_TTL_MS). Outcomes are signalled via JSON
// `success`/`verified` fields with HTTP 200 so SPA clients can read messages.
//
// Brute-force protection: lib/auth/reset-guard counts failed attempts per
// code-holder for a window; after too many failures the pending request is
// DELETED, forcing a brand-new code to be requested.
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { prisma } from "@/lib/prismaDB";
import { RESET_TOKEN_TTL_MS } from "@/lib/auth/jwt";
import {
  recordResetFailure,
  resetFailuresExceeded,
  clearResetFailures,
} from "@/lib/auth/reset-guard";

// Schema for validating the OTP verification request. `identifier` accepts
// whichever handle the UI obtained from forgot-password: the raw email
// (email path) or the numeric resetRequestId string (phone path).
const verifyOtpSchema = z.object({
  // identifier is the handle the UI holds: the account email OR the numeric
  // resetRequestId (may be a single digit, e.g. "8" — hence min(1)).
  identifier: z.string().min(1),
  otp: z.string().regex(/^\d{6}$/, "Enter the 6-digit code"),
});

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { identifier, otp } = verifyOtpSchema.parse(body);

    // 1. Resolve the pending request from the handle the UI has.
    //    • numeric identifier  → ForgetPasswordRequest.id (phone path)
    //    • email identifier    → the unique email column (email path)
    const passwordResetRequest = /^\d+$/.test(identifier)
      ? await prisma.forgetPasswordRequest.findUnique({
          where: { id: parseInt(identifier, 10) },
        })
      : await prisma.forgetPasswordRequest.findUnique({
          where: { email: identifier },
        });

    if (!passwordResetRequest || !passwordResetRequest.active) {
      return NextResponse.json(
        {
          success: false,
          verified: false,
          message: "Invalid or expired code. Request a new one.",
        },
        { status: 200 }
      );
    }

    // 2. Check the TTL window.
    const elapsedMs = Date.now() - new Date(passwordResetRequest.requestAt).getTime();
    if (elapsedMs > RESET_TOKEN_TTL_MS) {
      // Deactivate the expired token to prevent reuse.
      await prisma.forgetPasswordRequest.update({
        where: { id: passwordResetRequest.id },
        data: { active: false },
      });
      return NextResponse.json(
        {
          success: false,
          verified: false,
          message: "Code has expired. Please request a new one.",
        },
        { status: 200 }
      );
    }

    // The holder key is the email the request is stored under.
    const holderKey = `${passwordResetRequest.id}`;

    // 3. Brute-force guard: too many wrong codes → burn the request.
    if (resetFailuresExceeded(holderKey)) {
      await prisma.forgetPasswordRequest.delete({
        where: { id: passwordResetRequest.id },
      });
      clearResetFailures(holderKey);
      return NextResponse.json(
        {
          success: false,
          verified: false,
          message: "Too many wrong attempts. Please request a new code.",
        },
        { status: 200 }
      );
    }

    // 4. Compare codes.
    if (passwordResetRequest.token !== otp.trim()) {
      const count = recordResetFailure(holderKey);
      return NextResponse.json(
        {
          success: false,
          verified: false,
          message: `Wrong code (${count} wrong attempt${count === 1 ? "" : "s"}).`,
        },
        { status: 200 }
      );
    }

    // 5. Correct → clear the failure counter. The request stays active so the
    //    final reset-password call can consume it (identified by email+token).
    clearResetFailures(holderKey);
    return NextResponse.json(
      {
        success: true,
        verified: true,
        message: "Code verified.",
        // Echoed back ONLY after successful verification; the client passes it
        // to /api/auth/reset-password as the token.
        data: passwordResetRequest.token,
      },
      { status: 200 }
    );
  } catch (error) {
    if (error instanceof z.ZodError) {
      return NextResponse.json(
        { success: false, error: "Invalid input", details: error.issues },
        { status: 400 }
      );
    }
    console.error("Verify OTP error:", error);
    return NextResponse.json(
      { success: false, error: "Internal server error" },
      { status: 500 }
    );
  }
}
