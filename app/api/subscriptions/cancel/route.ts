import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
 ;
import { verifyToken } from "@/lib/auth/jwt";

import { prisma } from "@/lib/prismaDB";


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
      // Delete user subscription
      await tx.userSubscription.delete({
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