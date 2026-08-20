import { NextRequest, NextResponse } from 'next/server';
import { getCurrentUser,  TokenPayload as JWTPayload } from '@/lib/auth/jwt';

export type AuthenticatedHandler = (
  req: NextRequest,
  context: { params: any; user: any }
) => Promise<NextResponse>;

export function withAuth(handler: AuthenticatedHandler) {
  return async (req: NextRequest, context: { params: any }) => {
    const user = await getCurrentUser(req);
    
    if (!user) {
      return NextResponse.json(
        { error: 'Unauthorized', message: 'Authentication required' },
        { status: 401 }
      );
    }
    
    return handler(req, { ...context, user });
  };
}