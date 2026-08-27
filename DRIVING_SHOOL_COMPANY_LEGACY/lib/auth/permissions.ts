
export const PERMISSIONS = {
  // User Management
  USER_READ: 'user:read',
  USER_CREATE: 'user:create',
  USER_UPDATE: 'user:update',
  USER_DELETE: 'user:delete',
  
  // PDF Management
  PDF_READ: 'pdf:read',
  PDF_UPLOAD: 'pdf:upload',
  PDF_UPDATE: 'pdf:update',
  PDF_DELETE: 'pdf:delete',
  
  // Test Management
  TEST_READ: 'test:read',
  TEST_CREATE: 'test:create',
  TEST_UPDATE: 'test:update',
  TEST_DELETE: 'test:delete',
  
  // Subscription Management
  SUBSCRIPTION_READ: 'subscription:read',
  SUBSCRIPTION_MANAGE: 'subscription:manage',
  
  // System Settings
  SETTINGS_READ: 'settings:read',
  SETTINGS_WRITE: 'settings:write',
  
  // Reports & Analytics
  REPORTS_VIEW: 'reports:view',
  ANALYTICS_VIEW: 'analytics:view',
} as const;

export const ROLE_PERMISSIONS = {
  SUPER_ADMIN: Object.values(PERMISSIONS),
  ADMIN: [
    PERMISSIONS.USER_READ,
    PERMISSIONS.USER_UPDATE,
    PERMISSIONS.PDF_READ,
    PERMISSIONS.PDF_UPLOAD,
    PERMISSIONS.PDF_UPDATE,
    PERMISSIONS.TEST_READ,
    PERMISSIONS.TEST_CREATE,
    PERMISSIONS.TEST_UPDATE,
    PERMISSIONS.SUBSCRIPTION_READ,
    PERMISSIONS.REPORTS_VIEW,
  ],
  CONTENT_MANAGER: [
    PERMISSIONS.PDF_READ,
    PERMISSIONS.PDF_UPLOAD,
    PERMISSIONS.PDF_UPDATE,
    PERMISSIONS.TEST_READ,
    PERMISSIONS.TEST_CREATE,
    PERMISSIONS.TEST_UPDATE,
  ],
  USER: [
    PERMISSIONS.PDF_READ,
    PERMISSIONS.TEST_READ,
  ],
};

export function hasPermission(userPermissions: string[], required: string | string[]): boolean {
  // const requiredPerms = Array.isArray(required) ? required : [required];
  // return requiredPerms.every(perm => userPermissions.includes(perm));
  return true
}