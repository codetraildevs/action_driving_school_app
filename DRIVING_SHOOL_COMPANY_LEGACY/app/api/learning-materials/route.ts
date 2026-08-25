 
import { NextRequest, NextResponse } from 'next/server';
import { PrismaClient } from '@/lib/generated/prisma';

import { prisma } from "@/lib/prismaDB";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get('page') || '1');
    const limit = parseInt(searchParams.get('limit') || '10');
    const search = searchParams.get('search') || '';
    const isPublic = searchParams.get('isPublic');
    
    const skip = (page - 1) * limit;
    
    const where = {
      AND: [
        search ? {
          OR: [
            { title: { contains: search, mode: 'insensitive' } },
            { description: { contains: search, mode: 'insensitive' } }
          ]
        } : {},
        isPublic !== null ? { isPublic: isPublic === 'true' } : {}
      ]
    };
    
    const [materials, total] = await Promise.all([
      prisma.learningMaterial.findMany({
        where,
        skip,
        take: limit,
        orderBy: { createdAt: 'desc' },
     
      }),
      prisma.learningMaterial.count({ where })
    ]);
    
    return NextResponse.json({
      materials,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error('Error fetching learning materials:', error);
    return NextResponse.json(
      { error: 'Failed to fetch learning materials' },
      { status: 500 }
    );
  }
}