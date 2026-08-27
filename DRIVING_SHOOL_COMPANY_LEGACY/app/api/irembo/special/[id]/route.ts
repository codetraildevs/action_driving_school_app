import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prismaDB'
 

interface RouteParams {
  params: {
    id: string
  }
}

// GET - Get single special request
export async function GET(request: NextRequest, { params }: RouteParams) {
  try {
    const { id } = params

    const requestData = await prisma.iremboSpecialRequest.findUnique({
      where: { 
        id: parseInt(id)
      },
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

    if (!requestData) {
      const response  = {
        success: false,
        error: 'Special request not found'
      }
      return NextResponse.json(response, { status: 404 })
    }

    const response  = {
      success: true,
      data: requestData
    }

    return NextResponse.json(response)
  } catch (error) {
    console.error('GET /api/irembo/special/[id] error:', error)
    const response  = {
      success: false,
      error: 'Failed to fetch special request'
    }
    return NextResponse.json(response, { status: 500 })
  }
}

// PUT - Update special request
export async function PUT(request: NextRequest, { params }: RouteParams) {
  try {
    const { id } = params
    const body  = await request.json()

    const updatedRequest = await prisma.iremboSpecialRequest.update({
      where: { 
        id: parseInt(id)
      },
      data: body,
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

    const response  = {
      success: true,
      data: updatedRequest,
      message: 'Special request updated successfully'
    }

    return NextResponse.json(response)
  } catch (error) {
    console.error('PUT /api/irembo/special/[id] error:', error)
    const response  = {
      success: false,
      error: 'Failed to update special request'
    }
    return NextResponse.json(response, { status: 500 })
  }
}

// DELETE - Delete special request
export async function DELETE(request: NextRequest, { params }: RouteParams) {
  try {
    const { id } = params

    await prisma.iremboSpecialRequest.delete({
      where: { 
         id: parseInt(id)
      }
    })

    const response  = {
      success: true,
      message: 'Special request deleted successfully'
    }

    return NextResponse.json(response)
  } catch (error) {
    console.error('DELETE /api/irembo/special/[id] error:', error)
    const response  = {
      success: false,
      error: 'Failed to delete special request'
    }
    return NextResponse.json(response, { status: 500 })
  }
}