 
import { WhatsAppGroupsList } from '@/components/admin/WhatsAppGroupsList'
import {prisma} from '@/lib/prismaDB'

export default async function WhatsAppGroupsPage() {
 
  const groups = await prisma.whatsAppGroup.findMany({
     
    orderBy: { createdAt: 'desc' }
  })

  return (
    <div className="min-h-screen bg-background">
      <WhatsAppGroupsList initialGroups={groups} />
    </div>
  )
}