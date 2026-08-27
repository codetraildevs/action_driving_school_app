"use client"

import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Loader2, Users, Link, Image } from 'lucide-react'
import { MultiSelect } from '@/components/ui/multi-select'
import { User, WhatsAppGroup } from '@/lib/generated/prisma'
 
 
const formSchema = z.object({
  name: z.string().min(1, 'Group name is required').max(255),
  description: z.string().max(500).optional(),
  whatsappLink: z.string().url('Invalid URL').optional().or(z.literal('')),
  imageUrl: z.string().url('Invalid URL').optional().or(z.literal('')),
})

interface GroupDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  group?: WhatsAppGroup | null
  onSubmit: (data: z.infer<typeof formSchema>) => Promise<void>
  isSubmitting: boolean
}

export function GroupDialog({
  open,
  onOpenChange,
  group,
  onSubmit,
  isSubmitting
}: GroupDialogProps) {
  

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: '',
      description: '',
      whatsappLink: '',
      imageUrl: '',
     
    }
  })

  useEffect(() => {
    if (group) {
      form.reset({
        name: group.name,
        description: group.description || '',
        whatsappLink: group.whatsappLink || '',
        imageUrl: group.imageUrl || '',
        
      })
    } else {
      form.reset({
        name: '',
        description: '',
        whatsappLink: '',
        imageUrl: '',
       
      })
    }
  }, [group, form, open])

  

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[550px] bg-background">
        <DialogHeader>
          <DialogTitle className="text-foreground">
            {group ? 'Edit Group' : 'Create New Group'}
          </DialogTitle>
          <DialogDescription className="text-muted-foreground">
            {group ? 'Update the WhatsApp group details' : 'Create a new WhatsApp learning group'}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-foreground">Group Name</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder="Enter group name" 
                      {...field} 
                      className="bg-background border-input"
                    />
                  </FormControl>
                  <FormMessage className="text-destructive" />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-foreground">Description</FormLabel>
                  <FormControl>
                    <Textarea 
                      placeholder="Enter group description" 
                      {...field} 
                      className="bg-background border-input min-h-[100px]"
                    />
                  </FormControl>
                  <FormMessage className="text-destructive" />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="whatsappLink"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-foreground flex items-center gap-2">
                      <Link className="w-4 h-4" />
                      WhatsApp Link
                    </FormLabel>
                    <FormControl>
                      <Input 
                        placeholder="https://chat.whatsapp.com/..." 
                        {...field} 
                        className="bg-background border-input"
                      />
                    </FormControl>
                    <FormMessage className="text-destructive" />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="imageUrl"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-foreground flex items-center gap-2">
                      <Image className="w-4 h-4" />
                      Image URL
                    </FormLabel>
                    <FormControl>
                      <Input 
                        placeholder="https://example.com/image.jpg" 
                        {...field} 
                        className="bg-background border-input"
                      />
                    </FormControl>
                    <FormMessage className="text-destructive" />
                  </FormItem>
                )}
              />
            </div>

     

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isSubmitting}
                className="border-input text-foreground hover:bg-accent"
              >
                Cancel
              </Button>
              <Button 
                type="submit" 
                disabled={isSubmitting}
                className="bg-primary text-primary-foreground hover:bg-primary/90"
              >
                {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {group ? 'Update Group' : 'Create Group'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}