'use server';

import { PrismaClient } from '@/lib/generated/prisma';
import { revalidatePath } from 'next/cache';
import { cookies } from 'next/headers';
import { getCurrentUser } from '@/lib/auth/jwt';
import { hasPermission, PERMISSIONS } from '@/lib/auth/permissions';
import { unlink } from 'fs/promises';
import { join } from 'path';

import { prisma } from "@/lib/prismaDB";

async function verifyContentPermission() {
  const cookieStore = cookies();
  const token = (await cookieStore).get('auth_token');
  
  if (!token) {
    throw new Error('Unauthorized');
  }

  const user = await getCurrentUser({ 
    headers: new Headers(), 
    cookies: cookieStore 
  } as any);
  
  if (!user || !hasPermission(user.permissions, PERMISSIONS.PDF_UPDATE)) {
    throw new Error('Forbidden: Insufficient permissions');
  }

  return user;
}

export async function togglePdfVisibility(pdfId: number, isPublic: boolean) {
  try {
    const admin = await verifyContentPermission();

    await prisma.pdfFile.update({
      where: { id: pdfId },
      data: { isPublic }
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'PDF_VISIBILITY_UPDATE',
        description: `PDF ${pdfId} set to ${isPublic ? 'public' : 'private'}`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/pdfs');
    return { success: true };
  } catch (error) {
    console.error('Toggle PDF visibility error:', error);
    return { error: 'Failed to update PDF visibility' };
  }
}

export async function bulkDeletePdfs(pdfIds: number[]) {
  try {
    const admin = await verifyContentPermission();

    if (!hasPermission(admin.permissions, PERMISSIONS.PDF_DELETE)) {
      return { error: 'Insufficient permissions' };
    }

    // Get PDF file paths
    const pdfs = await prisma.pdfFile.findMany({
      where: { id: { in: pdfIds } },
      select: { id: true, filePath: true, title: true }
    });

    // Delete files from filesystem
    await Promise.allSettled(
      pdfs.map(async (pdf) => {
        try {
          const filepath = join(process.cwd(), 'public', pdf.filePath);
          await unlink(filepath);
        } catch (error) {
          console.error(`Failed to delete file for PDF ${pdf.id}:`, error);
        }
      })
    );

    // Delete database records
    await prisma.pdfFile.deleteMany({
      where: { id: { in: pdfIds } }
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'BULK_PDF_DELETE',
        description: `${pdfIds.length} PDFs deleted`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/pdfs');
    return { success: true, count: pdfIds.length };
  } catch (error) {
    console.error('Bulk delete PDFs error:', error);
    return { error: 'Failed to delete PDFs' };
  }
}

export async function updatePdfMetadata(
  pdfId: number,
  data: {
    title?: string;
    author?: string;
    description?: string;
    languageId?: number;
  }
) {
  try {
    const admin = await verifyContentPermission();

    const pdf = await prisma.pdfFile.update({
      where: { id: pdfId },
      data,
    });

    // Log activity
    await prisma.userActivity.create({
      data: {
        activityType: 'PDF_UPDATE',
        description: `PDF "${pdf.title}" metadata updated`,
        userId: admin.userId,
      }
    });

    revalidatePath('/admin/pdfs');
    revalidatePath(`/admin/pdfs/${pdfId}`);
    return { success: true };
  } catch (error) {
    console.error('Update PDF metadata error:', error);
    return { error: 'Failed to update PDF metadata' };
  }
}
