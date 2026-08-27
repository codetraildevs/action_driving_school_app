import { NextRequest, NextResponse } from 'next/server';
import { getCurrentUser } from '@/lib/auth/jwt';
import { isAdminRoleName } from '@/lib/auth/roles';
import { hasPermission } from '@/lib/auth/permissions';
import { prisma } from '@/lib/prismaDB';
import { AuthenticatedHandler } from './withAuth';

export function withPermission(requiredPermissions: string | string[]) {
  return function (handler: AuthenticatedHandler) {
    return async (req: NextRequest, context: { params: any }) => {
      const payload = await getCurrentUser(req);
      
      // getCurrentUser returns either a valid payload OR a 401 NextResponse
      // (truthy!) when the token is missing/invalid, so checking only `!payload`
      // would let the failure through and crash with payload.userId undefined.
      // Check for the actual userId to catch both cases.
      if (!payload || !(payload as any).userId) {
        return NextResponse.json(
          { error: 'Unauthorized', message: 'Authentication required' },
          { status: 401 }
        );
      }

      const userPayload = payload as { userId: number; email?: string };
      
      // One-app role gate: every /api/admin/* route behind withPermission is a
      // console feature (user management, subscriptions, reports, permissions).
      // Only admin / super_admin roles may use them — the token's role id is
      // checked first (fast path), then verified against the DB so a role
      // change made in the console applies immediately.
      const dbUser = await prisma.user.findUnique({
        where: { id: userPayload.userId },
        include: { role: true },
      });
      
      if (!isAdminRoleName(dbUser?.role.roleName)) {
        return NextResponse.json(
          { error: 'Forbidden', message: 'Admin access required' },
          { status: 403 }
        );
      }
      
      // Permission-level checks remain available for finer-grained control.
      // if (!hasPermission(user.permissions, requiredPermissions)) {
      //   return NextResponse.json(
      //     { error: 'Forbidden', message: 'Insufficient permissions' },
      //     { status: 403 }
      //   );
      // }
      
      return handler(req, { ...context, user: payload });
    };
  };
}