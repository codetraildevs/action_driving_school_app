package com.drivingschoolrwandaapp.utils;

/**
 * Role identifiers shared with the backend (see user_roles table in
 * DRIVING_SHOOL_COMPANY_LEGACY). The login/profile response returns the role
 * as a numeric id on {@code user.role}.
 */
public final class RoleUtils {

    public static final int ROLE_SUPER_ADMIN = 1;
    public static final int ROLE_ADMIN = 2;
    public static final int ROLE_CONTENT_MANAGER = 3;
    public static final int ROLE_TEACHER = 4;
    public static final int ROLE_STUDENT = 5;
    public static final int ROLE_PREMIUM_USER = 6;
    public static final int ROLE_FREE_USER = 7;
    public static final int ROLE_MODERATOR = 8;
    public static final int ROLE_SUPPORT_STAFF = 9;
    public static final int ROLE_GUEST = 10;

    private RoleUtils() {
    }

    /** Whether the given role id unlocks the admin console (roles 1 and 2). */
    public static boolean isAdminRole(int roleId) {
        return roleId == ROLE_SUPER_ADMIN || roleId == ROLE_ADMIN;
    }

    /**
     * Returns the string resource id of the human-readable name for the given
     * role id, or {@code R.string.role_student} for unknown/unspecified roles.
     */
    public static int getRoleNameRes(int roleId) {
        switch (roleId) {
            case ROLE_SUPER_ADMIN:
                return com.drivingschoolrwandaapp.R.string.role_super_admin;
            case ROLE_ADMIN:
                return com.drivingschoolrwandaapp.R.string.role_admin;
            case ROLE_CONTENT_MANAGER:
                return com.drivingschoolrwandaapp.R.string.role_content_manager;
            case ROLE_TEACHER:
                return com.drivingschoolrwandaapp.R.string.role_teacher;
            case ROLE_PREMIUM_USER:
                return com.drivingschoolrwandaapp.R.string.role_premium_user;
            case ROLE_FREE_USER:
                return com.drivingschoolrwandaapp.R.string.role_free_user;
            case ROLE_MODERATOR:
                return com.drivingschoolrwandaapp.R.string.role_moderator;
            case ROLE_SUPPORT_STAFF:
                return com.drivingschoolrwandaapp.R.string.role_support_staff;
            case ROLE_GUEST:
                return com.drivingschoolrwandaapp.R.string.role_guest;
            default:
                return com.drivingschoolrwandaapp.R.string.role_student;
        }
    }
}
