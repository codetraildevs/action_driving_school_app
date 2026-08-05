package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.drivingschoolrwandaapp.database.entities.SubscriptionPlan as DatabaseSubscriptionPlan
import com.drivingschoolrwandaapp.database.entities.UserSubscriptionWithPlan
import com.drivingschoolrwandaapp.models.entities.SubscriptionPlan
import com.drivingschoolrwandaapp.models.response.ApiResponse
import com.drivingschoolrwandaapp.models.response.UserSubscriptionResponse
import com.drivingschoolrwandaapp.repository.Resource
import com.drivingschoolrwandaapp.repository.SubscriptionRepository
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Unit tests for [SubscriptionViewModel] covering negative / error paths.
 *
 * [SubscriptionRepository] is mocked. For callback-based methods the enqueued
 * [Callback] is captured and driven with mocked [Response]s; for LiveData-based
 * methods ([fetchUserSubscription], [fetchSubscriptionPlans]) the returned
 * [MutableLiveData] is driven directly.
 *
 * Uses [InstantTaskExecutorRule] so LiveData updates execute synchronously.
 */
class SubscriptionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: SubscriptionRepository
    private lateinit var viewModel: SubscriptionViewModel

    private val plan = SubscriptionPlan(id = 7, planName = "Monthly", amount = "5000", duration = 30)

    @Before
    fun setUp() {
        repository = mock(SubscriptionRepository::class.java)
        viewModel = SubscriptionViewModel(mock(Application::class.java), repository)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun <T> callbackCaptor(): ArgumentCaptor<Callback<T>> =
        ArgumentCaptor.forClass(Callback::class.java) as ArgumentCaptor<Callback<T>>

    @Suppress("UNCHECKED_CAST")
    private fun <T> mockCall(): Call<T> = mock(Call::class.java) as Call<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T> mockResponse(): Response<T> = mock(Response::class.java) as Response<T>

    private fun <T> invokeResponse(callback: Callback<T>, successful: Boolean, body: T? = null) {
        val call = mockCall<T>()
        val response = mockResponse<T>()
        `when`(response.isSuccessful).thenReturn(successful)
        `when`(response.body()).thenReturn(body)
        callback.onResponse(call, response)
    }

    private fun <T> invokeFailure(callback: Callback<T>, throwable: Throwable) {
        callback.onFailure(mockCall<T>(), throwable)
    }

    private fun errorBodyJson(json: String): ResponseBody {
        // Build a REAL ResponseBody: string() is final in OkHttp and cannot be mock-stubbed.
        return json.toResponseBody(null)
    }

    // ---------------------------------------------------------------------------
    // subscribeToPlan — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `subscribeToPlan unsuccessful response sets error and stops loading`() {
        val captor = callbackCaptor<UserSubscriptionResponse>()
        viewModel.subscribeToPlan(plan)

        verify(repository).subscribeToPlan(anyInt(), captor.capture())
        val callback = captor.value

        invokeResponse(callback, successful = false)

        assertEquals("Failed to subscribe. Please try again.", viewModel.getError().value)
        assertFalse(viewModel.getIsLoading().value!!)
        assertNull("No success event on failure", viewModel.getNewSubscriptionSuccess().value)
    }

    @Test
    fun `subscribeToPlan response body with success false sets error`() {
        val captor = callbackCaptor<UserSubscriptionResponse>()
        viewModel.subscribeToPlan(plan)

        verify(repository).subscribeToPlan(anyInt(), captor.capture())
        val callback = captor.value

        val body = UserSubscriptionResponse(success = false, data = null)
        invokeResponse(callback, successful = true, body = body)

        assertEquals("Failed to subscribe. Please try again.", viewModel.getError().value)
        assertNull(viewModel.getNewSubscriptionSuccess().value)
    }

    @Test
    fun `subscribeToPlan network failure sets friendly error`() {
        val captor = callbackCaptor<UserSubscriptionResponse>()
        viewModel.subscribeToPlan(plan)

        verify(repository).subscribeToPlan(anyInt(), captor.capture())
        val callback = captor.value

        invokeFailure(callback, RuntimeException("timeout"))

        assertEquals(
            "Request timed out. The server is not responding. Please try again later.",
            viewModel.getError().value
        )
        assertFalse(viewModel.getIsLoading().value!!)
    }

    @Test
    fun `subscribeToPlan error body error field wins over default message`() {
        val captor = callbackCaptor<UserSubscriptionResponse>()
        viewModel.subscribeToPlan(plan)

        verify(repository).subscribeToPlan(anyInt(), captor.capture())
        val callback = captor.value

        val call = mockCall<UserSubscriptionResponse>()
        val response = mockResponse<UserSubscriptionResponse>()
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.errorBody()).thenReturn(errorBodyJson("""{"error":"Insufficient balance"}"""))

        callback.onResponse(call, response)

        assertEquals("Insufficient balance", viewModel.getError().value)
    }

    @Test
    fun `subscribeToPlan error body message field used when no error field`() {
        val captor = callbackCaptor<UserSubscriptionResponse>()
        viewModel.subscribeToPlan(plan)

        verify(repository).subscribeToPlan(anyInt(), captor.capture())
        val callback = captor.value

        val call = mockCall<UserSubscriptionResponse>()
        val response = mockResponse<UserSubscriptionResponse>()
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.errorBody()).thenReturn(errorBodyJson("""{"message":"Plan not available"}"""))

        callback.onResponse(call, response)

        assertEquals("Plan not available", viewModel.getError().value)
    }

    @Test
    fun `subscribeToPlan empty error body falls back to default message`() {
        val captor = callbackCaptor<UserSubscriptionResponse>()
        viewModel.subscribeToPlan(plan)

        verify(repository).subscribeToPlan(anyInt(), captor.capture())
        val callback = captor.value

        val call = mockCall<UserSubscriptionResponse>()
        val response = mockResponse<UserSubscriptionResponse>()
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.errorBody()).thenReturn(errorBodyJson("""{}"""))

        callback.onResponse(call, response)

        assertEquals("Failed to subscribe. Please try again.", viewModel.getError().value)
    }

    @Test
    fun `subscribeToPlan missing error body falls back to default message`() {
        val captor = callbackCaptor<UserSubscriptionResponse>()
        viewModel.subscribeToPlan(plan)

        verify(repository).subscribeToPlan(anyInt(), captor.capture())
        val callback = captor.value

        invokeResponse(callback, successful = false)

        assertEquals("Failed to subscribe. Please try again.", viewModel.getError().value)
    }

    @Test
    fun `subscribeToPlan successful response emits success event`() {
        val captor = callbackCaptor<UserSubscriptionResponse>()
        viewModel.subscribeToPlan(plan)

        verify(repository).subscribeToPlan(anyInt(), captor.capture())
        val callback = captor.value

        val body = UserSubscriptionResponse(success = true, data = null)
        invokeResponse(callback, successful = true, body = body)

        assertEquals("Success event must carry the subscribed plan", plan, viewModel.getNewSubscriptionSuccess().value)
        assertFalse(viewModel.getIsLoading().value!!)
    }

    // ---------------------------------------------------------------------------
    // requestTestAccess — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `requestTestAccess unsuccessful response sets error`() {
        val captor = callbackCaptor<ApiResponse<Void>>()
        viewModel.requestTestAccess(3, 7, 1)

        verify(repository).requestTestAccess(anyInt(), anyInt(), anyInt(), captor.capture())
        val callback = captor.value

        invokeResponse(callback, successful = false)

        assertEquals("Failed to request access.", viewModel.getError().value)
        assertNull("No success event on failure", viewModel.getRequestAccessSuccess().value)
        assertFalse(viewModel.getIsLoading().value!!)
    }

    @Test
    fun `requestTestAccess response body with success false sets error`() {
        val captor = callbackCaptor<ApiResponse<Void>>()
        viewModel.requestTestAccess(3, 7, 1)

        verify(repository).requestTestAccess(anyInt(), anyInt(), anyInt(), captor.capture())
        val callback = captor.value

        invokeResponse(callback, successful = true, body = ApiResponse<Void>(success = false))

        assertEquals("Failed to request access.", viewModel.getError().value)
        assertNull("No success event on failure", viewModel.getRequestAccessSuccess().value)
    }

    @Test
    fun `requestTestAccess network failure sets friendly error`() {
        val captor = callbackCaptor<ApiResponse<Void>>()
        viewModel.requestTestAccess(3, 7, 1)

        verify(repository).requestTestAccess(anyInt(), anyInt(), anyInt(), captor.capture())
        val callback = captor.value

        invokeFailure(callback, RuntimeException("connection refused"))

        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            viewModel.getError().value
        )
        assertFalse(viewModel.getIsLoading().value!!)
    }

    @Test
    fun `requestTestAccess successful response sets success and message`() {
        val captor = callbackCaptor<ApiResponse<Void>>()
        viewModel.requestTestAccess(3, 7, 1)

        verify(repository).requestTestAccess(anyInt(), anyInt(), anyInt(), captor.capture())
        val callback = captor.value

        invokeResponse(
            callback,
            successful = true,
            body = ApiResponse<Void>(success = true, message = "Access granted")
        )

        assertTrue(viewModel.getRequestAccessSuccess().value!!)
        assertEquals("Access granted", viewModel.getRequestAccessMessage().value)
    }

    // ---------------------------------------------------------------------------
    // cancelSubscription — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `cancelSubscription unsuccessful response sets error`() {
        val captor = callbackCaptor<ApiResponse<Void>>()
        viewModel.cancelSubscription()

        verify(repository).cancelSubscription(captor.capture())
        val callback = captor.value

        invokeResponse(callback, successful = false)

        assertEquals("Failed to cancel subscription", viewModel.getError().value)
        assertFalse(viewModel.getIsLoading().value!!)
    }

    @Test
    fun `cancelSubscription network failure sets friendly error`() {
        val captor = callbackCaptor<ApiResponse<Void>>()
        viewModel.cancelSubscription()

        verify(repository).cancelSubscription(captor.capture())
        val callback = captor.value

        invokeFailure(callback, RuntimeException("unable to resolve host"))

        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            viewModel.getError().value
        )
        assertFalse(viewModel.getIsLoading().value!!)
    }

    // ---------------------------------------------------------------------------
    // fetchUserSubscription — error / loading states
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchUserSubscription error resource sets error and keeps subscription null`() {
        val liveData = MutableLiveData<Resource<UserSubscriptionWithPlan>>()
        `when`(repository.getUserSubscription()).thenReturn(liveData)

        viewModel.fetchUserSubscription()
        liveData.setValue(Resource.error("Subscription fetch failed", null))

        assertEquals("Subscription fetch failed", viewModel.getError().value)
        assertFalse(viewModel.getIsLoading().value!!)
        assertNull(viewModel.getUserSubscription().value)
    }

    @Test
    fun `fetchUserSubscription loading resource sets loading true`() {
        val liveData = MutableLiveData<Resource<UserSubscriptionWithPlan>>()
        `when`(repository.getUserSubscription()).thenReturn(liveData)

        viewModel.fetchUserSubscription()
        liveData.setValue(Resource.loading(null))

        assertTrue(viewModel.getIsLoading().value!!)
    }

    // ---------------------------------------------------------------------------
    // fetchSubscriptionPlans — error / loading states
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchSubscriptionPlans error resource sets error`() {
        val liveData = MutableLiveData<Resource<List<DatabaseSubscriptionPlan>>>()
        `when`(repository.getSubscriptionPlans()).thenReturn(liveData)

        viewModel.fetchSubscriptionPlans()
        liveData.setValue(Resource.error("Plans fetch failed", null))

        assertEquals("Plans fetch failed", viewModel.getError().value)
        assertFalse(viewModel.getIsLoading().value!!)
    }

    @Test
    fun `fetchSubscriptionPlans loading resource sets loading true`() {
        val liveData = MutableLiveData<Resource<List<DatabaseSubscriptionPlan>>>()
        `when`(repository.getSubscriptionPlans()).thenReturn(liveData)

        viewModel.fetchSubscriptionPlans()
        liveData.setValue(Resource.loading(null))

        assertTrue(viewModel.getIsLoading().value!!)
    }

    // ---------------------------------------------------------------------------
    // Dialog state resets
    // ---------------------------------------------------------------------------

    @Test
    fun `doneShowingPaymentDialog clears success event`() {
        viewModel.doneShowingPaymentDialog()
        assertNull(viewModel.getNewSubscriptionSuccess().value)
    }

    @Test
    fun `doneShowingRequestAccessDialog clears success and message`() {
        viewModel.doneShowingRequestAccessDialog()
        assertFalse(viewModel.getRequestAccessSuccess().value!!)
        assertNull(viewModel.getRequestAccessMessage().value)
    }
}
