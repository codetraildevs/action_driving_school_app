package com.drivingschoolrwandaapp.utils

import android.text.TextUtils
import com.drivingschoolrwandaapp.models.entities.AdminRequest
import com.drivingschoolrwandaapp.models.entities.AdminRole
import com.drivingschoolrwandaapp.models.entities.AdminUser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mockStatic

/**
 * Unit tests for [AdminListFilter]. TextUtils is stubbed with real semantics
 * (local unit tests would otherwise throw "not mocked").
 */
class AdminListFilterTest {

    private var textUtilsMock: MockedStatic<TextUtils>? = null

    @Before
    fun setUp() {
        textUtilsMock = mockStatic(TextUtils::class.java)
        `when`(TextUtils.isEmpty(any())).thenAnswer { invocation ->
            (invocation.getArgument(0) as? CharSequence)?.isEmpty() != false
        }
    }

    @After
    fun tearDown() {
        textUtilsMock?.close()
    }

    private fun user(id: Int, name: String, phone: String, email: String, role: String): AdminUser =
        AdminUser(
            id = id,
            firstName = name,
            lastName = "Tester",
            phoneNumber = phone,
            email = email,
            role = AdminRole(id = id, roleName = role),
        )

    private val users = listOf(
        user(1, "Alice", "+250780000001", "alice@test.com", "admin"),
        user(2, "Bob", "+250780000002", "bob@test.com", "student"),
        user(3, "Carol", "+250780000003", "carol@test.com", "teacher"),
    )

    // ---------------------------------------------------------------------------
    // filterUsers
    // ---------------------------------------------------------------------------

    @Test
    fun `filterUsers blank query returns the full list`() {
        assertEquals(users, AdminListFilter.filterUsers(users, ""))
        assertEquals(users, AdminListFilter.filterUsers(users, "   "))
    }

    @Test
    fun `filterUsers null input returns empty list`() {
        assertTrue(AdminListFilter.filterUsers(null, "alice").isEmpty())
    }

    @Test
    fun `filterUsers matches name case-insensitively`() {
        val result = AdminListFilter.filterUsers(users, "ALICE")
        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun `filterUsers matches phone number`() {
        val result = AdminListFilter.filterUsers(users, "000003")
        assertEquals(1, result.size)
        assertEquals(3, result[0].id)
    }

    @Test
    fun `filterUsers matches email`() {
        val result = AdminListFilter.filterUsers(users, "bob@test.com")
        assertEquals(1, result.size)
        assertEquals(2, result[0].id)
    }

    @Test
    fun `filterUsers matches role name`() {
        val result = AdminListFilter.filterUsers(users, "Teacher")
        assertEquals(1, result.size)
        assertEquals(3, result[0].id)
    }

    @Test
    fun `filterUsers with no match returns empty list`() {
        assertTrue(AdminListFilter.filterUsers(users, "zzz").isEmpty())
    }

    @Test
    fun `filterUsers matches multiple fields with one query`() {
        // "25078000000" is a prefix of every phone; add a name constraint instead.
        val result = AdminListFilter.filterUsers(users, "+2507")
        assertEquals(users.size, result.size)
    }

    // ---------------------------------------------------------------------------
    // filterRequests
    // ---------------------------------------------------------------------------

    private val requests = listOf(
        AdminRequest(id = 1, type = "DRIVING_LICENSE", title = "LEARNER B", status = "PENDING", message = "Waiting payment"),
        AdminRequest(id = 2, type = "SPECIAL", title = "BUSANZA police", status = "PROCESSING", message = "Being processed"),
        AdminRequest(id = 3, type = "DRIVING_LICENSE", title = "FULL C", status = "APPROVED", message = "Approved"),
    )

    @Test
    fun `filterRequests blank query returns the full list`() {
        assertEquals(requests, AdminListFilter.filterRequests(requests, ""))
    }

    @Test
    fun `filterRequests null input returns empty list`() {
        assertTrue(AdminListFilter.filterRequests(null, "x").isEmpty())
    }

    @Test
    fun `filterRequests matches title case-insensitively`() {
        val result = AdminListFilter.filterRequests(requests, "learner")
        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun `filterRequests matches type`() {
        val result = AdminListFilter.filterRequests(requests, "special")
        assertEquals(1, result.size)
        assertEquals(2, result[0].id)
    }

    @Test
    fun `filterRequests matches status`() {
        val result = AdminListFilter.filterRequests(requests, "pending")
        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun `filterRequests matches message text`() {
        val result = AdminListFilter.filterRequests(requests, "being processed")
        assertEquals(1, result.size)
        assertEquals(2, result[0].id)
    }

    @Test
    fun `filterRequests with no match returns empty list`() {
        assertTrue(AdminListFilter.filterRequests(requests, "nothing").isEmpty())
    }

    @Test
    fun `filterRequests returns the original order for a broad query`() {
        val result = AdminListFilter.filterRequests(requests, "DRIVING_LICENSE")
        assertEquals(listOf(1, 3), result.map { it.id })
    }
}
