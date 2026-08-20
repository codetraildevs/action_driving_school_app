// components/user-subscription-requests/columns.tsx
"use client";

import { ColumnDef } from "@tanstack/react-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { MoreHorizontal, CheckCircle, XCircle, Copy } from "lucide-react";
import { toast } from "sonner";
import { UserTestAccess, UserRequestStatus } from "@/lib/generated/prisma";
import { differenceInDays } from "date-fns";

export type UserSubscriptionRequest = {
  id: number;
  user: {
    id:number,
    firstName: string;
    middleName:string;
    lastName: string;
    email: string;
  };
  userTestAccess: UserTestAccess;
  status: UserRequestStatus;
  createdAt: string;
  updatedAt: string;
  onAccept: (requestId: any) => void;
  onReject: (requestId: any) => void;
};

export const columns: ColumnDef<UserSubscriptionRequest>[] = [
  {
    accessorKey: "user",
    header: "User",
    
    cell: ({ row }) => {
      const user = row.getValue("user") as any;
      return (
        <div className="flex flex-col">
          <span className="font-medium">{`${user.firstName} ${user.middleName || ''} ${user.lastName}`}</span>
          <span className="text-sm text-muted-foreground">{user.email}</span>
        </div>
      );
    },
  },
  {
    accessorKey: "userTestAccess",
    header: "Test Access",
    cell: ({ row }) => {
      const plan = row.getValue("userTestAccess") as any;
      return (
        <div className="flex flex-col">
          <span className="font-medium">{plan?.maxTests} Tests</span>
          <span className="text-sm text-muted-foreground">
            {differenceInDays(new Date(plan?.expiresAt), new Date(plan?.createdAt))} days
          </span>
        </div>
      );
    },
  },
  {
    accessorKey: "status",
    header: "Status",
    cell: ({ row }) => {
      const status = row.getValue("status") as string;

      const statusConfig = {
        PENDING: { variant: "secondary" as const, label: "Pending" },
        ACCEPTED: { variant: "default" as const, label: "Accepted" },
        REJECTED: { variant: "destructive" as const, label: "Rejected" },
      };

      const config = statusConfig[status as keyof typeof statusConfig] || statusConfig.PENDING;

      return <Badge variant={config.variant}>{config.label}</Badge>;
    },
  },
  {
    accessorKey: "createdAt",
    header: "Request Date",
    cell: ({ row }) => {
      const date = new Date(row.getValue("createdAt"));
      return date.toLocaleDateString();
    },
  },
  {
    id: "actions",
    header: "Actions",
    cell: ({ row }) => {
      const request = row.original;
      const isPending = request.status === "PENDING";

      const handleCopyId = () => {
        navigator.clipboard.writeText(request.id.toString());
        toast("Request ID copied", {
          description: "Request ID has been copied successfully",
        });
      };

      return (
        <div className="flex items-center gap-2">
          {/* Visible Action Buttons */}
          {isPending && (
            <>
              <Button
                variant="outline"
                size="sm"
                onClick={() => request.onAccept(request.id)}
                className="text-green-600 border-green-200 hover:bg-green-50 hover:text-green-700"
                title="Accept Request"
              >
                <CheckCircle className="h-4 w-4" />
                <span className="hidden lg:inline ml-2">Accept</span>
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => request.onReject(request.id)}
                className="text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700"
                title="Reject Request"
              >
                <XCircle className="h-4 w-4" />
                <span className="hidden lg:inline ml-2">Reject</span>
              </Button>
            </>
          )}

          {/* Copy Button - Always Visible */}
          <Button
            variant="outline"
            size="sm"
            onClick={handleCopyId}
            className="text-blue-600 border-blue-200 hover:bg-blue-50 hover:text-blue-700"
            title="Copy Request ID"
          >
            <Copy className="h-4 w-4" />
            <span className="hidden lg:inline ml-2">Copy ID</span>
          </Button>

          {/* Dropdown Menu for Additional Actions */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="h-8 w-8 p-0">
                <span className="sr-only">Open menu</span>
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel>Additional Actions</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={handleCopyId}>
                <Copy className="h-4 w-4 mr-2" />
                Copy Request ID
              </DropdownMenuItem>
              {isPending && (
                <>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    onClick={() => request.onAccept(request.id)}
                    className="text-green-600"
                  >
                    <CheckCircle className="h-4 w-4 mr-2" />
                    Accept Request
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    onClick={() => request.onReject(request.id)}
                    className="text-red-600"
                  >
                    <XCircle className="h-4 w-4 mr-2" />
                    Reject Request
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      );
    },
  },
];