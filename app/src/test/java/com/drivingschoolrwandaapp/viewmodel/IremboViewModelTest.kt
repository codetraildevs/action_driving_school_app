package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.drivingschoolrwandaapp.api.ApiService
import com.drivingschoolrwandaapp.models.IremboApplication
import com.drivingschoolrwandaapp.models.request.IremboLicenseRequest
import com.drivingschoolrwandaapp.models.request.IremboSpecialRequest
import com.drivingschoolrwandaapp.models.response.ApiResponse
import com.drivingschoolrwandaapp.models.response.IremboPaymentResponse
import com.drivingschoolrwandaapp.repository.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Unit tests for [IremboViewModel].
 *
 * The ViewModel wraps [ApiService] calls in Retrofit [Callback]s. These tests
 * mock [ApiService] and [Call], capture the enqueued callback, and drive it
 * with mocked [Response]s to cover the negative / error paths:
 * unsuccessful HTTP responses, null bodies, and [Callback.onFailure].
 *
 * Uses [InstantTaskExecutorRule] so LiveData [androidx.lifecycle.LiveData.setValue]
 * executes synchronously on the test thread.
 */
class IremboViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var apiService: ApiService
    private lateinit var viewModel: IremboViewModel

    // The ViewModel resolves user-facing errors via Application.getString().
    // Stub it to a fixed value so the assertions verify the localized message
    // path rather than a hardcoded English string.
    private val localizedError = "localized_error"

    private fun setUpViewModel() {
        apiService = mock(ApiService::class.java)
        val application = mock(Application::class.java)
        `when`(application.getString(anyInt())).thenReturn(localizedError)
        viewModel = IremboViewModel(application, apiService)
    }

    private fun sampleApplication(number: String): IremboApplication {
        return IremboApplication(
            title = "Application $number",
            reference = "REF-$number",
            status = "IN_PROGRESS",
            date = "2024-01-01",
            message = "Processing",
            completionPercentage = 50,
            currentStep = "step-2"
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> mockCall(): Call<T> = mock(Call::class.java) as Call<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T> mockResponse(): Response<T> = mock(Response::class.java) as Response<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T> callbackCaptor(): ArgumentCaptor<Callback<T>> =
        ArgumentCaptor.forClass(Callback::class.java) as ArgumentCaptor<Callback<T>>

    // ---------------------------------------------------------------------------
    // fetchRecentApplications — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchRecentApplications sets loading before request`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<List<IremboApplication>>>()
        `when`(apiService.getRecentIremboApplications()).thenReturn(call)

        viewModel.fetchRecentApplications()

        assertEquals(Resource.Status.LOADING, viewModel.getRecentApplications().value?.status)
    }

    @Test
    fun `fetchRecentApplications unsuccessful response emits error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<List<IremboApplication>>>()
        `when`(apiService.getRecentIremboApplications()).thenReturn(call)

        viewModel.fetchRecentApplications()
        val captor = callbackCaptor<ApiResponse<List<IremboApplication>>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<List<IremboApplication>>>()
        `when`(response.isSuccessful).thenReturn(false)

        callback.onResponse(call, response)

        assertEquals(Resource.Status.ERROR, viewModel.getRecentApplications().value?.status)
        assertEquals(localizedError, viewModel.getRecentApplications().value?.message)
    }

    @Test
    fun `fetchRecentApplications successful response with null body emits error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<List<IremboApplication>>>()
        `when`(apiService.getRecentIremboApplications()).thenReturn(call)

        viewModel.fetchRecentApplications()
        val captor = callbackCaptor<ApiResponse<List<IremboApplication>>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<List<IremboApplication>>>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(null)

        callback.onResponse(call, response)

        assertEquals(Resource.Status.ERROR, viewModel.getRecentApplications().value?.status)
    }

    @Test
    fun `fetchRecentApplications network failure emits friendly error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<List<IremboApplication>>>()
        `when`(apiService.getRecentIremboApplications()).thenReturn(call)

        viewModel.fetchRecentApplications()
        val captor = callbackCaptor<ApiResponse<List<IremboApplication>>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value

        callback.onFailure(call, RuntimeException("timeout"))

        assertEquals(Resource.Status.ERROR, viewModel.getRecentApplications().value?.status)
        assertEquals(
            "Request timed out. The server is not responding. Please try again later.",
            viewModel.getRecentApplications().value?.message
        )
    }

    @Test
    fun `fetchRecentApplications response with null data list emits success with null`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<List<IremboApplication>>>()
        `when`(apiService.getRecentIremboApplications()).thenReturn(call)

        viewModel.fetchRecentApplications()
        val captor = callbackCaptor<ApiResponse<List<IremboApplication>>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<List<IremboApplication>>>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(ApiResponse<List<IremboApplication>>(success = true, data = null))

        callback.onResponse(call, response)

        assertEquals(Resource.Status.SUCCESS, viewModel.getRecentApplications().value?.status)
        assertNull(viewModel.getRecentApplications().value?.data)
    }

    // ---------------------------------------------------------------------------
    // submitLicenseRequest — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `submitLicenseRequest unsuccessful response emits error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboPaymentResponse>>()
        `when`(apiService.requestIremboLicense(any())).thenReturn(call)

        viewModel.submitLicenseRequest(IremboLicenseRequest())
        val captor = callbackCaptor<ApiResponse<IremboPaymentResponse>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<IremboPaymentResponse>>()
        `when`(response.isSuccessful).thenReturn(false)

        callback.onResponse(call, response)

        assertEquals(Resource.Status.ERROR, viewModel.getLicenseRequestStatus().value?.status)
        assertEquals(localizedError, viewModel.getLicenseRequestStatus().value?.message)
    }

    @Test
    fun `submitLicenseRequest network failure emits friendly error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboPaymentResponse>>()
        `when`(apiService.requestIremboLicense(any())).thenReturn(call)

        viewModel.submitLicenseRequest(IremboLicenseRequest())
        val captor = callbackCaptor<ApiResponse<IremboPaymentResponse>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value

        callback.onFailure(call, RuntimeException("network is unreachable"))

        assertEquals(Resource.Status.ERROR, viewModel.getLicenseRequestStatus().value?.status)
        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            viewModel.getLicenseRequestStatus().value?.message
        )
    }

    @Test
    fun `submitLicenseRequest response with null data emits success with null`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboPaymentResponse>>()
        `when`(apiService.requestIremboLicense(any())).thenReturn(call)

        viewModel.submitLicenseRequest(IremboLicenseRequest())
        val captor = callbackCaptor<ApiResponse<IremboPaymentResponse>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<IremboPaymentResponse>>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(ApiResponse<IremboPaymentResponse>(success = true, data = null))

        callback.onResponse(call, response)

        assertEquals(Resource.Status.SUCCESS, viewModel.getLicenseRequestStatus().value?.status)
        assertNull(viewModel.getLicenseRequestStatus().value?.data)
    }

    // ---------------------------------------------------------------------------
    // submitSpecialRequest — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `submitSpecialRequest unsuccessful response emits error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboPaymentResponse>>()
        `when`(apiService.requestSpecialIremboService(any())).thenReturn(call)

        viewModel.submitSpecialRequest(IremboSpecialRequest())
        val captor = callbackCaptor<ApiResponse<IremboPaymentResponse>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<IremboPaymentResponse>>()
        `when`(response.isSuccessful).thenReturn(false)

        callback.onResponse(call, response)

        assertEquals(Resource.Status.ERROR, viewModel.getSpecialRequestStatus().value?.status)
        assertEquals(localizedError, viewModel.getSpecialRequestStatus().value?.message)
    }

    @Test
    fun `submitSpecialRequest network failure emits friendly error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboPaymentResponse>>()
        `when`(apiService.requestSpecialIremboService(any())).thenReturn(call)

        viewModel.submitSpecialRequest(IremboSpecialRequest())
        val captor = callbackCaptor<ApiResponse<IremboPaymentResponse>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value

        callback.onFailure(call, RuntimeException("connection refused"))

        assertEquals(Resource.Status.ERROR, viewModel.getSpecialRequestStatus().value?.status)
        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            viewModel.getSpecialRequestStatus().value?.message
        )
    }

    // ---------------------------------------------------------------------------
    // fetchApplicationDetails — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchApplicationDetails unsuccessful response emits error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboApplication>>()
        `when`(apiService.getIremboApplicationByNumber(any())).thenReturn(call)

        viewModel.fetchApplicationDetails("REF-1")
        val captor = callbackCaptor<ApiResponse<IremboApplication>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<IremboApplication>>()
        `when`(response.isSuccessful).thenReturn(false)

        callback.onResponse(call, response)

        assertEquals(Resource.Status.ERROR, viewModel.getApplicationDetails().value?.status)
        assertEquals(localizedError, viewModel.getApplicationDetails().value?.message)
    }

    @Test
    fun `fetchApplicationDetails successful response with null body emits error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboApplication>>()
        `when`(apiService.getIremboApplicationByNumber(any())).thenReturn(call)

        viewModel.fetchApplicationDetails("REF-2")
        val captor = callbackCaptor<ApiResponse<IremboApplication>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<IremboApplication>>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(null)

        callback.onResponse(call, response)

        assertEquals(Resource.Status.ERROR, viewModel.getApplicationDetails().value?.status)
    }

    @Test
    fun `fetchApplicationDetails network failure emits friendly error`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboApplication>>()
        `when`(apiService.getIremboApplicationByNumber(any())).thenReturn(call)

        viewModel.fetchApplicationDetails("REF-3")
        val captor = callbackCaptor<ApiResponse<IremboApplication>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value

        callback.onFailure(call, RuntimeException("socket timeout"))

        assertEquals(Resource.Status.ERROR, viewModel.getApplicationDetails().value?.status)
        assertEquals(
            "Request timed out. The server is not responding. Please try again later.",
            viewModel.getApplicationDetails().value?.message
        )
    }

    @Test
    fun `fetchApplicationDetails response with null data emits success with null`() {
        setUpViewModel()
        val call = mockCall<ApiResponse<IremboApplication>>()
        `when`(apiService.getIremboApplicationByNumber(any())).thenReturn(call)

        viewModel.fetchApplicationDetails("REF-4")
        val captor = callbackCaptor<ApiResponse<IremboApplication>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<IremboApplication>>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(ApiResponse<IremboApplication>(success = true, data = null))

        callback.onResponse(call, response)

        assertEquals(Resource.Status.SUCCESS, viewModel.getApplicationDetails().value?.status)
        assertNull(viewModel.getApplicationDetails().value?.data)
    }

    // ---------------------------------------------------------------------------
    // Success paths (used as contrast to the negative paths above)
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchRecentApplications success with 4 items shows only first 2`() {
        // NOTE: this pins the ViewModel's current subList(0, 2) behavior when more than
        // 3 applications come back — items 3 and 4 are silently dropped. This looks like
        // a possible off-by-one in the source; the test documents the current behavior.

        setUpViewModel()
        val call = mockCall<ApiResponse<List<IremboApplication>>>()
        `when`(apiService.getRecentIremboApplications()).thenReturn(call)

        viewModel.fetchRecentApplications()
        val captor = callbackCaptor<ApiResponse<List<IremboApplication>>>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<ApiResponse<List<IremboApplication>>>()
        val items = listOf(
            sampleApplication("1"),
            sampleApplication("2"),
            sampleApplication("3"),
            sampleApplication("4")
        )
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(ApiResponse<List<IremboApplication>>(success = true, data = items))

        callback.onResponse(call, response)

        assertEquals(Resource.Status.SUCCESS, viewModel.getRecentApplications().value?.status)
        assertEquals(2, viewModel.getRecentApplications().value?.data?.size)
    }
}
