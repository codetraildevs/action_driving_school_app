import { NextRequest, NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";
import {
  UserRequestStatus,
  UserTestAccessStatus,
} from "@/lib/generated/prisma";
import { differenceInDays } from "date-fns";
import { NOTIFICATION_CHANNELS } from "@/lib/types";
import { sendFCMNotification } from "@/lib/notification";

const acceptRequestHandler = withPermission(PERMISSIONS.SUBSCRIPTION_MANAGE)(
  async (
    request: NextRequest,
    { params }: { params: { requestId: string } }
  ) => {
  try {
    const requestId = parseInt(params.requestId);
    const {days}= await request.json()
    console.log(days)
    if (isNaN(requestId)) {
      return NextResponse.json(
        { error: "Invalid request ID" },
        { status: 400 }
      );
    }

    let subscriptionRequest: any;
    const result = await prisma.$transaction(async (tx) => {
      subscriptionRequest = await tx.userSubscriptionRequest.findUnique({
        where: { id: requestId },
        include:{user:true}

       
      });

      if (!subscriptionRequest) {
        throw new Error("NOT_FOUND");
      }

       

      const requestDate = subscriptionRequest.createdAt;
      const expectedEndDate = subscriptionRequest.requestedExpiresAt;
      const daysElapsed = differenceInDays(new Date(), requestDate);
      
      const endDate = new Date(expectedEndDate);
      endDate.setDate(expectedEndDate.getDate() + (daysElapsed+days));

      const updatedTestAccess = await tx.userTestAccess.update({
        where: { 
          userId: subscriptionRequest.userId,
        },
        data: {
          
          maxTest:subscriptionRequest.requestedTests,
          expiresAt: endDate,
          status: UserTestAccessStatus.ACTIVE,
          
        },
      });

     
      await tx.userActivity.create({
        data: {
          userId: subscriptionRequest.userId,
          activityType: "SUBSCRIPTION_UPDATE",
          description: `User test access to ${subscriptionRequest.requestedDays} tests activated`,
        },
      });

   
      await tx.userSubscriptionRequest.update({
        where: { id: requestId },
        data: { status: UserRequestStatus.ACCEPTED },
      });

        await tx.user.update({
            where: { id: subscriptionRequest.userId },
            data: { languageId: subscriptionRequest.user.pendingLanguageId ?? undefined },
          });
        

      return { updatedTestAccess };
    }, {maxWait:60000,timeout:60000});
      const firebaseDevice = await prisma.firebaseDevice.findFirst({
        where: { physicalDeviceId: subscriptionRequest.user.physicalAddress },
      });

      if (firebaseDevice) {
        let notificationMessage;
        let notificationTitle;
        let userLanguageCode = subscriptionRequest.user.language?.languageCode || "en";
        switch (userLanguageCode) {
        case "fr":
          notificationMessage =
          "Votre accès aux tests a été activé avec succès!";
          notificationTitle = "Accès Activé!";
          break;
        case "es":
          notificationMessage =
          "¡Su acceso a las pruebas ha sido activado con éxito!";
          notificationTitle = "¡Acceso Activado!";
          break;
        case "rw":
          notificationMessage =
          "Ifatabuguzi ryawe ryemejwe neza!";
          notificationTitle = "Ifatabuguzi!";
          break;
        default:
          notificationMessage =
          "Your test access has been successfully activated!";
          notificationTitle = "Access Activated!";
        }

         sendFCMNotification(
        firebaseDevice.deviceToken,
        notificationTitle,
        notificationMessage,
        {
          channel_id: NOTIFICATION_CHANNELS.SUBSCRIPTIONS,
        },
        );
      }

    return NextResponse.json(
      { 
        success: true, 
        message: "User subscription request accepted successfully.",
        data: result
      },
      { status: 200 }
    );

  } catch (error: any) {
    console.error("Error accepting subscription request:", error);
 
    if (error.message === "NOT_FOUND") {
      return NextResponse.json(
        { error: "Subscription request not found" },
        { status: 404 }
      );
    }

    if (error.message === "INVALID_ACCESS") {
      return NextResponse.json(
        { error: "Test access not found or already processed" },
        { status: 400 }
      );
    }
 
    if (error.code === "P2025") {
      return NextResponse.json(
        { error: "Record not found or already updated" },
        { status: 404 }
      );
    }

    if (error.code === "P2002") {
      return NextResponse.json(
        { error: "Duplicate record violation" },
        { status: 409 }
      );
    }

    return NextResponse.json(
      { error: "Failed to accept subscription request" },
      { status: 500 }
    );
  }
  }
);

export const PATCH = acceptRequestHandler;