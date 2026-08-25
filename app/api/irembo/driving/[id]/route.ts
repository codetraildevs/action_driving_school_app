import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prismaDB'
import { sendFCMNotification } from '@/lib/notification'
import { NOTIFICATION_CHANNELS } from '@/lib/types'
 
interface RouteParams {
  params: {
    id: string
  }
}

export async function GET(request: NextRequest, { params }: RouteParams) {
  try {
    const { id } = params

    const requestData = await prisma.iremboDrivingLicenseRequest.findUnique({
      where: { 
       id: parseInt(id) 
      },
      include: {
        user: {
          select: {
            id: true,
            firstName: true,
            lastName: true,
            phoneNumber: true,
            email: true
          }
        }
      }
    })

    if (!requestData) {
      const response  = {
        success: false,
        error: 'Driving license request not found'
      }
      return NextResponse.json(response, { status: 404 })
    }

    const response  = {
      success: true,
      data: requestData
    }

    return NextResponse.json(response)
  } catch (error) {
    console.error('GET /api/irembo/driving/[id] error:', error)
    const response  = {
      success: false,
      error: 'Failed to fetch driving license request'
    }
    return NextResponse.json(response, { status: 500 })
  }
}

 
function getNotificationContent(
  type: "STATUS" | "PAYMENT" | "PROGRESS",
  value: string | number,
  lang: string
) {
  const messages: any = {
    STATUS: {
      APPROVED: {
        en: ["Request Approved 🎉", "Your driving license request has been approved."],
        fr: ["Demande approuvée 🎉", "Votre demande de permis a été approuvée."],
        rw: ["Byemejwe 🎉", "Gusaba uruhushya rwo gutwara byemejwe."]
      },
      PROCESSING: {
        en: ["Processing Request", "Your driving license request is being processed."],
        fr: ["Traitement en cours", "Votre demande est en cours de traitement."],
        rw: ["Birimo gutunganywa", "Gusaba kwawe biri gutunganywa."]
      },
      ACTION: {
        en: ["Action Required ⚠️", "Please complete the next step of your request."],
        fr: ["Action requise ⚠️", "Veuillez compléter l’étape suivante."],
        rw: ["Igikorwa kirakenewe ⚠️", "Nyamuneka komeza intambwe ikurikira."]
      },
      REJECTED: {
        en: ["Request Rejected ❌", "Your driving license request was rejected."],
        fr: ["Demande rejetée ❌", "Votre demande a été rejetée."],
        rw: ["Byanze ❌", "Gusaba kwawe kwanzwe."]
      }
    },

    PAYMENT: {
      PAID: {
        en: ["Payment Successful 💳", "Your payment was received successfully."],
        fr: ["Paiement réussi 💳", "Votre paiement a été reçu."],
        rw: ["Kwishyura byagenze neza 💳", "Wishyuye neza."]
      },
      PENDING: {
        en: ["Payment Pending", "Please complete your payment."],
        fr: ["Paiement en attente", "Veuillez compléter le paiement."],
        rw: ["Kwishyura birategerejwe", "Nyamuneka rangiza kwishyura."]
      }
    },

    PROGRESS: {
      en: ["Progress Update 📊", `Your request is ${value}% complete.`],
      fr: ["Progression 📊", `Votre demande est complétée à ${value}%.`],
      rw: ["Iterambere 📊", `Gusaba kwawe kugeze kuri ${value}%.`]
    }
  };

  if (type === "PROGRESS") return messages.PROGRESS[lang] || messages.PROGRESS.en;

  return messages[type]?.[value]?.[lang] || messages[type]?.[value]?.en;
}

/* ----------------------------------------------------
   PUT – Update Driving License Request
---------------------------------------------------- */
export async function PUT(request: NextRequest, { params }: RouteParams) {
  try {
    const { id } = params;
    const body = await request.json();

    const existingRequest = await prisma.iremboDrivingLicenseRequest.findUnique({
      where: { id: parseInt(id) },
      include: {
        user: {
          include: {
            language: true,
            devices: true
          }
        }
      }
    });

    if (!existingRequest) {
      return NextResponse.json(
        { success: false, error: "Driving license request not found" },
        { status: 404 }
      );
    }

    /* ---------- Update request ---------- */
    const updatedRequest = await prisma.iremboDrivingLicenseRequest.update({
      where: { id: parseInt(id) },
      data: body
    });

    const user = existingRequest.user;
    const lang = user.language?.languageCode || "en";
    const device = user.devices[0];

    if (!device) {
      return NextResponse.json({
        success: true,
        data: updatedRequest,
        message: "Updated (no device for notification)"
      });
    }

    const firebaseDevice = await prisma.firebaseDevice.findFirst({
      where: { physicalDeviceId: device.physicalAddress }
    });

    if (!firebaseDevice) {
      return NextResponse.json({
        success: true,
        data: updatedRequest,
        message: "Updated (no firebase device found)"
      });
    }

    /* ---------- Detect changes ---------- */
    const notificationsToSend: { title: string; message: string }[] = [];

    if (body.status && body.status !== existingRequest.status) {
      const [title, message] = getNotificationContent(
        "STATUS",
        body.status,
        lang
      );
      notificationsToSend.push({ title, message });
    }

    if (body.paymentStatus && body.paymentStatus !== existingRequest.paymentStatus) {
      const [title, message] = getNotificationContent(
        "PAYMENT",
        body.paymentStatus,
        lang
      );
      notificationsToSend.push({ title, message });
    }

    if (
      typeof body.completionPercentage === "number" &&
      body.completionPercentage !== existingRequest.completionPercentage
    ) {
      const [title, message] = getNotificationContent(
        "PROGRESS",
        body.completionPercentage,
        lang
      );
      notificationsToSend.push({ title, message });
    }

    /* ---------- Send & Store Notifications ---------- */
    for (const notif of notificationsToSend) {
      const exists = await prisma.userNotification.findFirst({
        where: {
          userId: user.id,
          title: notif.title,
          message: notif.message
        }
      });

      if (!exists) {
        await prisma.userNotification.create({
          data: {
            userId: user.id,
            title: notif.title,
            message: notif.message
          }
        });

        await sendFCMNotification(
          firebaseDevice.deviceToken,
          notif.title,
          notif.message,
          {
            channel_id: NOTIFICATION_CHANNELS.APPLICATIONS,
          }
        );
      }
    }

    return NextResponse.json({
      success: true,
      data: updatedRequest,
      message: "Driving license request updated successfully"
    });

  } catch (error) {
    console.error("PUT /api/irembo/driving/[id] error:", error);
    return NextResponse.json(
      { success: false, error: "Failed to update driving license request" },
      { status: 500 }
    );
  }
}


// DELETE - Delete driving license request
export async function DELETE(request: NextRequest, { params }: RouteParams) {
  try {
    const { id } = params

    await prisma.iremboDrivingLicenseRequest.delete({
      where: { 
       id: parseInt(id) 
      }
    })

    const response  = {
      success: true,
      message: 'Driving license request deleted successfully'
    }

    return NextResponse.json(response)
  } catch (error) {
    console.error('DELETE /api/irembo/driving/[id] error:', error)
    const response  = {
      success: false,
      error: 'Failed to delete driving license request'
    }
    return NextResponse.json(response, { status: 500 })
  }
}