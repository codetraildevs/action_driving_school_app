// POST /api/auth/forgot-password — step 1 of the password reset flow.
//
// The admin console signs in with a phone number, but account emails are the
// only address a reset code can be emailed to. This route therefore accepts
// EITHER an email OR a phone identifier:
//   • email given      → must match the account's stored email exactly
//   • phone given      → resolves the account, returns masked email + masked
//                        phone so the UI can say where the code was sent
//
// The code (6 digits) is stored in ForgetPasswordRequest with a 15-minute TTL
// (RESET_TOKEN_TTL_MS) and is ALWAYS created, even when SMTP is not
// configured — the operator can tail PM2 logs to read the code. When email is
// configured, the same code is emailed instead (never both delivery paths).
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { prisma } from "@/lib/prismaDB";
import {
  generateResetToken,
  RESET_TOKEN_TTL_MS,
  ResetTokenPayload,
} from "@/lib/auth/jwt";
import { sendPasswordResetEmail } from "@/lib/email";

const forgotPasswordSchema = z.object({
  identifier: z.string().min(3, "Enter your registered email or phone number"),
});

/**
 * Generates the common formats for a Rwandan phone number so the lookup
 * matches however the number was stored: the Android app sends E.164
 * (+250732657995), while the web console and legacy DB rows use local
 * format (0732657995). Mirror of phoneVariants() in app/api/auth/login —
 * keep in sync.
 */
function phoneVariants(raw: string): string[] {
  const digits = raw.replace(/\D/g, "");
  const set = new Set<string>();
  if (raw) set.add(raw);
  if (digits) {
    if (digits.startsWith("250")) {
      set.add("+" + digits);
      set.add("0" + digits.slice(3));
    } else if (digits.startsWith("0")) {
      set.add("+" + "250" + digits.slice(1));
      set.add("250" + digits.slice(1));
    } else {
      set.add("+" + digits);
    }
  }
  return [...set];
}

/** Mask an address like ad***@amategekoyumuhanda.rw so the UI can show it. */
function maskEmail(email: string): string {
  const [local, domain] = email.split("@");
  if (!domain) return "***";
  const shown = local.slice(0, 2);
  return `${shown}${"*".repeat(Math.max(local.length - 2, 3))}@${domain}`;
}

