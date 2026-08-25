import { NextRequest, NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';
import { PrismaClient } from '@/lib/generated/prisma';

import { prisma } from "@/lib/prismaDB";

// Update user permissions
const updatePermissionsHandler = withPermission(PERMISSIONS.USER_UPDATE)(
  async (req, { params, user }) => {
    try {
      const userId = parseInt(params.id);
      const { permissionIds } = await req.json();

      if (!Array.isArray(permissionIds)) {
        return NextResponse.json(
          { error: 'permissionIds must be an array' },
          { status: 400 }
        );
      }

      // Delete existing permissions
      await prisma.userPermission.deleteMany({
        where: { userId }
      });

      // Create new permissions
      await prisma.userPermission.createMany({
        data: permissionIds.map((permissionId: number) => ({
          userId,
          permissionId,
        }))
      });

      // Log activity
      await prisma.userActivity.create({
        data: {
          activityType: 'PERMISSION_UPDATE',
          description: `Permissions updated for user ${userId} by admin ${user.userId}`,
          userId: user.userId,
        }
      });

      return NextResponse.json({
        success: true,
        message: 'Permissions updated successfully'
      });
    } catch (error) {
      console.error('Update permissions error:', error);
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      );
    }
  }
);

export const PUT = updatePermissionsHandler;
