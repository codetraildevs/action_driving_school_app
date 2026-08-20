"use client"

import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { AdminRequest } from "@/types/request"
import { Progress } from "@/components/ui/progress"
import { Button } from "../ui/button"
import { Copy, Trash2 } from "lucide-react"
import { toast } from "sonner"

interface Props {
  request: AdminRequest
  onClick: () => void
    onDelete: (req:AdminRequest) => void
}

export default function RequestCard({ request, onClick,onDelete }: Props) {
  return (
    <Card 
      onClick={onClick} 
      className="cursor-pointer hover:shadow-lg transition-all duration-200 border-border my-2"
    >
      <CardContent className="p-6 space-y-4">
        <div className="flex items-center justify-between gap-3">
          <Badge variant="secondary" className="text-xs font-medium">
            {request.type}
          </Badge>
          <Badge 
            variant={request.status === "APPROVED" ? "default" : "outline"}
            className="text-xs font-medium"
          >
            {request.status}
          </Badge>
          <Button onClick={(e) => {
            e.stopPropagation();
            onDelete(request);
          }} variant={"ghost"}><Trash2/></Button>
        </div>

        <div className="space-y-2">
          <h3 className="font-semibold text-lg text-foreground text-center">
            {request.title}
          </h3>

          <div className="flex flex-col items-center gap-2 text-sm">
              <div className="text-muted-foreground">
              Names: <span className="text-foreground font-medium">{request.names}</span>
            </div>
            <div className="text-muted-foreground">
              ID: <span className="text-foreground font-medium">{request.nationalId}</span>
            </div>
            <div className="text-muted-foreground flex justify-center">
              Phone: <span className="text-foreground font-medium">{request.phoneNumber}</span>
            </div>
            <div className="text-muted-foreground flex justify-center">
              Ref: <span className="text-foreground font-medium ">{request.referenceId }</span>
              <Button variant={"ghost"} onClick={(e) => {
                e.stopPropagation();
                
                navigator.clipboard.writeText(request.referenceId)
                toast.success("Reference ID copied to clipboard")
              }}><Copy/></Button>
            </div>
          </div>

          {request.address && (
            <p className="text-sm text-muted-foreground">
              Address: <span className="text-foreground">{request.address}</span>
            </p>
          )}
        </div>

        {request.message && (
          <div className="bg-muted/50 rounded-lg p-3 border border-border">
            <p className="text-sm text-foreground leading-relaxed break-all">
              {request.message}
            </p>
          </div>
        )}

        <div className="space-y-2">
          <Progress 
            value={request.completionPercentage} 
            className="h-2"
          />
          <p className="text-xs text-muted-foreground text-right">
            {request.completionPercentage}% Complete
          </p>
        </div>
      </CardContent>
    </Card>
  )
}