// GET single user
import {  NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';
import { z } from 'zod';
 
import { prisma } from "@/lib/prismaDB";
const getUserHandler = withPermission(PERMISSIONS.USER_READ)(
  async (req, { params, user }) => {
    try {
      const userId = parseInt(params.id);

      const userData = await prisma.user.findUnique({
        where: { id: userId },
        include: {
          role: true,
          language: true,
          addresses: true,
          devices: true,
          userSubscription: {
            include: {
              subscriptionPlan: true
            }
          },
          userPermissions: {
            include: {
              permission: true
            }
          },
          userTimezone: {
            include: {
              timezone: true
            }
          }
        }
      });

      if (!userData) {
        return NextResponse.json(
          { error: 'User not found' },
          { status: 404 }
        );
      }

      // Remove sensitive data
      const { password, ...userWithoutPassword } = userData;

      return NextResponse.json({
        success: true,
        data: userWithoutPassword
      });
    } catch (error) {
      console.error('Fetch user error:', error);
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      );
    }
  }
);

// UPDATE user
const updateUserSchema = z.object({
  firstName: z.string().optional(),
  lastName: z.string().optional(),
  email: z.string().optional(),
  phoneNumber: z.string().optional(),
  isActive: z.boolean().optional(),
  roleId: z.number().optional(),
  languageId: z.number().optional(),
});

const updateUserHandler = withPermission(PERMISSIONS.USER_UPDATE)(
  async (req, { params, user }) => {
    try {
      const userId = parseInt(params.id);
      const body = await req.json();
      const validated = updateUserSchema.parse(body);
      console.log('Validated update data:', validated);

      // Prevent self-deactivation for admins
      if (userId === user.userId && validated.isActive === false) {
        return NextResponse.json(
          { error: 'Cannot deactivate your own account' },
          { status: 400 }
        );
      }

      const updatedUser = await prisma.user.update({
        where: { id: userId },
        data: validated,
        select: {
          id: true,
          firstName: true,
          middleName:true,
          lastName: true,
          email: true,
          phoneNumber: true,
          isActive: true,
          role: true,
          language: true,
        }
      });

      // Log activity
      await prisma.userActivity.create({
        data: {
          activityType: 'USER_UPDATE',
          description: `User ${userId} updated by admin ${user.userId}`,
          userId: user.userId,
        }
      });

      return NextResponse.json({
        success: true,
        data: updatedUser
      });
    } catch (error) {
      if (error instanceof z.ZodError) {
        return NextResponse.json(
          { error: 'Validation error', details: error },
          { status: 400 }
        );
      }
      console.error('Update user error:', error);
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      );
    }
  }
);

// Delete the account and record the administrator's action atomically.
const deleteUserHandler = withPermission(PERMISSIONS.USER_DELETE)(
  async (req, { params, user }) => {
    try {
      const userId = Number((await params).id);
      if (!Number.isSafeInteger(userId) || userId <= 0) {
        return NextResponse.json({ error: 'Invalid user ID' }, { status: 400 });
      }

      // Prevent self-deletion
      if (userId === user.userId) {
        return NextResponse.json(
          { error: 'Cannot delete your own account' },
          { status: 400 }
        );
      }

      await prisma.$transaction(async (tx) => {
        await tx.user.delete({ where: { id: userId }, select: { id: true } });
        await tx.userActivity.create({
          data: {
            activityType: 'USER_DELETE',
            description: `User ${userId} deleted by admin ${user.userId}`,
            userId: user.userId,
          },
        });
      });

      return NextResponse.json({
        success: true,
        message: 'User deleted successfully'
      });
    } catch (error) {
      const code = (error as { code?: string })?.code;
      if (code === 'P2025') {
        return NextResponse.json({ error: 'User not found' }, { status: 404 });
      }
      if (code === 'P2003') {
        return NextResponse.json({ error: 'Linked records prevent deleting this account.' }, { status: 409 });
      }
      console.error('Delete user error:', error);
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      );
    }
}
)

export const DELETE = deleteUserHandler;
export const GET = getUserHandler;
export const PATCH = updateUserHandler;
export const PUT = updateUserHandler;
