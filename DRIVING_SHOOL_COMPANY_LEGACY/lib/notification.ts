import { getAdmin } from "./firebaseAdmin";

export async function sendFCMNotification(deviceToken: String, title: string, body: string, data = {}) {
  try {
    const message = {
      token: deviceToken,
      notification: {
        title: title,
        body: body
      },
      data: data
    };

    const response = await getAdmin().messaging().send(message as any);

    return response;
  } catch (error) {
    console.error('Error sending message:', error);
    throw error;
  }
}

export async function sendFCMNotificationToMultiple(deviceTokens: string[], title: string, body: string, data = {}) {
  try {
    const message = {
      tokens: deviceTokens,
      notification: {
        title: title,
        body: body
      },
      data
    };

    const response = await getAdmin().messaging().sendEachForMulticast(message as any);
    return response;
  } catch (error) {
    console.error('Error sending multicast message:', error);
    throw error;
  }
}
