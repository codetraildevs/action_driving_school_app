import crypto from "crypto";

/**
 * Minimal OAuth2 service-account flow for Google APIs that REJECT API keys
 * (e.g. Play Integrity decodeIntegrityToken). Uses the same FIREBASE_*
 * service-account credentials already present in .env for FCM.
 *
 * - Signs a JWT RS256 with the service-account private key.
 * - Exchanges it at https://oauth2.googleapis.com/token for a bearer token
 *   scoped to the requested Google API.
 * - Caches the token in memory until 60s before its documented expiry.
 *
 * Returns null when the FIREBASE_* credentials are missing/invalid so callers
 * can respond with a clean "server_not_configured" error.
 */

const TOKEN_URI = "https://oauth2.googleapis.com/token";

let cached: { token: string; expiresAtMs: number } | null = null;

function loadServiceAccount(): {
  client_email: string;
  private_key: string;
} | null {
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  let privateKey = process.env.FIREBASE_PRIVATE_KEY;
  if (!clientEmail || !privateKey) return null;
  // .env stores the PEM with literal \n sequences
  privateKey = privateKey.replace(/\\n/g, "\n");
  try {
    // Fail fast on unparseable keys instead of throwing at fetch time
    crypto.createPrivateKey(privateKey);
  } catch {
    return null;
  }
  return { client_email: clientEmail, private_key: privateKey };
}

function base64url(input: Buffer | string): string {
  return Buffer.from(input)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

export async function getGoogleAccessToken(
  scope = "https://www.googleapis.com/auth/playintegrity",
): Promise<string | null> {
  try {
    const now = Date.now();
    if (cached && cached.expiresAtMs - 60_000 > now) return cached.token;

    const sa = loadServiceAccount();
    if (!sa) return null;

    const iat = Math.floor(now / 1000);
    const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
    const claims = base64url(
      JSON.stringify({
        iss: sa.client_email,
        scope,
        aud: TOKEN_URI,
        iat,
        exp: iat + 3600,
      }),
    );
    const signer = crypto.createSign("RSA-SHA256");
    signer.update(`${header}.${claims}`);
    const signature = base64url(signer.sign(sa.private_key));
    const assertion = `${header}.${claims}.${signature}`;

    const res = await fetch(TOKEN_URI, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion,
      }),
    });

    if (!res.ok) {
      console.error("Google OAuth token exchange failed:", res.status, (await res.text()).slice(0, 200));
      return null;
    }
    const data = (await res.json()) as { access_token?: string; expires_in?: number };
    if (!data.access_token) return null;

    cached = {
      token: data.access_token,
      expiresAtMs: now + (data.expires_in ?? 3600) * 1000,
    };
    return cached.token;
  } catch (e) {
    console.error("getGoogleAccessToken error:", e);
    return null;
  }
}
