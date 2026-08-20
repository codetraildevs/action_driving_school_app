import jwt from "jsonwebtoken";
import { NextResponse } from "next/server";

const ACCESS_TOKEN_SECRET = process.env.ACCESS_TOKEN_SECRET!;
const REFRESH_TOKEN_SECRET = process.env.REFRESH_TOKEN_SECRET!;
const RESET_TOKEN_SECRET =
  process.env.RESET_TOKEN_SECRET || ACCESS_TOKEN_SECRET;

export interface TokenPayload {
  userId: number;
  email: string | null;
  role: number;
  language: string;
  timezone: string;
}

export interface ResetTokenPayload {
  userId: number;
  email: string;
}

export function generateAccessToken(payload: TokenPayload): string {
  return jwt.sign(payload, ACCESS_TOKEN_SECRET, {
    expiresIn: "30d", // 15 minutes
  });
}

export function generateRefreshToken(payload: TokenPayload): string {
  return jwt.sign(payload, REFRESH_TOKEN_SECRET, {
    expiresIn: "60d", // 7 days
  });
}

export function verifyAccessToken(token: string): TokenPayload | null {
  try {
    return jwt.verify(token, ACCESS_TOKEN_SECRET) as TokenPayload;
  } catch (error) {
    return null;
  }
}

export function verifyRefreshToken(token: string): TokenPayload | null {
  try {
    return jwt.verify(token, REFRESH_TOKEN_SECRET) as TokenPayload;
  } catch (error) {
    return null;
  }
}
// export function generateResetToken(payload: ResetTokenPayload): string {
//   return jwt.sign(payload, RESET_TOKEN_SECRET, {
//     expiresIn: "1h",
//   });
// }

export function generateResetToken(payload: ResetTokenPayload): string {
  const array = new Uint32Array(1);
  crypto.getRandomValues(array);
  const token = ((array[0] % 900000) + 100000).toString().padStart(6, "0");
  return token;
}

export async function verifyResetToken(
  token: string
): Promise<ResetTokenPayload | null> {
  try {
    return jwt.verify(token, RESET_TOKEN_SECRET) as ResetTokenPayload;
  } catch (error) {
    return null;
  }
}
export async function verifyToken(token: string): Promise<TokenPayload | null> {
  try {
    return jwt.verify(token, ACCESS_TOKEN_SECRET) as TokenPayload;
  } catch (error) {
    return null;
  }
}

export async function  getCurrentUser(request:any) {
   const authHeader = request.headers.get('authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json({ success: false, error: 'Unauthorized: Missing or malformed token' }, { status: 401 });
    }
    const token = authHeader.substring(7);

     
    const payload = await verifyToken(token);
    if (!payload || !payload.userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized: Invalid or expired token' }, { status: 401 });
    }
    return payload
    
  
}
