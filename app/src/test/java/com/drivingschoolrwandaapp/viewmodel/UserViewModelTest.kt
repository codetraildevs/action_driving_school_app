package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.drivingschoolrwandaapp.database.entities.User as DbUser
import com.drivingschoolrwandaapp.models.response.ApiResponse
import com.drivingschoolrwandaapp.models.response.LoginResponse
import com.drivingschoolrwandaapp.repository.Resource
import com.drivingschoolrwandaapp.repository.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

/**
 * Unit tests for [UserViewModel] covering the negative / error paths.
 *
 * [UserRepository] is mocked. Each ViewModel method observes the repository's
 * [LiveData] forever and forwards resources into its own result [MutableLiveData],
 * removing the observer once a terminal (SUCCESS/ERROR) resource arrives.
 *
 * These tests drive the returned LiveData directly and assert:
 *  - LOADING resources are forwarded,
 *  - ERROR resources (with messages) are forwarded,
 *  - re-invocation detaches the previous observer (no leak / no stale results),
 *  - terminal resources cause self-removal of the observer,
 *  - fire-and-forget delegates ([updateUser], [logout], [deleteAccount]) hit the repository.
 *
 * Uses [InstantTaskExecutorRule] so LiveData updates execute synchronously.
 */
class UserViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: UserRepository
    private lateinit var viewModel: UserViewModel

    @Before
    fun setUp() {
        repository = mock(UserRepository::class.java)
        viewModel = UserViewModel(mock(Application::class.java), repository)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> observerCaptor(): ArgumentCaptor<Observer<T>> =
        ArgumentCaptor.forClass(Observer::class.java) as ArgumentCaptor<Observer<T>>

    // ---------------------------------------------------------------------------
    // login
    // ---------------------------------------------------------------------------

    @Test
    fun `login forwards loading resource`() {
        val liveData = MutableLiveData<Resource<LoginResponse>>()
        `when`(repository.login(any(), any(), any())).thenReturn(liveData)

        viewModel.login("user", "pass", "dev-1")
        liveData.setValue(Resource.loading(null))

        assertEquals(Resource.Status.LOADING, viewModel.getLoginResult().value?.status)
    }

    @Test
    fun `login forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<LoginResponse>>()
        `when`(repository.login(any(), any(), any())).thenReturn(liveData)

        viewModel.login("user", "pass", "dev-1")
        liveData.setValue(Resource.error("Invalid credentials", null))

        assertEquals(Resource.Status.ERROR, viewModel.getLoginResult().value?.status)
        assertEquals("Invalid credentials", viewModel.getLoginResult().value?.message)
    }

    @Test
    fun `login forwards success resource`() {
        val liveData = MutableLiveData<Resource<LoginResponse>>()
        `when`(repository.login(any(), any(), any())).thenReturn(liveData)

        viewModel.login("user", "pass", "dev-1")
        val body = LoginResponse(success = true, accessToken = "t")
        liveData.setValue(Resource.success(body))

        assertEquals(Resource.Status.SUCCESS, viewModel.getLoginResult().value?.status)
        assertEquals(body, viewModel.getLoginResult().value?.data)
    }

    @Test
    fun `login removes observer after terminal error arrives`() {
        val liveData = spy(MutableLiveData<Resource<LoginResponse>>())
        `when`(repository.login(any(), any(), any())).thenReturn(liveData)

        viewModel.login("user", "pass", "dev-1")
        val observer = observerCaptor<Resource<LoginResponse>>()
        verify(liveData).observeForever(observer.capture())

        liveData.setValue(Resource.error("boom", null))

        // Terminal state → the ViewModel detaches the SAME observer it registered.
        verify(liveData).removeObserver(observer.value)
    }

    @Test
    fun `login after terminal state does not keep forwarding stale updates`() {
        val liveData = spy(MutableLiveData<Resource<LoginResponse>>())
        `when`(repository.login(any(), any(), any())).thenReturn(liveData)

        viewModel.login("user", "pass", "dev-1")
        liveData.setValue(Resource.error("first error", null))
        // Observer was removed; a second value must not reach the ViewModel result.
        liveData.setValue(Resource.success(LoginResponse(success = true)))

        assertEquals(Resource.Status.ERROR, viewModel.getLoginResult().value?.status)
        assertEquals("first error", viewModel.getLoginResult().value?.message)
    }

    @Test
    fun `re-login detaches the previous observer`() {
        val firstLiveData = spy(MutableLiveData<Resource<LoginResponse>>())
        val secondLiveData = MutableLiveData<Resource<LoginResponse>>()
        `when`(repository.login(any(), any(), any())).thenReturn(firstLiveData, secondLiveData)

        viewModel.login("user", "pass", "dev-1")
        val firstObserver = observerCaptor<Resource<LoginResponse>>()
        verify(firstLiveData).observeForever(firstObserver.capture())

        viewModel.login("user", "pass", "dev-2")

        verify(firstLiveData).removeObserver(firstObserver.value)

        // The second login's result must be the one that surfaces.
        secondLiveData.setValue(Resource.error("second login error", null))
        assertEquals("second login error", viewModel.getLoginResult().value?.message)
    }

    // ---------------------------------------------------------------------------
    // forgotPassword
    // ---------------------------------------------------------------------------

    @Test
    fun `forgotPassword forwards loading and error resources`() {
        val liveData = MutableLiveData<Resource<ApiResponse<Void>>>()
        `when`(repository.forgotPassword(any())).thenReturn(liveData)

        viewModel.forgotPassword("user@example.com")
        liveData.setValue(Resource.loading(null))
        assertEquals(Resource.Status.LOADING, viewModel.getForgotPasswordResult().value?.status)

        liveData.setValue(Resource.error("Email not found", null))
        assertEquals(Resource.Status.ERROR, viewModel.getForgotPasswordResult().value?.status)
        assertEquals("Email not found", viewModel.getForgotPasswordResult().value?.message)
    }

    @Test
    fun `forgotPassword removes observer after terminal error`() {
        val liveData = spy(MutableLiveData<Resource<ApiResponse<Void>>>())
        `when`(repository.forgotPassword(any())).thenReturn(liveData)

        viewModel.forgotPassword("user@example.com")
        val observer = observerCaptor<Resource<ApiResponse<Void>>>()
        verify(liveData).observeForever(observer.capture())

        liveData.setValue(Resource.error("Email not found", null))

        verify(liveData).removeObserver(observer.value)
    }

    // ---------------------------------------------------------------------------
    // verifyOtp
    // ---------------------------------------------------------------------------

    @Test
    fun `verifyOtp forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<ApiResponse<String>>>()
        `when`(repository.verifyOtp(any(), any())).thenReturn(liveData)

        viewModel.verifyOtp("user@example.com", "123456")
        liveData.setValue(Resource.error("Invalid OTP", null))

        assertEquals(Resource.Status.ERROR, viewModel.getVerifyOtpResult().value?.status)
        assertEquals("Invalid OTP", viewModel.getVerifyOtpResult().value?.message)
    }

    // ---------------------------------------------------------------------------
    // resetPassword
    // ---------------------------------------------------------------------------

    @Test
    fun `resetPassword forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<ApiResponse<Void>>>()
        `when`(repository.resetPassword(any(), any(), any())).thenReturn(liveData)

        viewModel.resetPassword("token", "new", "new")
        liveData.setValue(Resource.error("Token expired", null))

        assertEquals(Resource.Status.ERROR, viewModel.getResetPasswordResult().value?.status)
        assertEquals("Token expired", viewModel.getResetPasswordResult().value?.message)
    }

    // ---------------------------------------------------------------------------
    // changePassword
    // ---------------------------------------------------------------------------

    @Test
    fun `changePassword forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<ApiResponse<Void>>>()
        `when`(repository.changePassword(any(), any(), any())).thenReturn(liveData)

        viewModel.changePassword("old", "new", "new")
        liveData.setValue(Resource.error("Wrong current password", null))

        assertEquals(Resource.Status.ERROR, viewModel.getChangePasswordResult().value?.status)
        assertEquals("Wrong current password", viewModel.getChangePasswordResult().value?.message)
    }

    // ---------------------------------------------------------------------------
    // loadProfile
    // ---------------------------------------------------------------------------

    @Test
    fun `loadProfile forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<DbUser>>()
        `when`(repository.getProfile()).thenReturn(liveData)

        viewModel.loadProfile()
        liveData.setValue(Resource.error("Profile fetch failed", null))

        assertEquals(Resource.Status.ERROR, viewModel.getUserLiveData().value?.status)
        assertEquals("Profile fetch failed", viewModel.getUserLiveData().value?.message)
    }

    @Test
    fun `loadProfile forwards success resource`() {
        val liveData = MutableLiveData<Resource<DbUser>>()
        `when`(repository.getProfile()).thenReturn(liveData)

        viewModel.loadProfile()
        val user = DbUser(id = 7, firstName = "Alice")
        liveData.setValue(Resource.success(user))

        assertEquals(Resource.Status.SUCCESS, viewModel.getUserLiveData().value?.status)
        assertEquals(user, viewModel.getUserLiveData().value?.data)
    }

    @Test
    fun `loadProfile removes observer after terminal error`() {
        val liveData = spy(MutableLiveData<Resource<DbUser>>())
        `when`(repository.getProfile()).thenReturn(liveData)

        viewModel.loadProfile()
        val observer = observerCaptor<Resource<DbUser>>()
        verify(liveData).observeForever(observer.capture())

        liveData.setValue(Resource.error("Profile fetch failed", null))

        verify(liveData).removeObserver(observer.value)
    }

    // ---------------------------------------------------------------------------
    // sleepSubscription
    // ---------------------------------------------------------------------------

    @Test
    fun `sleepSubscription forwards error resource with message`() {
        val liveData = MutableLiveData<Resource<ApiResponse<Void>>>()
        `when`(repository.sleepSubscription(anyInt())).thenReturn(liveData)

        viewModel.sleepSubscription(41)
        liveData.setValue(Resource.error("Not subscribed", null))

        assertEquals(Resource.Status.ERROR, viewModel.getSleepSubscriptionResult().value?.status)
        assertEquals("Not subscribed", viewModel.getSleepSubscriptionResult().value?.message)
    }

    @Test
    fun `sleepSubscription passes languageId to repository`() {
        val liveData = MutableLiveData<Resource<ApiResponse<Void>>>()
        `when`(repository.sleepSubscription(anyInt())).thenReturn(liveData)

        viewModel.sleepSubscription(48)

        verify(repository).sleepSubscription(48)
    }

    // ---------------------------------------------------------------------------
    // Fire-and-forget delegates
    // ---------------------------------------------------------------------------

    @Test
    fun `updateUser delegates to repository`() {
        val user = DbUser(id = 3, firstName = "Bob")

        viewModel.updateUser(user)

        verify(repository).updateUser(user)
    }

    @Test
    fun `logout delegates to repository`() {
        viewModel.logout()

        verify(repository).logout()
    }

    @Test
    fun `deleteAccount delegates to repository`() {
        viewModel.deleteAccount()

        verify(repository).deleteAccount()
    }

    // ---------------------------------------------------------------------------
    // Initial state
    // ---------------------------------------------------------------------------

    @Test
    fun `results are null before any operation`() {
        assertNull(viewModel.getLoginResult().value)
        assertNull(viewModel.getForgotPasswordResult().value)
        assertNull(viewModel.getVerifyOtpResult().value)
        assertNull(viewModel.getResetPasswordResult().value)
        assertNull(viewModel.getChangePasswordResult().value)
        assertNull(viewModel.getSleepSubscriptionResult().value)
        assertNull(viewModel.getUserLiveData().value)
    }

    @Test
    fun `never removes observer when only loading arrives`() {
        val liveData = spy(MutableLiveData<Resource<LoginResponse>>())
        `when`(repository.login(any(), any(), any())).thenReturn(liveData)

        viewModel.login("user", "pass", "dev-1")
        val observer = observerCaptor<Resource<LoginResponse>>()
        verify(liveData).observeForever(observer.capture())

        liveData.setValue(Resource.loading(null))

        verify(liveData, never()).removeObserver(observer.value)
    }
}
