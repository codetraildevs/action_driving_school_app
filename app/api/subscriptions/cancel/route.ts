import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
 ;import { verifyToken } from "@/lib/auth/jwt";
import { prisma } from "@/lib/prismaDB";
import { UserTestAccessStatus } from "@/lib/generated/prisma";


export async function POST(request: NextRequest) {
  try {
        const authHeader = request.headers.get('authorization');
     if (!authHeader || !authHeader.startsWith('Bearer ')) {
       return NextResponse.json({ success: false, error: 'Unauthorized: Missing or malformed token' }, { status: 401 });
     }
     const token = authHeader.substring(7);
 
     const payload = await verifyToken(token);
     if (!payload || !payload.userId) {
       return NextResponse.json({ success: false, error: 'Unauthorized: Invalid or expired token' }, { status: 401 });
     }
     
     const userId = payload.userId;

    await prisma.$transaction(async (tx) => {
      // Revoke learning access in the REAL ledger: the content gate in
      // app/api/tests/[id]/questions/route.ts checks userTestAccess.status,
      // so flipping it to INACTIVE (and clearing maxTest) is what actually
      // locks the user out of tests. updateMany (not update) so cancelling
      // still succeeds for a user with no access row.
      await tx.userTestAccess?.updateMany({
        where: { userId },
        data: {
          status: UserTestAccessStatus.INACTIVE,
          maxTest: 0,
        },
      });

      // Legacy cleanup. deleteMany (not delete): most users have no
      // user_subscriptions row, and delete would throw P2025 -> 500 for them.
      await tx.userSubscription.deleteMany({
        where: { userId:userId }
      });

      // Remove all user permissions
      await tx.userPermission.deleteMany({
        where: { userId:userId }
      });

      // Log activity
      await tx.userActivity.create({
        data: {
          userId:userId,
          activityType: 'SUBSCRIPTION_CANCEL',
          description: 'Cancelled subscription'
        }
      });
    });

    return NextResponse.json({ 
      success: true, 
      message: 'Subscription cancelled successfully' 
    });
  } catch (error) {
    console.error('Cancel subscription error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}