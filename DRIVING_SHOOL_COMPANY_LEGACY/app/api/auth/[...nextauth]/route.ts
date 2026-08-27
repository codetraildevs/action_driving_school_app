// app/api/auth/[...nextauth]/route.ts
import NextAuth, { SessionStrategy } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import axios from "axios";
import { JWT } from "next-auth/jwt";

export const authOptions = {
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        identifier: { 
          label: "Email/Phone", 
          type: "text", 
          placeholder: "email@example.com or phone number" 
        },
        deviceId: { 
          label: "Device", 
          type: "text" 
        },
        password: { 
          label: "Password", 
          type: "password" 
        },
      },
      async authorize(credentials, req) {
        try {
          const res = await axios.post(
            `${process.env.NEXTAUTH_URL}/api/auth/login`,
            {
              identifier: credentials?.identifier,
              deviceId: credentials?.deviceId,
              password: credentials?.password,
            }
          );
          
          const data = res.data;
          
          if (data.success && data.user) {
            return {
              id: String(data.user.id),
              name: data.user.name,
              email: data.user.email,
              role: data.user.role,
              language: data.user.language,
              timezone: data.user.timezone,
              device: credentials?.deviceId || "",
              identifier: credentials?.identifier || "",
              image: data.user.profilePicture || null,
            };
          }
          
          return null;
        } catch (error) {
          console.error("Auth error:", error);
          return null;
        }
      },
    }),
  ],
  callbacks: {
    async jwt({
      token,
      user,
      trigger,
      session,
    }: {
      token: JWT;
      user?: {
        id?: string;
        name?: string | null;
        email?: string | null;
        role?: number;
        language?: string | null;
        timezone?: string | null;
        identifier?: string | null;
        device?: string | null;
        image?: string | null;
        [key: string]: any;
      };
      trigger?: "signIn" | "signUp" | "update";
      session?: {
        id?: string;
        name?: string | null;
        role?:number;
        language?: string | null;
        timezone?: string | null;
        identifier?: string | null;
        device?: string | null;
        image?: string | null;
        [key: string]: any;
      };
    }): Promise<JWT> {
      // Initial sign in
      if (user) {
        token.id = user.id || "";
        token.name = user.name || null;
        token.role = user?.role|| -1;
        token.language = user.language || null;
        token.timezone = user.timezone || null;
        token.identifier = user.identifier || null;
        token.device = user.device || null;
        token.image = user.image || null;
      }
      
      // Update session
      if (trigger === "update" && session) {
        token.id = session.id ?? token.id;
        token.name = session.name ?? token.name;
        token.role = session.role ?? token.role;
        token.language = session.language ?? token.language;
        token.timezone = session.timezone ?? token.timezone;
        token.identifier = session.identifier ?? token.identifier;
        token.device = session.device ?? token.device;
        token.image = session.image ?? token.image;
      }
      
      return token;
    },
    
    async session({ 
      session, 
      token 
    }: { 
      session: any; 
      token: JWT;
    }) {
      return {
        ...session,
        user: {
          ...session.user,
          id: token.id as string,
          name: token.name as string | null,
          role: token.role ,
          language: token.language as string | null,
          timezone: token.timezone as string | null,
          device: token.device as string | null,
          identifier: token.identifier as string | null,
          image: token.image as string | null,
        },
      };
    },
  },
  session: {
    strategy: "jwt" as SessionStrategy,
    maxAge: 30 * 24 * 60 * 60, // 30 days
  },
  pages: {
    signIn: "/",
    error: "/auth/error",
  },
  secret: process.env.NEXTAUTH_SECRET,
};

const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };