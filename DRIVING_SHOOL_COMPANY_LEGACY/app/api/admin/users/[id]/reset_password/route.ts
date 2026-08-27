import { NextRequest, NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';
import { PrismaClient } from '@/lib/generated/prisma';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';

import { prisma } from "@/lib/prismaDB";

const resetPasswordHandler = withPermission(PERMISSIONS.USER_UPDATE)(
  async (req, { params, user }) => {
    try {
      const userId = parseInt(params.id);
      const { password, sendEmail } = await req.json();

      let newPassword = password;
      
      // Generate random password if not provided
      if (!newPassword) {
        newPassword = crypto.randomBytes(16).toString('hex');
      }

      // Hash password
      const hashedPassword = await bcrypt.hash(newPassword, 12);

      await prisma.user.update({
        where: { id: userId },
        data: { password: hashedPassword }
      });

      // Log activity
      await prisma.userActivity.create({
        data: {
          activityType: 'PASSWORD_RESET',
          description: `Password reset for user ${userId} by admin ${user.userId}`,
          userId: user.userId,
        }
      });

      // TODO: Send email with new password if sendEmail is true

      return NextResponse.json({
        success: true,
        message: 'Password reset successfully',
        ...(sendEmail ? {} : { temporaryPassword: newPassword })
      });
    } catch (error) {
      console.error('Reset password error:', error);
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      );
    }
  }
);

export const POST = resetPasswordHandler;

