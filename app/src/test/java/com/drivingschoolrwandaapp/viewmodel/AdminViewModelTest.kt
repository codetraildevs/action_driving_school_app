package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.drivingschoolrwandaapp.models.entities.AdminDashboardStats
import com.drivingschoolrwandaapp.models.entities.AdminRequest
import com.drivingschoolrwandaapp.models.entities.AdminUser
import com.drivingschoolrwandaapp.models.entities.AdminUserDetail
import com.drivingschoolrwandaapp.repository.AdminRepository
import com.drivingschoolrwandaapp.repository.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

/**
 * Unit tests for [AdminViewModel].
 *
 * [AdminRepository] is mocked. Each refresh method observes the repository's
 * [LiveData] forever and forwards resources into its own result
 * [MutableLiveData], removing the observer once a terminal (SUCCESS/ERROR)
 * resource arrives. These tests drive the returned LiveData directly and
 * assert the same forwarding / self-removal contract as [UserViewModelTest].
 */
class AdminViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: AdminRepository
    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        repository = mock(AdminRepository::class.java)
        viewModel = AdminViewModel(mock(Application::class.java), repository)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> observerCaptor(): ArgumentCaptor<Observer<T>> =
        ArgumentCaptor.forClass(Observer::class.java) as ArgumentCaptor<Observer<T>>

    // ---------------------------------------------------------------------------
    // refreshDashboard
    // ---------------------------------------------------------------------------

    @Test
    fun `refreshDashboard forwards loading and success resources`() {
        val liveData = MutableLiveData<Resource<AdminDashboardStats>>()
        `when`(repository.fetchDashboardStats()).thenReturn(liveData)

        viewModel.refreshDashboard()
        liveData.setValue(Resource.loading(null))
        assertEquals(Resource.Status.LOADING, viewModel.getDashboardStats().value?.status)

        val stats = AdminDashboardStats(totalUsers = 42)
        liveData.setValue(Resource.success(stats))
        assertEquals(Resource.Status.SUCCESS, viewModel.getDashboardStats().value?.status)
        assertEquals(stats, viewModel.getDashboardStats().value?.data)
    }

    @Test
    fun `refreshDashboard forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<AdminDashboardStats>>()
        `when`(repository.fetchDashboardStats()).thenReturn(liveData)

        viewModel.refreshDashboard()
        liveData.setValue(Resource.error("Network error", null))

        assertEquals(Resource.Status.ERROR, viewModel.getDashboardStats().value?.status)
        assertEquals("Network error", viewModel.getDashboardStats().value?.message)
    }

    @Test
    fun `refreshDashboard removes observer after terminal error arrives`() {
        val liveData = spy(MutableLiveData<Resource<AdminDashboardStats>>())
        `when`(repository.fetchDashboardStats()).thenReturn(liveData)

        viewModel.refreshDashboard()
        val observer = observerCaptor<Resource<AdminDashboardStats>>()
        verify(liveData).observeForever(observer.capture())

        liveData.setValue(Resource.error("boom", null))

        verify(liveData).removeObserver(observer.value)
    }

    @Test
    fun `refreshDashboard after terminal state does not keep forwarding stale updates`() {
        val liveData = spy(MutableLiveData<Resource<AdminDashboardStats>>())
        `when`(repository.fetchDashboardStats()).thenReturn(liveData)

        viewModel.refreshDashboard()
        liveData.setValue(Resource.error("first error", null))
        liveData.setValue(Resource.success(AdminDashboardStats(totalUsers = 1)))

        assertEquals(Resource.Status.ERROR, viewModel.getDashboardStats().value?.status)
        assertEquals("first error", viewModel.getDashboardStats().value?.message)
    }

    @Test
    fun `re-refreshDashboard detaches the previous observer`() {
        val firstLiveData = spy(MutableLiveData<Resource<AdminDashboardStats>>())
        val secondLiveData = MutableLiveData<Resource<AdminDashboardStats>>()
        `when`(repository.fetchDashboardStats()).thenReturn(firstLiveData, secondLiveData)

        viewModel.refreshDashboard()
        val firstObserver = observerCaptor<Resource<AdminDashboardStats>>()
        verify(firstLiveData).observeForever(firstObserver.capture())

        viewModel.refreshDashboard()

        verify(firstLiveData).removeObserver(firstObserver.value)

        secondLiveData.setValue(Resource.error("second error", null))
        assertEquals("second error", viewModel.getDashboardStats().value?.message)
    }

    // ---------------------------------------------------------------------------
    // refreshUsers
    // ---------------------------------------------------------------------------

    @Test
    fun `refreshUsers forwards loading and success resources`() {
        val liveData = MutableLiveData<Resource<List<AdminUser>>>()
        `when`(repository.fetchUsers()).thenReturn(liveData)
        // Success also triggers a dashboard refresh; stub it so the mock is quiet.
        `when`(repository.fetchDashboardStats()).thenReturn(MutableLiveData())

        viewModel.refreshUsers()
        liveData.setValue(Resource.loading(null))
        assertEquals(Resource.Status.LOADING, viewModel.getUsers().value?.status)

        val users = listOf(AdminUser(id = 1, firstName = "Alice"))
        liveData.setValue(Resource.success(users))
        assertEquals(Resource.Status.SUCCESS, viewModel.getUsers().value?.status)
        assertEquals(users, viewModel.getUsers().value?.data)
    }

    @Test
    fun `refreshUsers success triggers a dashboard refresh`() {
        val usersLiveData = MutableLiveData<Resource<List<AdminUser>>>()
        `when`(repository.fetchUsers()).thenReturn(usersLiveData)
        val dashboardLiveData = spy(MutableLiveData<Resource<AdminDashboardStats>>())
        `when`(repository.fetchDashboardStats()).thenReturn(dashboardLiveData)

        viewModel.refreshUsers()
        usersLiveData.setValue(Resource.success(emptyList()))

        // The dashboard stats LiveData was observed, and a fetch was issued.
        val dashboardObserver = observerCaptor<Resource<AdminDashboardStats>>()
        verify(dashboardLiveData).observeForever(dashboardObserver.capture())

        dashboardLiveData.setValue(Resource.success(AdminDashboardStats(totalUsers = 7)))
        assertEquals(Resource.Status.SUCCESS, viewModel.getDashboardStats().value?.status)
        assertEquals(7L, viewModel.getDashboardStats().value?.data?.totalUsers)
    }

    @Test
    fun `refreshUsers error does not trigger a dashboard refresh`() {
        val usersLiveData = MutableLiveData<Resource<List<AdminUser>>>()
        `when`(repository.fetchUsers()).thenReturn(usersLiveData)

        viewModel.refreshUsers()
        usersLiveData.setValue(Resource.error("Forbidden", null))

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
            .fetchDashboardStats()
    }

    @Test
    fun `refreshUsers forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<List<AdminUser>>>()
        `when`(repository.fetchUsers()).thenReturn(liveData)

        viewModel.refreshUsers()
        liveData.setValue(Resource.error("Forbidden", null))

        assertEquals(Resource.Status.ERROR, viewModel.getUsers().value?.status)
        assertEquals("Forbidden", viewModel.getUsers().value?.message)
    }

    @Test
    fun `refreshUsers removes observer after terminal error arrives`() {
        val liveData = spy(MutableLiveData<Resource<List<AdminUser>>>())
        `when`(repository.fetchUsers()).thenReturn(liveData)

        viewModel.refreshUsers()
        val observer = observerCaptor<Resource<List<AdminUser>>>()
        verify(liveData).observeForever(observer.capture())

        liveData.setValue(Resource.error("boom", null))

        verify(liveData).removeObserver(observer.value)
    }

    // ---------------------------------------------------------------------------
    // refreshRequests
    // ---------------------------------------------------------------------------

    @Test
    fun `refreshRequests forwards loading and success resources`() {
        val liveData = MutableLiveData<Resource<List<AdminRequest>>>()
        `when`(repository.fetchRequests()).thenReturn(liveData)
        // Success also triggers a dashboard refresh; stub it so the mock is quiet.
        `when`(repository.fetchDashboardStats()).thenReturn(MutableLiveData())

        viewModel.refreshRequests()
        liveData.setValue(Resource.loading(null))
        assertEquals(Resource.Status.LOADING, viewModel.getRequests().value?.status)

        val requests = listOf(AdminRequest(id = 9, type = "DRIVING_LICENSE"))
        liveData.setValue(Resource.success(requests))
        assertEquals(Resource.Status.SUCCESS, viewModel.getRequests().value?.status)
        assertEquals(requests, viewModel.getRequests().value?.data)
    }

    @Test
    fun `refreshRequests success triggers a dashboard refresh`() {
        val requestsLiveData = MutableLiveData<Resource<List<AdminRequest>>>()
        `when`(repository.fetchRequests()).thenReturn(requestsLiveData)
        val dashboardLiveData = spy(MutableLiveData<Resource<AdminDashboardStats>>())
        `when`(repository.fetchDashboardStats()).thenReturn(dashboardLiveData)

        viewModel.refreshRequests()
        requestsLiveData.setValue(Resource.success(emptyList()))

        val dashboardObserver = observerCaptor<Resource<AdminDashboardStats>>()
        verify(dashboardLiveData).observeForever(dashboardObserver.capture())

        dashboardLiveData.setValue(Resource.success(AdminDashboardStats(totalUsers = 3)))
        assertEquals(Resource.Status.SUCCESS, viewModel.getDashboardStats().value?.status)
        assertEquals(3L, viewModel.getDashboardStats().value?.data?.totalUsers)
    }

    @Test
    fun `refreshRequests error does not trigger a dashboard refresh`() {
        val requestsLiveData = MutableLiveData<Resource<List<AdminRequest>>>()
        `when`(repository.fetchRequests()).thenReturn(requestsLiveData)

        viewModel.refreshRequests()
        requestsLiveData.setValue(Resource.error("Unauthorized", null))

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
            .fetchDashboardStats()
    }

    @Test
    fun `refreshRequests forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<List<AdminRequest>>>()
        `when`(repository.fetchRequests()).thenReturn(liveData)

        viewModel.refreshRequests()
        liveData.setValue(Resource.error("Unauthorized", null))

        assertEquals(Resource.Status.ERROR, viewModel.getRequests().value?.status)
        assertEquals("Unauthorized", viewModel.getRequests().value?.message)
    }

    @Test
    fun `refreshRequests removes observer after terminal error arrives`() {
        val liveData = spy(MutableLiveData<Resource<List<AdminRequest>>>())
        `when`(repository.fetchRequests()).thenReturn(liveData)

        viewModel.refreshRequests()
        val observer = observerCaptor<Resource<List<AdminRequest>>>()
        verify(liveData).observeForever(observer.capture())

        liveData.setValue(Resource.error("boom", null))

        verify(liveData).removeObserver(observer.value)
    }

    // ---------------------------------------------------------------------------
    // refreshUserDetail
    // ---------------------------------------------------------------------------

    @Test
    fun `refreshUserDetail forwards loading and success for detail`() {
        val detailLiveData = MutableLiveData<Resource<AdminUserDetail>>()
        val requestsLiveData = MutableLiveData<Resource<List<AdminRequest>>>()
        `when`(repository.fetchUserDetail(5)).thenReturn(detailLiveData)
        `when`(repository.fetchUserRequests(5)).thenReturn(requestsLiveData)

        viewModel.refreshUserDetail(5)
        detailLiveData.setValue(Resource.loading(null))
        assertEquals(Resource.Status.LOADING, viewModel.getUserDetail().value?.status)

        val detail = AdminUserDetail(id = 5, firstName = "Bob")
        detailLiveData.setValue(Resource.success(detail))
        assertEquals(Resource.Status.SUCCESS, viewModel.getUserDetail().value?.status)
        assertEquals(detail, viewModel.getUserDetail().value?.data)
    }

    @Test
    fun `refreshUserDetail forwards the user's requests`() {
        val detailLiveData = MutableLiveData<Resource<AdminUserDetail>>()
        val requestsLiveData = MutableLiveData<Resource<List<AdminRequest>>>()
        `when`(repository.fetchUserDetail(5)).thenReturn(detailLiveData)
        `when`(repository.fetchUserRequests(5)).thenReturn(requestsLiveData)

        viewModel.refreshUserDetail(5)
        val requests = listOf(AdminRequest(id = 9, type = "SPECIAL"))
        requestsLiveData.setValue(Resource.success(requests))

        assertEquals(Resource.Status.SUCCESS, viewModel.getUserRequests().value?.status)
        assertEquals(requests, viewModel.getUserRequests().value?.data)
    }

    @Test
    fun `refreshUserDetail forwards error resource with message`() {
        val detailLiveData = MutableLiveData<Resource<AdminUserDetail>>()
        val requestsLiveData = MutableLiveData<Resource<List<AdminRequest>>>()
        `when`(repository.fetchUserDetail(5)).thenReturn(detailLiveData)
        `when`(repository.fetchUserRequests(5)).thenReturn(requestsLiveData)

        viewModel.refreshUserDetail(5)
        detailLiveData.setValue(Resource.error("User not found", null))

        assertEquals(Resource.Status.ERROR, viewModel.getUserDetail().value?.status)
        assertEquals("User not found", viewModel.getUserDetail().value?.message)
    }

    @Test
    fun `refreshUserDetail removes observers after terminal error arrives`() {
        val detailLiveData = spy(MutableLiveData<Resource<AdminUserDetail>>())
        val requestsLiveData = spy(MutableLiveData<Resource<List<AdminRequest>>>())
        `when`(repository.fetchUserDetail(5)).thenReturn(detailLiveData)
        `when`(repository.fetchUserRequests(5)).thenReturn(requestsLiveData)

        viewModel.refreshUserDetail(5)
        val detailObserver = observerCaptor<Resource<AdminUserDetail>>()
        verify(detailLiveData).observeForever(detailObserver.capture())
        val requestsObserver = observerCaptor<Resource<List<AdminRequest>>>()
        verify(requestsLiveData).observeForever(requestsObserver.capture())

        detailLiveData.setValue(Resource.error("boom", null))
        requestsLiveData.setValue(Resource.error("boom", null))

        verify(detailLiveData).removeObserver(detailObserver.value)
        verify(requestsLiveData).removeObserver(requestsObserver.value)
    }

    @Test
    fun `re-refreshUserDetail detaches previous observers`() {
        val firstDetail = spy(MutableLiveData<Resource<AdminUserDetail>>())
        val firstRequests = spy(MutableLiveData<Resource<List<AdminRequest>>>())
        `when`(repository.fetchUserDetail(5)).thenReturn(firstDetail)
        `when`(repository.fetchUserRequests(5)).thenReturn(firstRequests)

        viewModel.refreshUserDetail(5)
        val firstDetailObserver = observerCaptor<Resource<AdminUserDetail>>()
        verify(firstDetail).observeForever(firstDetailObserver.capture())
        val firstRequestsObserver = observerCaptor<Resource<List<AdminRequest>>>()
        verify(firstRequests).observeForever(firstRequestsObserver.capture())

        val secondDetail = MutableLiveData<Resource<AdminUserDetail>>()
        val secondRequests = MutableLiveData<Resource<List<AdminRequest>>>()
        `when`(repository.fetchUserDetail(6)).thenReturn(secondDetail)
        `when`(repository.fetchUserRequests(6)).thenReturn(secondRequests)
        viewModel.refreshUserDetail(6)

        verify(firstDetail).removeObserver(firstDetailObserver.value)
        verify(firstRequests).removeObserver(firstRequestsObserver.value)

        secondDetail.setValue(Resource.error("second error", null))
        assertEquals("second error", viewModel.getUserDetail().value?.message)
    }

    // ---------------------------------------------------------------------------
    // Initial state
    // ---------------------------------------------------------------------------

    @Test
    fun `results are null before any refresh`() {
        assertNull(viewModel.getDashboardStats().value)
        assertNull(viewModel.getUsers().value)
        assertNull(viewModel.getRequests().value)
        assertNull(viewModel.getUserDetail().value)
        assertNull(viewModel.getUserRequests().value)
    }

    @Test
    fun `never removes observer when only loading arrives`() {
        val liveData = spy(MutableLiveData<Resource<AdminDashboardStats>>())
        `when`(repository.fetchDashboardStats()).thenReturn(liveData)

        viewModel.refreshDashboard()
        val observer = observerCaptor<Resource<AdminDashboardStats>>()
        verify(liveData).observeForever(observer.capture())

        liveData.setValue(Resource.loading(null))

        verify(liveData, org.mockito.Mockito.never()).removeObserver(observer.value)
    }
}
