import { NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";
import { RequestStatus } from "@/lib/generated/prisma";
import { sendFCMNotification } from "@/lib/notification";

/* ----------------------------------------------------
   Status → default progress mapping
   Used when the admin changes a request's status without
   moving the Completion Percentage slider, so progress
   reflects the status instead of staying at 0%.
---------------------------------------------------- */
function deriveDefaultPercentage(
  status: RequestStatus,
  current: number
): number {
  switch (status) {
    case "PROCESSING":
      return Math.max(current, 50);
    case "ACTION":
      return Math.max(current, 60);
    case "APPROVED":
    case "REJECTED":
      return 100;
    default:
      return current; // PENDING stays as-is
  }
}

/* ----------------------------------------------------
   Notification helper
---------------------------------------------------- */
function buildNotification(
  type: "DRIVING_LICENSE" | "SPECIAL",
  status: RequestStatus,
  progress: number,
  lang: string
) {
  const baseTitle =
    type === "DRIVING_LICENSE"
      ? {
          en: "Driving License Update",
          fr: "Mise à jour du permis",
          rw: "Amakuru ku ruhushya rwo gutwara",
        }
      : {
          en: "Service Request Update",
          fr: "Mise à jour du service",
          rw: "Amakuru kuri serivisi",
        };

  const statusMessages: any = {
    PENDING: {
      en: "Your request is pending.",
      fr: "Votre demande est en attente.",
      rw: "Gusaba kwawe biracyategerejwe.",
    },
    PROCESSING: {
      en: "Your request is being processed.",
      fr: "Votre demande est en cours de traitement.",
      rw: "Gusaba kwawe biri gutunganywa.",
    },
    ACTION: {
      en: "Action is required from you.",
      fr: "Une action est requise de votre part.",
      rw: "Hari igikorwa ugomba gukora.",
    },
    APPROVED: {
      en: "Your request has been approved 🎉",
      fr: "Votre demande a été approuvée 🎉",
      rw: "Gusaba kwawe byemejwe 🎉",
    },
    REJECTED: {
      en: "Your request was rejected ❌",
      fr: "Votre demande a été rejetée ❌",
      rw: "Gusaba kwawe kwanzwe ❌",
    },
  };

  return {
    title: baseTitle[lang as keyof typeof baseTitle] || baseTitle.en,
    message:
      `${statusMessages[status]?.[lang] || statusMessages[status]?.en} ` +
      `(${progress}%)`,
  };
}

/* ----------------------------------------------------
   PUT – Update Request + Notify
---------------------------------------------------- */
const updateRequestHandler = withPermission(PERMISSIONS.USER_UPDATE)(
  async (
    req: Request,
    { params }: { params: { id: string } }
  ) => {
  const referenceId = params.id;
  const body = await req.json();

  const {
    type,
    status,
    message,
    completionPercentage,
  }: {
    type: "DRIVING_LICENSE" | "SPECIAL";
    status: RequestStatus;
    message?: string;
    completionPercentage: number;
  } = body;

  /* ================= DRIVING LICENSE ================= */
  if (type === "DRIVING_LICENSE") {
    const existing = await prisma.iremboDrivingLicenseRequest.findUnique({
      where: { referenceId },
      include: {
        user: {
          include: {
            language: true,
            devices: true,
          },
        },
      },
    });

    if (!existing) {
      return NextResponse.json(
        { error: "Driving license request not found" },
        { status: 404 }
      );
    }

    // If the admin didn't move the slider (sent === current), derive the
    // percentage from the status. This also repairs stale rows: an
    // APPROVED/REJECTED request still sitting at 0% gets corrected to 100%
    // on the next save, even without a status change.
    const statusChanged = status !== existing.status;
    const percentageChanged =
      completionPercentage !== existing.completionPercentage;
    const percentage =
      !percentageChanged &&
      (statusChanged ||
        status === "APPROVED" ||
        status === "REJECTED")
        ? deriveDefaultPercentage(status, existing.completionPercentage)
        : completionPercentage;

    const updated = await prisma.iremboDrivingLicenseRequest.update({
      where: { referenceId },
      data: { status, message, completionPercentage: percentage },
    });

    await sendNotificationIfChanged(
      existing,
      updated,
      "DRIVING_LICENSE"
    );

    return NextResponse.json(formatDrivingResponse(updated));
  }

  /* ================= SPECIAL REQUEST ================= */
  const existing = await prisma.iremboSpecialRequest.findUnique({
    where: { referenceId },
    include: {
      user: {
        include: {
          language: true,
          devices: true,
        },
      },
    },
  });

  if (!existing) {
    return NextResponse.json(
      { error: "Special request not found" },
      { status: 404 }
    );
  }

  // Same status → percentage derivation as the driving branch
  // (see comment above for the stale-row repair).
  const statusChanged = status !== existing.status;
  const percentageChanged =
    completionPercentage !== existing.completionPercentage;
  const percentage =
    !percentageChanged &&
    (statusChanged ||
      status === "APPROVED" ||
      status === "REJECTED")
      ? deriveDefaultPercentage(status, existing.completionPercentage)
      : completionPercentage;

  const updated = await prisma.iremboSpecialRequest.update({
    where: { referenceId },
    data: { status, message, completionPercentage: percentage },
  });

  await sendNotificationIfChanged(existing, updated, "SPECIAL");

  return NextResponse.json(formatSpecialResponse(updated));
  }
);

export const PUT = updateRequestHandler;

/* ----------------------------------------------------
   Shared notification sender
---------------------------------------------------- */
async function sendNotificationIfChanged(
  before: any,
  after: any,
  type: "DRIVING_LICENSE" | "SPECIAL"
) {
  const user = before.user;
  const lang = user.language?.languageCode || "en";
  const device = user.devices?.[0];

  if (!device) return;

  const firebase = await prisma.firebaseDevice.findFirst({
    where: { physicalDeviceId: device.physicalAddress },
  });

  if (!firebase) return;

  const changed =
    before.status !== after.status ||
    before.completionPercentage !== after.completionPercentage ||
    before.message !== after.message;

  if (!changed) return;

  const notif = buildNotification(
    type,
    after.status,
    after.completionPercentage,
    lang
  );

  const exists = await prisma.userNotification.findFirst({
    where: {
      userId: user.id,
      title: notif.title,
      message: notif.message,
    },
  });

  if (exists) return;

  await prisma.userNotification.create({
    data: {
      userId: user.id,
      title: notif.title,
      message: notif.message,
    },
  });

  await sendFCMNotification(
    firebase.deviceToken,
    notif.title,
    notif.message,
    {
      channel_id: type,
      referenceId: after.referenceId,
      status: after.status,
    }
  );
}

/* ----------------------------------------------------
   Response formatters
---------------------------------------------------- */
function formatDrivingResponse(updated: any) {
  return {
    id: updated.id,
    type: "DRIVING_LICENSE",
    title: `${updated.licenseType} Driving License`,
    status: updated.status,
    message: updated.message,
    nationalId: updated.applicantNationalId,
    phoneNumber: updated.applicantPhoneNumber,
    completionPercentage: updated.completionPercentage,
    address: updated.address,
    updatedAt: updated.updatedAt,
    referenceId: updated.referenceId,
  };
}

function formatSpecialResponse(updated: any) {
  return {
    id: updated.id,
    type: "SPECIAL",
    title: updated.serviceName,
    status: updated.status,
    message: updated.message,
    completionPercentage: updated.completionPercentage,
    updatedAt: updated.updatedAt,
    nationalId: updated.nationalId,
    phoneNumber: updated.user?.phoneNumber,
    address: null,
    referenceId: updated.referenceId,
  };
}

 

const deleteRequestHandler = withPermission(PERMISSIONS.USER_DELETE)(
  async (
    req: Request,
    { params }: { params: { id: string } }
  ) => {
  try {
    const id = Number(params.id);

    // Get the type from query params
    const url = new URL(req.url);
    const type = url.searchParams.get("type") as
      | "DRIVING_LICENSE"
      | "SPECIAL"
      | null;

    if (!type) {
      return NextResponse.json(
        { error: "Request type is required" },
        { status: 400 }
      );
    }

    if (type === "DRIVING_LICENSE") {
      await prisma.iremboDrivingLicenseRequest.delete({
        where: { id },
      });
    } else {
      await prisma.iremboSpecialRequest.delete({
        where: { id },
      });
    }

    return NextResponse.json({
      success: true,
      message: "Request deleted successfully",
    });
  } catch (error) {
    console.error("Error deleting request:", error);
    return NextResponse.json(
      { error: "Failed to delete request" },
      { status: 500 }
    );
  }
  }
);

export const DELETE = deleteRequestHandler;
