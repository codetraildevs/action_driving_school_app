package com.drivingschoolrwandaapp.models.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminUserTest {

    @Test
    fun `getFullName combines first middle and last`() {
        val user = AdminUser(
            firstName = "John",
            middleName = "Paul",
            lastName = "Doe"
        )
        assertEquals("John Paul Doe", user.getFullName())
    }

    @Test
    fun `getFullName tolerates missing names`() {
        val user = AdminUser(firstName = "Jane", lastName = null)
        assertEquals("Jane", user.getFullName())
        assertEquals("", AdminUser().getFullName())
    }

    @Test
    fun `getInitials uses first letters of first and last name`() {
        assertEquals("JD", AdminUser(firstName = "John", lastName = "Doe").getInitials())
        assertEquals("J", AdminUser(firstName = "Jane", lastName = null).getInitials())
        assertEquals("?", AdminUser().getInitials())
    }

    @Test
    fun `isAdmin reflects backend role ids 1 and 2`() {
        assertTrue(AdminUser(role = AdminRole(id = 1, roleName = "super_admin")).isAdmin())
        assertTrue(AdminUser(role = AdminRole(id = 2, roleName = "admin")).isAdmin())
        assertFalse(AdminUser(role = AdminRole(id = 5, roleName = "student")).isAdmin())
        assertFalse(AdminUser(role = null).isAdmin())
        assertFalse(AdminUser().isAdmin())
    }
}
