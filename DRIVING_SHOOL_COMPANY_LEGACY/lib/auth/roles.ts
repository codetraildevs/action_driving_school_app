/**
 * Shared role helpers for the one-app role-based flow.
 *
 * The Android app treats roles 1 (super_admin) and 2 (admin) as console roles
 * (see RoleUtils on the client). All backend admin endpoints and auth logic
 * must grant the same set, so console users are never locked out of their own
 * console.
 *
 * NOTE: role_name values in the hosted DB are display-formatted ("Super Admin",
 * "Student"), while older seeds used snake_case ("super_admin", "student").
 * The check therefore normalizes both to lowercase with spaces/underscores
 * removed so console users are never locked out by formatting differences.
 */
export const CONSOLE_ROLE_NAMES = ["admin", "super_admin"] as const;

/** Normalizes role names: "Super Admin" / "super_admin" / "SUPER ADMIN" -> "superadmin". */
export function normalizeRoleName(roleName?: string | null): string {
  if (typeof roleName !== "string") return "";
  return roleName.toLowerCase().replace(/[\s_-]+/g, "");
}

export function isAdminRoleName(roleName?: string | null): boolean {
  const normalized = normalizeRoleName(roleName);
  return (CONSOLE_ROLE_NAMES as readonly string[]).some(
    (name) => normalizeRoleName(name) === normalized,
  );
}
