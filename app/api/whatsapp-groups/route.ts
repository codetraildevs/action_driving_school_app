import { NextRequest, NextResponse } from 'next/server'
import {prisma} from '@/lib/prismaDB'
import { verifyToken } from '@/lib/auth/jwt'
 


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

    const groups = await prisma.whatsAppGroup.findMany({
      
      orderBy: { createdAt: 'desc' }
    })

    return NextResponse.json(groups)
  } catch (error) {
    console.error('Error fetching groups:', error)
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    )
  }
}

 