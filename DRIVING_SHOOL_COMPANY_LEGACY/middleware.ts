import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Routes that are genuinely public — reachable without any auth token.
 *
 * IMPORTANT: this list must stay exact. Never add a bare "/" here: a single
 * "/" makes `pathname.startsWith("/")` true for every path, which silently
 * disables auth for the whole app (that was the original bug — the middleware
 * never actually checked anything).
 */
const publicExact = new Set([
  // Auth entry points (login/registration flows must be token-free)
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/refresh',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
  '/api/auth/verify-otp',
  // Public content & mobile-app data
  '/api/health',
  '/api/languages',
  '/api/folders',
  '/api/files',
  '/api/questions',
  '/api/sse',
  '/api/irembo/driving',
  '/api/irembo/special',
  '/api/privacy-policy',
  '/api/privacy-policy/versions',
  '/api/terms-of-service',
  '/api/terms-of-service/versions',
  '/api/learning-materials',
]);

/**
 * Public GET sub-paths under otherwise-protected prefixes (e.g. reading one
 * file / question / material / Irembo request). Non-GET methods stay
 * protected, so mutations like PUT /api/files/{id}, DELETE /api/questions/{id}
 * or POST /api/learning-materials/upload still require a valid token.
 */
const publicGetPrefixes = [
  '/api/files/',
  '/api/questions/',
  '/api/learning-materials/',
  '/api/irembo/driving/',
  '/api/irembo/special/',
];

function isPublic(pathname: string, method: string): boolean {
  if (publicExact.has(pathname)) return true;
  if (method === 'GET' && publicGetPrefixes.some((p) => pathname.startsWith(p))) {
    return true;
  }
  return false;
}

// ---------------------------------------------------------------------------
// Edge-compatible HS256 JWT verification.
//
// Middleware runs on the Edge runtime, where the jsonwebtoken library (Node
// crypto) cannot verify signatures — verifyToken() from lib/auth/jwt silently
// returned null there, so every token was rejected (web console login loop,
// Android app stuck on loading). WebCrypto (crypto.subtle) is available in
// both the Edge and Node runtimes, so this verifies the same HS256 token the
// login route signs with jsonwebtoken.
// ---------------------------------------------------------------------------
function base64UrlDecode(input: string): ArrayBuffer {
  const b64 = input.replace(/-/g, '+').replace(/_/g, '/');
  const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4);
  const bin = atob(padded);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer as ArrayBuffer;
}

async function verifyTokenEdge(
  token: string,
  secret: string
): Promise<{ userId?: number; email?: string } | null> {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const [headerB64, payloadB64, signatureB64] = parts;
    const data = new TextEncoder().encode(headerB64 + '.' + payloadB64);

    const key = await crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(secret),
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['verify']
    );
    const valid = await crypto.subtle.verify(
      'HMAC',
      key,
      base64UrlDecode(signatureB64),
      data
    );
    if (!valid) return null;

    const payloadJson = new TextDecoder().decode(base64UrlDecode(payloadB64));
    const payload = JSON.parse(payloadJson) as {
      userId?: number;
      email?: string;
      exp?: number;
    };
    // Reject expired tokens (exp is in seconds).
    if (payload.exp && payload.exp * 1000 < Date.now()) return null;
    return payload;
  } catch {
    return null;
  }
}

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Allow public routes
  if (isPublic(pathname, request.method)) {
    return NextResponse.next();
  }

  // Check authentication
  const authHeader =
    request.headers.get('authorization') || request.headers.get('Authorization');
  const token = authHeader?.startsWith('Bearer ')
    ? authHeader.substring(7)
    : request.cookies.get('auth_token')?.value;

  if (!token) {
    return NextResponse.json(
      {
        success: false,
        error: 'Unauthorized',
        message: 'Authentication required',
      },
      { status: 401 }
    );
  }

  const payload = await verifyTokenEdge(token, process.env.ACCESS_TOKEN_SECRET || '');
  if (!payload || !payload.userId) {
    return NextResponse.json(
      {
        success: false,
        error: 'Unauthorized',
        message: 'Invalid or expired token',
      },
      { status: 401 }
    );
  }

  // Add user info to headers for downstream routes
  const requestHeaders = new Headers(request.headers);
  requestHeaders.set('x-user-id', payload.userId.toString());
  requestHeaders.set('x-user-email', payload.email ?? '');

  return NextResponse.next({
    request: {
      headers: requestHeaders,
    },
  });
}

export const config = {
  // Enforce on API routes only. The admin pages are client-rendered and do
  // their own token handling (localStorage + redirect to /admin/login); the
  // pages also use server actions which carry no Bearer header, so including
  // page paths here would break the admin console.
  matcher: ['/api/:path*'],
};
