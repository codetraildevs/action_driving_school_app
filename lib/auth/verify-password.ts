import bcrypt from 'bcryptjs';

/** Every login must prove knowledge of the stored credential, including admins. */
export async function verifyLoginPassword(password: string, storedHash: string): Promise<boolean> {
  return bcrypt.compare(password, storedHash);
}
