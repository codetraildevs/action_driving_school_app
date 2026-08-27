import { NextRequest, NextResponse } from 'next/server'
import { withPermission } from '@/lib/middleware/withPermission'
import { PERMISSIONS } from '@/lib/auth/permissions'
import { prisma } from '@/lib/prismaDB'
import { z } from 'zod'

// Validation schemas
const createGroupSchema = z.object({
  name: z.string().min(1, 'Group name is required').max(255),
  description: z.string().max(500).optional(),
  whatsappLink: z.string().url('Invalid WhatsApp link').optional(),
  imageUrl: z.string().url('Invalid image URL').optional(),
  memberIds: z.array(z.string()).optional(),
})

// GET all groups
const getGroupsHandler = withPermission(PERMISSIONS.USER_READ)(
  async () => {
    const groups = await prisma.whatsAppGroup.findMany({
      orderBy: { createdAt: 'desc' }
    })

    return NextResponse.json(groups)
  }
)

export const GET = getGroupsHandler

// POST create new group
const createGroupHandler = withPermission(PERMISSIONS.USER_UPDATE)(
  async (request: NextRequest) => {
    try {
      const body = await request.json()
      const validatedData = createGroupSchema.parse(body)

      // Create group
      const group = await prisma.whatsAppGroup.create({
        data: {
          name: validatedData.name,
          description: validatedData.description,
          whatsappLink: validatedData.whatsappLink,
          imageUrl: validatedData.imageUrl,
        }
      })

      return NextResponse.json(group, { status: 201 })
    } catch (error) {
      if (error instanceof z.ZodError) {
        return NextResponse.json(
          { error: 'Validation error', details: error.issues },
          { status: 400 }
        )
      }
      console.error('Error creating group:', error)
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      )
    }
  }
)

export const POST = createGroupHandler