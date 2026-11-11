import type { Prisma } from "@/lib/generated/prisma";

export async function deleteAccount(tx: Prisma.TransactionClient, userId: number) {
  // Some installations lack the devices-to-users cascade. Registration checks
  // this table directly, so an orphaned binding would still block the phone.
  await tx.device.deleteMany({ where: { userId } });
  await tx.user.delete({ where: { id: userId }, select: { id: true } });
}
