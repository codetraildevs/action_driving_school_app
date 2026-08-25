import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";
import { UserRequestStatus } from "@/lib/generated/prisma";
import { sendFCMNotification } from "@/lib/notification";
import { NOTIFICATION_CHANNELS } from "@/lib/types";

const rejectRequestHandler = withPermission(PERMISSIONS.SUBSCRIPTION_MANAGE)(
  async (
    request: NextRequest,
    { params }: { params: { requestId: string } }
  ) => {
  try {
    await prisma.userSubscriptionRequest.update({
      where: { id: parseInt(params.requestId) },
      data: {
        status: UserRequestStatus.REJECTED,
      },
    });

    const subscriptionRequest = await prisma.userSubscriptionRequest.findUnique({
      where: { id: parseInt(params.requestId) },
      include: { user: { include: { language: true } } },
    });

    if (subscriptionRequest) {
      const firebaseDevice = await prisma.firebaseDevice.findFirst({
      where: { physicalDeviceId: subscriptionRequest.user.id.toString() },
      });

      if (firebaseDevice) {
      let notificationMessage;
      let notificationTitle;
      let userLanguageCode = subscriptionRequest.user.language?.languageCode || "en";
      switch (userLanguageCode) {
        case "fr":
        notificationMessage = "Votre demande d'accès a été rejetée.";
        notificationTitle = "Demande Rejetée";
        break;
        case "es":
        notificationMessage = "Su solicitud de acceso ha sido rechazada.";
        notificationTitle = "Solicitud Rechazada";
        break;
        case "rw":
        notificationMessage = "Ifatabuguzi ryawe Ryanzwe.";
        notificationTitle = "Ifatabuguzi";
        break;
        default:
        notificationMessage = "Your access request has been rejected.";
        notificationTitle = "Request Rejected";
      }

      sendFCMNotification(
        firebaseDevice.deviceToken,
        notificationTitle,
        notificationMessage,
        {
        channel_id: NOTIFICATION_CHANNELS.SUBSCRIPTIONS,
        }
      );
      }
    }

    return NextResponse.json(
      { success: true, message: "User subscription request rejected" },
      { status: 200 }
    );
  } catch (error: any) {
    console.error("Error updating subscription plan:", error);

    if (error.code === "P2002") {
      return NextResponse.json(
        { error: "A plan with this name already exists" },
        { status: 400 }
      );
    }

    return NextResponse.json(
      { error: "Failed to update subscription plan" },
      { status: 500 }
    );
  }
  }
);

export const PATCH = rejectRequestHandler;
