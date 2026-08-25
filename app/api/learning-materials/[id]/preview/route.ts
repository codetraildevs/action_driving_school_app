// app/api/learning-materials/[id]/preview/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { createReadStream, stat } from 'fs/promises';
import { join } from 'path';
import { PrismaClient } from '@/lib/generated/prisma';

import { prisma } from "@/lib/prismaDB";

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const material = await prisma.learningMaterial.findUnique({
      where: { id: parseInt(params.id) }
    });

    if (!material) {
      return NextResponse.json(
        { error: 'Learning material not found' },
        { status: 404 }
      );
    }

    const filePath = join(process.cwd(), 'public', material.filePath);
    const fileStat = await stat(filePath);

    // For video/audio files, support range requests for streaming
    const range = request.headers.get('range');
    if (range && material.fileType.startsWith('video/') || material.fileType.startsWith('audio/')) {
      return handleRangeRequest(filePath, range, material.fileType, fileStat.size);
    }

    // For other files, return the entire file
    const fileBuffer = await readFile(filePath);
    
    return new NextResponse(fileBuffer, {
      headers: {
        'Content-Type': material.fileType,
        'Content-Length': fileBuffer.length.toString()
      }
    });
  } catch (error) {
    console.error('Error previewing file:', error);
    return NextResponse.json(
      { error: 'Failed to preview file' },
      { status: 500 }
    );
  }
}

async function handleRangeRequest(filePath: string, range: string, contentType: string, fileSize: number) {
  const parts = range.replace(/bytes=/, "").split("-");
  const start = parseInt(parts[0], 10);
  const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
  const chunksize = (end - start) + 1;

  const fileStream = createReadStream(filePath, { start, end });
  
  const headers = {
    'Content-Range': `bytes ${start}-${end}/${fileSize}`,
    'Accept-Ranges': 'bytes',
    'Content-Length': chunksize.toString(),
    'Content-Type': contentType,
  };

  return new NextResponse(fileStream as any, {
    status: 206,
    headers
  });
}