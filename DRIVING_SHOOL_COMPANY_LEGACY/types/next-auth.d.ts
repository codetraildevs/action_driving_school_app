// types/next-auth.d.ts
import NextAuth, { DefaultSession } from "next-auth";

declare module "next-auth" {
  interface Session {
    user: {
      id: string;
      name: string | null;
      email?: string | null;
      language: string | null;
      timezone: string | null;
      device: string | null;
      identifier: string | null;
      role: number;
      image?: string | null;
    } & DefaultSession["user"];
  }

  interface User {
    id: string;
    name: string | null;
    email?: string | null;
    language: string | null;
    timezone: string | null;
    device: string | null;
    identifier: string | null;
    role: number;
    image?: string | null;
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    id: string;
    name: string | null;
    language: string | null;
    timezone: string | null;
    device: string | null;
    identifier: string | null;
    role: number;
    image?: string | null;
  }
}