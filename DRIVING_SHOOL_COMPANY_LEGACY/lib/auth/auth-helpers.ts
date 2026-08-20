import { NextRequest } from "next/server";
import { verifyAccessToken } from "./jwt";

export function getTokenFromHeader(request: NextRequest): string | null {
  const authHeader = request.headers.get("authorization") || request.headers.get("Authorization") ;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return null;
  }
  return authHeader.substring(7);
}

export function getUserFromToken(request: NextRequest) {
  const token = getTokenFromHeader(request);
  if (!token) {
    return null;
  }
  return verifyAccessToken(token);
}