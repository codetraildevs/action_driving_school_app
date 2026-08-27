// app/api/user-learning-materials/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { PrismaClient } from '@/lib/generated/prisma';

import { prisma } from "@/lib/prismaDB";

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    
    const { userId, learningMaterialId, localPath, expiresAt } = body;
    
    if (!userId || !learningMaterialId) {
      return NextResponse.json(
        { error: 'userId and learningMaterialId are required' },
        { status: 400 }
      );
    }
    
    // Check if material exists and is public
    const material = await prisma.learningMaterial.findUnique({
      where: { id: learningMaterialId }
    });
    
    if (!material) {
      return NextResponse.json(
        { error: 'Learning material not found' },
        { status: 404 }
      );
    }
    
    if (!material.isPublic) {
      return NextResponse.json(
        { error: 'This learning material is not available for download' },
        { status: 403 }
      );
    }
    
    const userMaterial = await prisma.userLearningMaterial.create({
      data: {
        userId,
        learningMaterialId,
        downloadedAt: new Date(),
        localPath,
        expiresAt: expiresAt ? new Date(expiresAt) : null
      },
      include: {
        learningMaterial: true,
        user: {
          select: { firstName: true, lastName: true, email: true }
        }
      }
    });
    
    return NextResponse.json(userMaterial, { status: 201 });
  } catch (error) {
    console.error('Error assigning learning material to user:', error);
    return NextResponse.json(
      { error: 'Failed to assign learning material' },
      { status: 500 }
    );
  }
}