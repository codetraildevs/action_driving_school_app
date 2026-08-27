import { PaymentStatus, RequestStatus } from "@/lib/generated/prisma";

export type AdminRequestDTO = {
  id: number;
  type: "DRIVING_LICENSE" | "SPECIAL";
  title: string;
  referenceId?: string;
  category?: string;
  serviceName?: string;
  status: RequestStatus;
  message?: string;
  completionPercentage: number;
  currentStep?: string;
  paymentStatus?: PaymentStatus;
  paymentAmount?: number;
  applicantName: string;
  applicantPhone: string;
  nationalId: string;
  createdAt: string;
  updatedAt: string;
  raw: any;
};

export type RequestType = "DRIVING_LICENSE" | "SPECIAL";
export interface AdminRequest {
  id: number;
  type: RequestType;
  title: string;
  status: RequestStatus;
  message?: string | null;
  completionPercentage: number;
  updatedAt: string;
  referenceId: string;
  nationalId: string;
  phoneNumber: string;
  address: null;
  names: string;
}
