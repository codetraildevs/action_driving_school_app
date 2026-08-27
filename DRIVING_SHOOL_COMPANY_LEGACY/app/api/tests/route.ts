import { NextRequest, NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';
import { verifyToken } from '@/lib/auth/jwt';
import { prisma } from "@/lib/prismaDB";

export async function GET(request: NextRequest) {
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
    
 
    const availableTests = await prisma.test.findMany({
      include: {
        testTranslations:true,
        _count: {
          select: {
            testQuestions: true
          }
        }
      },
   
    });

    return NextResponse.json({ 
      success: true, 
      data: availableTests,
      
    });
  } catch (error) {
    console.error('Get tests error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

const createTestHandler = withPermission(PERMISSIONS.TEST_CREATE)(
  async (request: NextRequest) => {
  try {
    const { title, description, totalMarks, passMarks, duration, imageUrl, isFree } = await request.json();

    if (!title || !totalMarks || !passMarks || !duration) {
      return NextResponse.json(
        { error: "Title, totalMarks, passMarks, and duration are required" },
        { status: 400 }
      );
    }

    if (passMarks > totalMarks) {
      return NextResponse.json(
        { error: "Pass marks cannot exceed total marks" },
        { status: 400 }
      );
    }

    
    const maxTestNumber = await prisma.test.aggregate({
      _max: {
        testNumber: true,
      },
    });

   
    const nextTestNumber = (maxTestNumber._max.testNumber || 0) + 1;

    const test = await prisma.test.create({
      data: {
        title,
        description,
        totalMarks: parseInt(totalMarks),
        passMarks: parseInt(passMarks),
        duration: parseInt(duration),
        testNumber: nextTestNumber, 
        imageUrl,
        isFree
      },
      include: {
        _count: {
          select: {
            testQuestions: true,
          },
        },
      },
    });

    return NextResponse.json({ 
      data: test,
      message: `Test created successfully with test number: ${nextTestNumber}` 
    }, { status: 201 });
  } catch (error) {
    console.error("Error creating test:", error);
    return NextResponse.json(
      { error: "Failed to create test" },
      { status: 500 }
    );
  }
  }
);

export const POST = createTestHandler;