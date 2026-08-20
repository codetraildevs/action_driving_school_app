"use client"

import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectItem,
  SelectContent,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select"
import { Label } from "@/components/ui/label"
import { apiClient } from "@/lib/api-client"
import { useState, useEffect } from "react"
import { AdminRequest } from "@/types/request"
import { RequestStatus } from "@/lib/generated/prisma"
import { Slider } from "../ui/slider"
import { toast } from "sonner"

interface Props {
  open: boolean
  onOpenChange: (v: boolean) => void
  request: AdminRequest
  onUpdated: (req: AdminRequest) => void
}

interface RequestFormState {
  status: RequestStatus
  message: string
  percentage: number
  loading: boolean
}

export default function RequestDetailsDialog({
  open,
  onOpenChange,
  request,
  onUpdated
}: Props) {
  // Single state object
  const [formState, setFormState] = useState<RequestFormState>({
    status: request.status,
    message: request.message || "",
    percentage: request.completionPercentage,
    loading: false
  })

  // Update internal state when request prop changes
  useEffect(() => {
    setFormState({
      status: request.status,
      message: request.message || "",
      percentage: request.completionPercentage,
      loading: false
    })
  }, [request])

  async function handleSave() {
   try{
     setFormState(prev => ({ ...prev, loading: true }))

    const res = await apiClient.put<AdminRequest>(
      `/api/admin/requests/${request.referenceId}`,
      {
        type: request.type,
        status: formState.status,
        message: formState.message,
        completionPercentage: formState.percentage
      }
    )

    onUpdated(res)
    setFormState(prev => ({ ...prev, loading: false }))
    onOpenChange(false)
   }catch{
    toast.error("Failed to updated request")
   }finally{
     setFormState(prev => ({ ...prev, loading: false }))
   }
  }

  // Helper functions to update specific fields
  const updateStatus = (status: RequestStatus) => {
    setFormState(prev => ({
      ...prev,
      status,
      // Auto-suggest a percentage when the status changes and the
      // slider hasn't been touched (matches the server-side derivation).
      percentage: prev.percentage === request.completionPercentage
        ? defaultPercentageFor(status)
        : prev.percentage,
    }))
  }

  function defaultPercentageFor(status: RequestStatus): number {
    switch (status) {
      case "PROCESSING": return 50
      case "ACTION": return 60
      case "APPROVED":
      case "REJECTED": return 100
      default: return 0
    }
  }

  const updateMessage = (message: string) => {
    setFormState(prev => ({ ...prev, message }))
  }

  const updatePercentage = (percentage: number) => {
    setFormState(prev => ({ ...prev, percentage }))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Request #{request.referenceId}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <div>
            <Label>Status</Label>
            <Select 
              value={formState.status} 
              onValueChange={(value) => updateStatus(value as RequestStatus)}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                { Object.values(RequestStatus).map(s => (
                  <SelectItem key={s} value={s}>
                    {s}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div>
            <Label>Admin Message</Label>
            <Textarea
              value={formState.message}
              onChange={e => updateMessage(e.target.value)}
              placeholder="Instructions for applicant..."
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label>Completion Percentage {formState.percentage}%</Label>
            <Slider
              min={0}
              max={100}
              step={1}
              defaultValue={[formState.percentage]}
              onValueChange={(newValue) => updatePercentage(Number(newValue[0]))}
            />
          </div>
        </div>

        <DialogFooter>
          <Button onClick={handleSave} disabled={formState.loading}>
            {formState.loading ? "Saving..." : "Save Changes"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}