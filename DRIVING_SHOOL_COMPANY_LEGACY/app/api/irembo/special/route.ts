import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prismaDB'
import { verifyToken } from '@/lib/auth/jwt'
 

// GET - List all special requests
export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const page = parseInt(searchParams.get('page') || '1')
    const limit = parseInt(searchParams.get('limit') || '10')
    const search = searchParams.get('search') || ''
    const status = searchParams.get('status')
    const category = searchParams.get('category')
    const userId = searchParams.get('userId')

    const skip = (page - 1) * limit

    // Build where clause
    const where: any = {}

    if (userId) {
      where.userId = parseInt(userId)
    }

    if (status) {
      where.status = status
    }

    if (category) {
      where.category = category
    }

    if (search) {
      where.OR = [
        { applicantName: { contains: search, mode: 'insensitive' } },
        { nationalId: { contains: search, mode: 'insensitive' } },
        { applicantPhone: { contains: search, mode: 'insensitive' } },
        { serviceName: { contains: search, mode: 'insensitive' } },
        { requestId: { contains: search, mode: 'insensitive' } },
      ]
    }

    // Get total count
    const total = await prisma.iremboSpecialRequest.count({ where })

    // Get paginated results
    const requests = await prisma.iremboSpecialRequest.findMany({
      where,
      skip,
      take: limit,
      orderBy: { createdAt: 'desc' },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
            phoneNumber: true,
            email: true
          }
        }
      }
    })

    const response = {
      success: true,
      data: {
        requests,
        pagination: {
          page,
          limit,
          total,
          pages: Math.ceil(total / limit)
        }
      }
    }

    return NextResponse.json(response)
  } catch (error) {
    console.error('GET /api/irembo/special error:', error)
    const response = {
      success: false,
      error: 'Failed to fetch special requests'
    }
    return NextResponse.json(response, { status: 500 })
  }
}

// POST - Create new special request
export async function POST(request: NextRequest) {
  try {
    const body  = await request.json()

      const authHeader = request.headers.get("authorization");
        if (!authHeader?.startsWith("Bearer ")) {
          return NextResponse.json(
            { success: false, error: "Unauthorized: Missing or malformed token" },
            { status: 401 }
          );
        }
    
        const token = authHeader.substring(7);
        const payload = await verifyToken(token);
    
        if (!payload?.userId) {
          return NextResponse.json(
            { success: false, error: "Unauthorized: Invalid or expired token" },
            { status: 401 }
          );
        }
        const userId = payload.userId;

    // Prevent duplicate active requests: a user may have only one
    // in-progress (PENDING/PROCESSING/ACTION) special request at a time.
    const activeRequest = await prisma.iremboSpecialRequest.findFirst({
      where: {
        userId,
        status: { in: ["PENDING", "PROCESSING", "ACTION"] },
      },
      select: { referenceId: true },
    });

    if (activeRequest) {
      return NextResponse.json(
        {
          success: false,
          error:
            "You already have a pending special service request. Please wait for it to be processed before submitting a new one.",
          existingReferenceId: activeRequest.referenceId,
        },
        { status: 409 }
      );
    }

    // Create request (explicit field mapping keeps the payload aligned with the schema)
    const { serviceName, category, applicantName, applicantPhone, nationalId, description } = body

    const newRequest = await prisma.iremboSpecialRequest.create({
      data: {
        serviceName,
        category,
        applicantName,
        applicantPhone,
        nationalId,
        description,
        userId
      },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true
          }
        }
      }
    })
     await prisma.iremboSpecialRequest.update({where:{id:newRequest.id}, data:{referenceId:`IREMBO_${Date.now()}${newRequest.id}`}})

    const response = {
      success: true,
      data: {
        amount:2000,
        currency:"RWF",
        itemName:"Special Service Request",
        recipient:"0791105800",
        transactionFee:200
      },
      message: 'Special request created successfully'
    }

    return NextResponse.json(response, { status: 201 })
  } catch (error) {
    console.error('POST /api/irembo/special error:', error)
    const response = {
      success: false,
      error: 'Failed to create special request'
    }
    return NextResponse.json(response, { status: 500 })
  }
}