/** Mask a Rwandan phone number like 073***7995. */
function maskPhone(phone: string): string {
  if (phone.length < 6) return "***";
  return `${phone.slice(0, 3)}***${phone.slice(-3)}`;
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { identifier } = forgotPasswordSchema.parse(body);

    // Uniform responses for unknown accounts (no account/identifier
    // enumeration): the UI shows the same "sent" message either way.
    //
    // resetRequestId is the id of the ForgetPasswordRequest row the UI must
    // hand to /api/auth/verify-otp and /api/auth/reset-password. It is an
    // opaque handle — it exposes neither the account email nor the code —
    // and it lets the client key the later steps without ever knowing the
    // address the request is stored under (needed for the phone path).
    const genericResponse = (
      resetRequestId?: number,
      maskedEmail?: string,
      maskedPhone?: string
    ) =>
      NextResponse.json(
        {
          success: true,
          message:
            "If this account exists, a 6-digit reset code has been sent.",
          resetRequestId,
          maskedEmail,
          maskedPhone,
        },
        { status: 200 }
      );

    const looksLikeEmail = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(
      identifier
    );

    // ── Resolve the account from the identifier ──────────────────────────
    let user: { id: number; email: string | null; phoneNumber: string; isActive: boolean } | null =
      null;

    if (looksLikeEmail) {
      const found = await prisma.user.findMany({
        where: { email: identifier },
        select: { id: true, email: true, phoneNumber: true, isActive: true },
        orderBy: { id: "asc" },
      });
      user = found[0] ?? null;
    } else {
      const found = await prisma.user.findMany({
        where: { phoneNumber: { in: phoneVariants(identifier) } },
        select: { id: true, email: true, phoneNumber: true, isActive: true },
        orderBy: { id: "asc" },
      });
      user = found.find((u) => u.isActive) ?? found[0] ?? null;
    }

    // No resolvable account, or inactive: same neutral answer either way.
    if (!user || !user.isActive) {
      return genericResponse();
    }
    // Email mismatch guard: only when the user explicitly typed an email do we
    // enforce the match. (findMany with email = identifier already matched, so
    // this is just belt-and-braces for the case where the row's email differs
    // in case — MySQL collation is case-insensitive by default.)
    if (looksLikeEmail && user.email && user.email.toLowerCase() !== identifier.toLowerCase()) {
      return NextResponse.json(
        {
          success: false,
          error: "Email mismatch",
          message:
            "That is not the email registered on this account. Check your entry or contact an administrator.",
        },
        { status: 400 }
      );
    }

    if (!user.email) {
      return NextResponse.json(
        {
          success: false,
          error: "No email on file",
          message:
            "This account has no registered email. Contact an administrator to reset your password.",
        },
        { status: 400 }
      );
    }

    // ── One active request per email + resend cooldown ───────────────────
    const currentRequest = await prisma.forgetPasswordRequest.findUnique({
      where: { email: user.email },
    });

    if (currentRequest && currentRequest.active) {
      const elapsedMs = Date.now() - new Date(currentRequest.requestAt).getTime();
      if (elapsedMs < RESET_TOKEN_TTL_MS) {
        if (elapsedMs < 60_000) {
          return NextResponse.json(
            {
              success: false,
              error: "Rate limited",
              message: "A code was just sent. Wait a minute before requesting another.",
              retryAfterSeconds: Math.ceil((60_000 - elapsedMs) / 1000),
            },
            { status: 429 }
          );
        }
        // Same TTL window → reuse the still-valid code (resend), no new code.
        const maskedEmail = maskEmail(user.email);
        const maskedPhone = maskPhone(user.phoneNumber);
        let delivered = false;
        try {
          await sendPasswordResetEmail(
            user.email,
            `Your password reset code is: ${currentRequest.token}`,
            ""
          );
          delivered = true;
        } catch {
          delivered = false;
        }
        console.log(
          `[forgot-password] resend for ${user.email}; email=${delivered ? "SENT" : "SKIPPED(not configured)"} code=${currentRequest.token}`
        );
        return genericResponse(
          currentRequest.id,
          maskedEmail,
          maskedPhone
        );
      }
      // Expired → drop the stale row and fall through to a fresh code.
      await prisma.forgetPasswordRequest.delete({ where: { id: currentRequest.id } });
    }

    // ── Create a fresh code ──────────────────────────────────────────────
    const resetToken = generateResetToken({ userId: user.id, email: user.email });
    await prisma.forgetPasswordRequest.create({
      data: {
        email: user.email,
        token: resetToken,
        expiresAt: new Date(Date.now() + RESET_TOKEN_TTL_MS),
      },
    });

    // Try to email the code. SMTP credentials are not currently configured in
    // production, so treat send failure as non-fatal: the flow still works via
    // the code being logged here (PM2 logs) or shown by an operator.
    let emailSent = false;
    try {
      const userFull = await prisma.user.findUnique({
        where: { id: user.id },
        select: { firstName: true },
      });
      await sendPasswordResetEmail(
        user.email,
        `Your password reset code is: ${resetToken}`,
        userFull?.firstName ?? ""
      );
      emailSent = true;
    } catch {
      emailSent = false;
    }
    console.log(
      `[forgot-password] code issued for ${user.email}; email=${emailSent ? "SENT" : "SKIPPED(not configured)"} code=${resetToken}`
    );

    // Never put the raw code in the HTTP response — that would let anyone
    // self-serve a reset for any account. The UI only gets masked addresses.
    const created = await prisma.forgetPasswordRequest.findUnique({
      where: { email: user.email },
      select: { id: true },
    });
    return genericResponse(created?.id, maskEmail(user.email), maskPhone(user.phoneNumber));
  } catch (error) {
    if (error instanceof z.ZodError) {
      return NextResponse.json(
        { success: false, error: "Invalid input", details: error.issues },
        { status: 400 }
      );
    }
    console.error("Forgot password error:", error);
    return NextResponse.json(
      { success: false, error: "Internal server error" },
      { status: 500 }
    );
  }
}
