
import { NextRequest, NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';
import { prisma } from '@/lib/prismaDB';

// Full user record lookup (sensitive data) — admin only. The mobile app uses
// /api/users/profile for its own record; nothing user-facing calls this.
const getUserHandler = withPermission(PERMISSIONS.USER_READ)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
  try {
    const userId = parseInt(params.id);
    const user = await prisma.user.findUnique({
      where: { id: userId },
    });

    if (!user) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    return NextResponse.json(user);
  } catch (error) {
    console.error('Get user error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
  }
);

export const GET = getUserHandler;

// Both admin endpoints share the same permission checks, cleanup and audit entry.
export { DELETE } from '@/app/api/admin/users/[id]/route';
