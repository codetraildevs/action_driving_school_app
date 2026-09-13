import { NextRequest, NextResponse } from "next/server";
import { getGoogleAccessToken } from "@/lib/google-oauth";

// Never cache: attestation must always be fresh.
export const dynamic = "force-dynamic";

/**
 * Play Integrity verification endpoint (mobile app → backend).
 *
 * The Android app (IntegrityHelper) POSTs { integrityToken, nonce } here after
 * obtaining the token from Google. This route independently verifies the token
 * with Google's Play Integrity API, then applies a verdict policy:
 *
 *   - the nonce supplied by the app must match the request hash Google stamped
 *     into the token (proves this exact request was attested),
 *   - the app must be PLAY_RECOGNIZED (installed from Play, not tampered),
 *   - the device must meet at least MEETS_DEVICE_INTEGRITY (not rooted/emulated
 *     beyond Google's own detection).
 *
 * Protected by middleware (requires a Bearer token), so only signed-in app
 * users can call it. Tune `acceptedDeviceVerdicts` for your risk appetite.
 *
 * Setup: the Play Integrity API does NOT accept API keys — this route
 * authenticates with the FIREBASE_* service account (OAuth2 JWT flow, see
 * lib/google-oauth.ts). The service account's Google Cloud project must have
 * the "Google Play Integrity API" enabled and be linked to the Play listing.
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const integrityToken: string | undefined = body?.integrityToken;
    const nonce: string | undefined = body?.nonce; // base64 (NO_WRAP)

    if (!integrityToken || !nonce) {
      return NextResponse.json(
        { verified: false, error: "missing_token_or_nonce" },
        { status: 400 }
      );
    }

    const accessToken = await getGoogleAccessToken();
    if (!accessToken) {
      return NextResponse.json(
        { verified: false, error: "server_not_configured" },
        { status: 500 }
      );
    }

    // Play Integrity v1: POST /v1/{package}:decodeIntegrityToken with an
    // OAuth2 bearer token (API keys are rejected by this API). The verdict
    // comes back inside { tokenPayloadExternal: … }.
    const packageName = process.env.INTEGRITY_PACKAGE_NAME || "com.drivingschoolrwandaapp";
    const googleRes = await fetch(
      `https://playintegrity.googleapis.com/v1/${encodeURIComponent(packageName)}:decodeIntegrityToken`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({ integrity_token: integrityToken }),
      }
    );

    if (!googleRes.ok) {
      const googleError = await googleRes.text();
      console.error("Play Integrity Google verify failed", googleRes.status, googleError);
      return NextResponse.json(
        { verified: false, error: "google_verify_failed" },
        { status: 502 }
      );
    }

    const verdictEnvelope = await googleRes.json();
    // decodeIntegrityToken returns { tokenPayloadExternal: { …verdict fields… } }
    const verdict = verdictEnvelope?.tokenPayloadExternal ?? verdictEnvelope;

    // Google echoes the request hash (base64) it stamped at request time;
    // it must equal the nonce the app generated for this call.
    const requestHash: string | undefined = verdict?.requestDetails?.requestHash;
    const nonceMatches =
      !!nonce &&
      !!requestHash &&
      Buffer.from(nonce, "base64").equals(Buffer.from(requestHash, "base64"));

    const appIntegrity = verdict?.appIntegrity ?? {};
    const deviceIntegrity = verdict?.deviceIntegrity ?? {};
    const accountDetails = verdict?.accountDetails ?? {};

    const appVerdict: string = appIntegrity.appRecognitionVerdict ?? "UNEVALUATED";
    const deviceVerdict: string = deviceIntegrity.deviceRecognitionVerdict ?? "UNEVALUATED";
    const licensingVerdict: string = accountDetails.appLicensingVerdict ?? "UNEVALUATED";

    // Verdict policy — tune as needed.
    const acceptedDeviceVerdicts = ["MEETS_DEVICE_INTEGRITY", "MEETS_STRONG_INTEGRITY"];
    const verified =
      nonceMatches &&
      appVerdict === "PLAY_RECOGNIZED" &&
      acceptedDeviceVerdicts.includes(deviceVerdict);

    return NextResponse.json({
      verified,
      requestId: verdict?.requestDetails?.requestId ?? null,
      nonceMatch: nonceMatches,
      appVerdict,
      deviceVerdict,
      licensingVerdict,
      // x-user-id is injected by the middleware from the Bearer token — useful
      // for logging which user attested.
      userId: request.headers.get("x-user-id") ?? null,
    });
  } catch (e) {
    console.error("Integrity verify route error", e);
    return NextResponse.json(
      { verified: false, error: "invalid_request" },
      { status: 400 }
    );
  }
}
