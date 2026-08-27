package com.drivingschoolrwandaapp.repository

import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.drivingschoolrwandaapp.R
import com.drivingschoolrwandaapp.api.AdminApiService
import com.drivingschoolrwandaapp.models.entities.AdminDashboardResponse
import com.drivingschoolrwandaapp.models.entities.AdminDashboardStats
import com.drivingschoolrwandaapp.models.entities.AdminRequest
import com.drivingschoolrwandaapp.models.entities.AdminUser
import com.drivingschoolrwandaapp.models.entities.AdminUserDetail
import com.drivingschoolrwandaapp.models.entities.AdminUserDetailResponse
import com.drivingschoolrwandaapp.models.entities.AdminUsersResponse
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Unit tests for [AdminRepository].
 *
 * [AdminApiService] and [Context] are mocked. The enqueued Retrofit
 * [Callback]s are captured and driven with mocked [Response]s / [Throwable]s
 * to exercise the success, error-body, and network-failure branches for the
 * dashboard, users, and requests endpoints.
 *
 * Android statics ([Log], [TextUtils]) are mocked with [mockStatic] so the
 * repository runs headless on the JVM (same approach as [UserRepositoryTest]).
 */
class AdminRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var apiService: AdminApiService
    private lateinit var context: Context
    private lateinit var repository: AdminRepository
    private var logMock: MockedStatic<Log>? = null
    private var textUtilsMock: MockedStatic<TextUtils>? = null

    @Before
    fun setUp() {
        logMock = mockStatic(Log::class.java)
        textUtilsMock = mockStatic(TextUtils::class.java)
        // Real TextUtils.isEmpty() throws "not mocked" in local unit tests; stub it with
        // real semantics so parseErrorMessage() can decide whether to parse the error body.
        `when`(TextUtils.isEmpty(any())).thenAnswer { invocation ->
            (invocation.getArgument(0) as? CharSequence)?.isEmpty() != false
        }

        apiService = mock(AdminApiService::class.java)
        context = mock(Context::class.java)
        `when`(context.getApplicationContext()).thenReturn(context)

        `when`(context.getString(R.string.something_went_wrong)).thenReturn("Something went wrong")
        `when`(context.getString(R.string.network_error))
            .thenReturn("Connection failed. Please check your internet connection and try again.")

        repository = AdminRepository(context, apiService)
    }

    @After
    fun tearDown() {
        logMock?.close()
        textUtilsMock?.close()
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

    private fun errorBodyJson(json: String): ResponseBody {
        // Build a REAL ResponseBody: string() is final in OkHttp and cannot be mock-stubbed.
        return json.toResponseBody(null)
    }

    /** Drive the enqueued callback of [call] with an unsuccessful HTTP response. */
    private fun <T> invokeUnsuccessful(call: Call<T>, errorBody: ResponseBody? = null): Callback<T> {
        val captor = callbackCaptor<T>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mockResponse<T>()
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.errorBody()).thenReturn(errorBody)
        callback.onResponse(mockCall(), response)
        return callback
    }

    private fun <T> invokeFailure(call: Call<T>, throwable: Throwable): Callback<T> {
        val captor = callbackCaptor<T>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        callback.onFailure(mockCall(), throwable)
        return callback
    }

    // ---------------------------------------------------------------------------
    // fetchDashboardStats
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchDashboardStats sets loading before enqueueing request`() {
        val call = mockCall<AdminDashboardResponse>()
        `when`(apiService.getDashboardStats()).thenReturn(call)

        val result = repository.fetchDashboardStats()

        assertEquals(Resource.Status.LOADING, result.value!!.status)
        verify(call).enqueue(any())
    }

    @Test
    fun `fetchDashboardStats successful response with data emits success`() {
        val call = mockCall<AdminDashboardResponse>()
        `when`(apiService.getDashboardStats()).thenReturn(call)

        val result = repository.fetchDashboardStats()
        val captor = callbackCaptor<AdminDashboardResponse>()
        verify(call).enqueue(captor.capture())
        val stats = AdminDashboardStats(totalUsers = 10, activeUsers = 4)
        val response = mockResponse<AdminDashboardResponse>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(AdminDashboardResponse(data = stats))
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.SUCCESS, result.value!!.status)
        assertEquals(stats, result.value!!.data)
    }

    @Test
    fun `fetchDashboardStats successful response with null data emits error`() {
        val call = mockCall<AdminDashboardResponse>()
        `when`(apiService.getDashboardStats()).thenReturn(call)

        val result = repository.fetchDashboardStats()
        val captor = callbackCaptor<AdminDashboardResponse>()
        verify(call).enqueue(captor.capture())
        val response = mockResponse<AdminDashboardResponse>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(AdminDashboardResponse(data = null))
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.ERROR, result.value!!.status)
    }

    @Test
    fun `fetchDashboardStats unsuccessful response uses message from error body`() {
        val call = mockCall<AdminDashboardResponse>()
        `when`(apiService.getDashboardStats()).thenReturn(call)

        val result = repository.fetchDashboardStats()
        invokeUnsuccessful(call, errorBodyJson("""{"message":"Not authorized"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Not authorized", result.value!!.message)
    }

    @Test
    fun `fetchDashboardStats network failure emits friendly error`() {
        val call = mockCall<AdminDashboardResponse>()
        `when`(apiService.getDashboardStats()).thenReturn(call)

        val result = repository.fetchDashboardStats()
        invokeFailure(call, RuntimeException("timeout"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            result.value!!.message
        )
    }

    // ---------------------------------------------------------------------------
    // fetchUsers
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchUsers successful response emits success with users`() {
        val call = mockCall<AdminUsersResponse>()
        `when`(apiService.getUsers()).thenReturn(call)

        val result = repository.fetchUsers()
        val captor = callbackCaptor<AdminUsersResponse>()
        verify(call).enqueue(captor.capture())
        val users = listOf(AdminUser(id = 1, firstName = "Alice"))
        val response = mockResponse<AdminUsersResponse>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(AdminUsersResponse(success = true, data = users))
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.SUCCESS, result.value!!.status)
        assertEquals(users, result.value!!.data)
    }

    @Test
    fun `fetchUsers successful response with null data emits error`() {
        val call = mockCall<AdminUsersResponse>()
        `when`(apiService.getUsers()).thenReturn(call)

        val result = repository.fetchUsers()
        val captor = callbackCaptor<AdminUsersResponse>()
        verify(call).enqueue(captor.capture())
        val response = mockResponse<AdminUsersResponse>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(AdminUsersResponse(success = true, data = null))
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.ERROR, result.value!!.status)
    }

    @Test
    fun `fetchUsers unsuccessful response uses error field when message absent`() {
        val call = mockCall<AdminUsersResponse>()
        `when`(apiService.getUsers()).thenReturn(call)

        val result = repository.fetchUsers()
        invokeUnsuccessful(call, errorBodyJson("""{"error":"Forbidden"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Forbidden", result.value!!.message)
    }

    @Test
    fun `fetchUsers unsuccessful response with no error body falls back to default message`() {
        val call = mockCall<AdminUsersResponse>()
        `when`(apiService.getUsers()).thenReturn(call)

        val result = repository.fetchUsers()
        invokeUnsuccessful(call, errorBody = null)

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Something went wrong", result.value!!.message)
    }

    @Test
    fun `fetchUsers network failure emits friendly error`() {
        val call = mockCall<AdminUsersResponse>()
        `when`(apiService.getUsers()).thenReturn(call)

        val result = repository.fetchUsers()
        invokeFailure(call, RuntimeException("Unable to resolve host api.example.com"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            result.value!!.message
        )
    }

    // ---------------------------------------------------------------------------
    // fetchRequests
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchRequests successful response emits success with bare list`() {
        val call = mockCall<List<AdminRequest>>()
        `when`(apiService.getRequests()).thenReturn(call)

        val result = repository.fetchRequests()
        val captor = callbackCaptor<List<AdminRequest>>()
        verify(call).enqueue(captor.capture())
        val requests = listOf(AdminRequest(id = 9, type = "DRIVING_LICENSE"))
        val response = mockResponse<List<AdminRequest>>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(requests)
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.SUCCESS, result.value!!.status)
        assertEquals(requests, result.value!!.data)
    }

    @Test
    fun `fetchRequests unsuccessful response uses message from error body`() {
        val call = mockCall<List<AdminRequest>>()
        `when`(apiService.getRequests()).thenReturn(call)

        val result = repository.fetchRequests()
        invokeUnsuccessful(call, errorBodyJson("""{"message":"Failed to load requests"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Failed to load requests", result.value!!.message)
    }

    @Test
    fun `fetchRequests network failure emits friendly error`() {
        val call = mockCall<List<AdminRequest>>()
        `when`(apiService.getRequests()).thenReturn(call)

        val result = repository.fetchRequests()
        invokeFailure(call, RuntimeException("boom"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertNotNull("Expected a friendly error message", result.value!!.message)
    }

    // ---------------------------------------------------------------------------
    // fetchUserDetail
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchUserDetail successful response emits success with detail`() {
        val call = mockCall<AdminUserDetailResponse>()
        `when`(apiService.getUserDetail(7)).thenReturn(call)

        val result = repository.fetchUserDetail(7)
        val captor = callbackCaptor<AdminUserDetailResponse>()
        verify(call).enqueue(captor.capture())
        val detail = AdminUserDetail(id = 7, firstName = "Alice", phoneNumber = "+250700000001")
        val response = mockResponse<AdminUserDetailResponse>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(AdminUserDetailResponse(success = true, data = detail))
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.SUCCESS, result.value!!.status)
        assertEquals(detail, result.value!!.data)
    }

    @Test
    fun `fetchUserDetail successful response with null data emits error`() {
        val call = mockCall<AdminUserDetailResponse>()
        `when`(apiService.getUserDetail(7)).thenReturn(call)

        val result = repository.fetchUserDetail(7)
        val captor = callbackCaptor<AdminUserDetailResponse>()
        verify(call).enqueue(captor.capture())
        val response = mockResponse<AdminUserDetailResponse>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(AdminUserDetailResponse(success = true, data = null))
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.ERROR, result.value!!.status)
    }

    @Test
    fun `fetchUserDetail network failure emits friendly error`() {
        val call = mockCall<AdminUserDetailResponse>()
        `when`(apiService.getUserDetail(7)).thenReturn(call)

        val result = repository.fetchUserDetail(7)
        invokeFailure(call, RuntimeException("Unable to resolve host api.example.com"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            result.value!!.message
        )
    }

    // ---------------------------------------------------------------------------
    // fetchUserRequests
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchUserRequests successful response emits success with bare list`() {
        val call = mockCall<List<AdminRequest>>()
        `when`(apiService.getUserRequests(3)).thenReturn(call)

        val result = repository.fetchUserRequests(3)
        val captor = callbackCaptor<List<AdminRequest>>()
        verify(call).enqueue(captor.capture())
        val requests = listOf(AdminRequest(id = 9, type = "DRIVING_LICENSE", status = "PENDING"))
        val response = mockResponse<List<AdminRequest>>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(requests)
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.SUCCESS, result.value!!.status)
        assertEquals(requests, result.value!!.data)
    }

    @Test
    fun `fetchUserRequests unsuccessful response uses message from error body`() {
        val call = mockCall<List<AdminRequest>>()
        `when`(apiService.getUserRequests(3)).thenReturn(call)

        val result = repository.fetchUserRequests(3)
        invokeUnsuccessful(call, errorBodyJson("""{"error":"Forbidden"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Forbidden", result.value!!.message)
    }

    @Test
    fun `fetchUserRequests network failure emits friendly error`() {
        val call = mockCall<List<AdminRequest>>()
        `when`(apiService.getUserRequests(3)).thenReturn(call)

        val result = repository.fetchUserRequests(3)
        invokeFailure(call, RuntimeException("timeout"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertNotNull("Expected a friendly error message", result.value!!.message)
    }
}
