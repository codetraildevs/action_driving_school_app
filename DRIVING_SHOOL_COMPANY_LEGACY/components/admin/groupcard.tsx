"use client"

 
import { 
  Card, 
  CardContent, 
  CardDescription, 
  CardFooter, 
  CardHeader, 
  CardTitle 
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { 
  Users, 
  Link, 
  Edit, 
  Trash2, 
  Copy,
  ExternalLink,
  MessageCircle
} from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { formatDistanceToNow } from 'date-fns'
import { toast } from 'sonner'
import { User, WhatsAppGroup } from "@/lib/generated/prisma"
 
interface GroupCardProps {
  group:  WhatsAppGroup
  onEdit: (group:   WhatsAppGroup) => void
  onDelete: (group:  WhatsAppGroup) => void
}

export function GroupCard({ group, onEdit, onDelete }: GroupCardProps) {
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    toast.success('Copied to clipboard')
  }

  const formatDate = (date: Date) => {
    return formatDistanceToNow(new Date(date), { addSuffix: true })
  }

  return (
    <Card className="bg-background border-border hover:shadow-md transition-shadow">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <Avatar className="h-12 w-12 border-2 border-primary/20">
              <AvatarImage src={group.imageUrl || ''} />
              <AvatarFallback className="bg-primary/10 text-primary">
                <MessageCircle className="h-6 w-6" />
              </AvatarFallback>
            </Avatar>
            <div>
              <CardTitle className="text-foreground text-lg flex items-center gap-2">
                {group.name}
                {!group.isActive && (
                  <Badge variant="outline" className="text-muted-foreground border-muted">
                    Inactive
                  </Badge>
                )}
              </CardTitle>
              
            </div>
          </div>
          <Badge 
            variant={group.isActive ? "default" : "secondary"} 
            className={group.isActive ? "bg-primary text-primary-foreground" : ""}
          >
            {group.isActive ? 'Active' : 'Inactive'}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="pb-3">
        {group.description && (
          <p className="text-sm text-foreground mb-4">{group.description}</p>
        )}

        <div className="flex flex-wrap gap-3 mb-3">
          
          
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <span>Created {formatDate(group.createdAt)}</span>
          </div>
        </div>

        {group.whatsappLink && (
          <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
            <Link className="h-4 w-4 text-primary" />
            <span className="text-sm text-foreground truncate flex-1">
              {group.whatsappLink}
            </span>
            <div className="flex gap-1">
              <Button
                size="sm"
                variant="ghost"
                onClick={() => copyToClipboard(group.whatsappLink!)}
                className="h-8 w-8 p-0 hover:bg-accent"
              >
                <Copy className="h-4 w-4" />
              </Button>
              <Button
                size="sm"
                variant="ghost"
                asChild
                className="h-8 w-8 p-0 hover:bg-accent"
              >
                <a href={group.whatsappLink} target="_blank" rel="noopener noreferrer">
                  <ExternalLink className="h-4 w-4" />
                </a>
              </Button>
            </div>
          </div>
        )}
      </CardContent>

      <CardFooter className="pt-0 flex justify-between">
        <div className="text-xs text-muted-foreground">
          Updated {formatDate(group.updatedAt)}
        </div>
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() => onEdit(group)}
            className="border-input text-foreground hover:bg-accent"
          >
            <Edit className="h-4 w-4 mr-2" />
            Edit
          </Button>
          <Button
            size="sm"
            variant="destructive"
            onClick={() => onDelete(group)}
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Delete
          </Button>
        </div>
      </CardFooter>
    </Card>
  )
}