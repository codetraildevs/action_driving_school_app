package com.drivingschoolrwandaapp.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleUtilsTest {

    @Test
    fun `super admin and admin unlock the console`() {
        assertTrue(RoleUtils.isAdminRole(RoleUtils.ROLE_SUPER_ADMIN))
        assertTrue(RoleUtils.isAdminRole(RoleUtils.ROLE_ADMIN))
    }

    @Test
    fun `staff and student roles do not unlock the console`() {
        assertFalse(RoleUtils.isAdminRole(RoleUtils.ROLE_CONTENT_MANAGER))
        assertFalse(RoleUtils.isAdminRole(RoleUtils.ROLE_TEACHER))
        assertFalse(RoleUtils.isAdminRole(RoleUtils.ROLE_STUDENT))
        assertFalse(RoleUtils.isAdminRole(RoleUtils.ROLE_PREMIUM_USER))
        assertFalse(RoleUtils.isAdminRole(RoleUtils.ROLE_FREE_USER))
        assertFalse(RoleUtils.isAdminRole(RoleUtils.ROLE_MODERATOR))
        assertFalse(RoleUtils.isAdminRole(RoleUtils.ROLE_SUPPORT_STAFF))
        assertFalse(RoleUtils.isAdminRole(RoleUtils.ROLE_GUEST))
    }

    @Test
    fun `unknown or missing role does not unlock the console`() {
        assertFalse(RoleUtils.isAdminRole(0))
        assertFalse(RoleUtils.isAdminRole(-1))
        assertFalse(RoleUtils.isAdminRole(99))
    }
}
