import { NextRequest, NextResponse } from 'next/server' 
import { z } from 'zod'
import { withPermission } from '@/lib/middleware/withPermission'
import { PERMISSIONS } from '@/lib/auth/permissions'
import { prisma } from '@/lib/prismaDB'

const updateGroupSchema = z.object({
  name: z.string().min(1).max(255).optional(),
  description: z.string().max(500).optional(),
  whatsappLink: z.string().url().optional(),
  imageUrl: z.string().url().optional(),
  isActive: z.boolean().optional(),
  memberIds: z.array(z.string()).optional(),
})

// GET single group
const getGroupHandler = withPermission(PERMISSIONS.USER_READ)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
    const group = await prisma.whatsAppGroup.findUnique({
      where: { id: params.id },
    })

    if (!group) {
      return NextResponse.json({ error: 'Group not found' }, { status: 404 })
    }

    return NextResponse.json(group)
  }
)

export const GET = getGroupHandler

// PATCH update group
const updateGroupHandler = withPermission(PERMISSIONS.USER_UPDATE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
    try {
      const body = await request.json()
      const validatedData = updateGroupSchema.parse(body)

      // Update group
      const group = await prisma.whatsAppGroup.update({
        where: { id: params.id },
        data: {
          ...validatedData,
        }
      })

      return NextResponse.json(group)
    } catch (error) {
      if (error instanceof z.ZodError) {
        return NextResponse.json(
          { error: 'Validation error', details: error.issues },
          { status: 400 }
        )
      }
      console.error('Error updating group:', error)
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      )
    }
  }
)

export const PATCH = updateGroupHandler

// DELETE group
const deleteGroupHandler = withPermission(PERMISSIONS.USER_DELETE)(
  async (
    request: NextRequest,
    { params }: { params: { id: string } }
  ) => {
    try {
      await prisma.whatsAppGroup.delete({
        where: { id: params.id }
      })

      return NextResponse.json({ message: 'Group deleted successfully' })
    } catch (error) {
      console.error('Error deleting group:', error)
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      )
    }
  }
)

export const DELETE = deleteGroupHandler