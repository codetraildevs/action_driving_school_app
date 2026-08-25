// app/api/user-learning-materials/[id]/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { PrismaClient } from '@/lib/generated/prisma';

import { prisma } from "@/lib/prismaDB";

export async function DELETE(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const userMaterial = await prisma.userLearningMaterial.findUnique({
      where: { id: parseInt(params.id) }
    });
    
    if (!userMaterial) {
      return NextResponse.json(
        { error: 'User learning material not found' },
        { status: 404 }
      );
    }
    
    await prisma.userLearningMaterial.delete({
      where: { id: parseInt(params.id) }
    });
    
    return NextResponse.json({ message: 'Learning material removed from user' });
  } catch (error) {
    console.error('Error removing learning material from user:', error);
    return NextResponse.json(
      { error: 'Failed to remove learning material from user' },
      { status: 500 }
    );
  }
}