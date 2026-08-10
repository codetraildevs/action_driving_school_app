package com.drivingschoolrwandaapp.repository

import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.drivingschoolrwandaapp.R
import com.drivingschoolrwandaapp.api.ApiService
import com.drivingschoolrwandaapp.database.dao.UserDao
import com.drivingschoolrwandaapp.database.entities.User as DbUser
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager
import com.drivingschoolrwandaapp.models.entities.User as NetworkUser
import com.drivingschoolrwandaapp.models.response.ApiResponse
import com.drivingschoolrwandaapp.models.response.LoginResponse
import com.drivingschoolrwandaapp.utils.PhoneUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Unit tests for [UserRepository] covering the negative / error paths.
 *
 * [ApiService], [UserDao], [TokenManager] and [Context] are mocked. The enqueued
 * Retrofit [Callback]s are captured and driven with mocked [Response]s / [Throwable]s
 * to exercise every error branch: unsuccessful HTTP responses, null bodies, malformed
 * error bodies, and network failures with each message-mapping bucket.
 *
 * Android statics ([Log], [FirebaseCrashlytics], [PhoneUtils]) are mocked with
 * [mockStatic] so the repository runs headless on the JVM.
 */
class UserRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var apiService: ApiService
    private lateinit var userDao: UserDao
    private lateinit var tokenManager: TokenManager
    private lateinit var context: Context
    private lateinit var repository: UserRepository
    private var logMock: MockedStatic<Log>? = null
    private var crashlyticsMock: MockedStatic<FirebaseCrashlytics>? = null
    private var phoneUtilsMock: MockedStatic<PhoneUtils>? = null
    private var textUtilsMock: MockedStatic<TextUtils>? = null

    @Before
    fun setUp() {
        logMock = mockStatic(Log::class.java)
        crashlyticsMock = mockStatic(FirebaseCrashlytics::class.java)
        phoneUtilsMock = mockStatic(PhoneUtils::class.java)
        textUtilsMock = mockStatic(TextUtils::class.java)
        `when`(FirebaseCrashlytics.getInstance()).thenReturn(mock(FirebaseCrashlytics::class.java))
        `when`(PhoneUtils.normalize(any())).thenReturn("+250700000000")
        // Real TextUtils.isEmpty() throws "not mocked" in local unit tests; stub it with
        // real semantics so parseErrorMessage() can decide whether to parse the error body.
        `when`(TextUtils.isEmpty(any())).thenAnswer { invocation ->
            (invocation.getArgument(0) as? CharSequence)?.isEmpty() != false
        }

        apiService = mock(ApiService::class.java)
        userDao = mock(UserDao::class.java)
        tokenManager = mock(TokenManager::class.java)
        context = mock(Context::class.java)
        `when`(context.getApplicationContext()).thenReturn(context)

        // Stub the string resources the repository resolves for user-facing messages.
        `when`(context.getString(R.string.something_went_wrong)).thenReturn("Something went wrong")
        `when`(context.getString(R.string.network_error))
            .thenReturn("Connection failed. Please check your internet connection and try again.")
        `when`(context.getString(R.string.request_timeout))
            .thenReturn("Request timed out. The server is not responding. Please try again later or contact support.")
        `when`(context.getString(R.string.device_not_allowed))
            .thenReturn("You are not allowed to login from this device. Please contact support at +250782877442 or +250722877442.")

        repository = UserRepository(context, apiService, userDao, tokenManager)
    }

    @After
    fun tearDown() {
        repository.shutdown()
        logMock?.close()
        crashlyticsMock?.close()
        phoneUtilsMock?.close()
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
    // login — loading state
    // ---------------------------------------------------------------------------

    @Test
    fun `login sets loading before enqueueing request`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")

        assertEquals(Resource.Status.LOADING, result.value!!.status)
        verify(call).enqueue(any())
    }

    // ---------------------------------------------------------------------------
    // login — error body parsing
    // ---------------------------------------------------------------------------

    @Test
    fun `login unsuccessful response uses message field from error body`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeUnsuccessful(call, errorBodyJson("""{"message":"Invalid credentials"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Invalid credentials", result.value!!.message)
    }

    @Test
    fun `login unsuccessful response falls back to error field when message is absent`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeUnsuccessful(call, errorBodyJson("""{"error":"Server rejected request"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Server rejected request", result.value!!.message)
    }

    @Test
    fun `login unsuccessful response with empty error body falls back to default message`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeUnsuccessful(call, errorBodyJson("""{}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Something went wrong", result.value!!.message)
    }

    @Test
    fun `login unsuccessful response with no error body falls back to default message`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeUnsuccessful(call, errorBody = null)

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Something went wrong", result.value!!.message)
    }

    @Test
    fun `login unsuccessful response with malformed error body falls back to default message`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeUnsuccessful(call, errorBodyJson("this is not valid json"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Something went wrong", result.value!!.message)
    }

    @Test
    fun `login error message mentioning device is replaced with support message`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeUnsuccessful(call, errorBodyJson("""{"message":"This device is not registered"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "You are not allowed to login from this device. Please contact support at +250782877442 or +250722877442.",
            result.value!!.message
        )
    }

    // ---------------------------------------------------------------------------
    // login — body handling
    // ---------------------------------------------------------------------------

    @Test
    fun `login successful response with null body emits error`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        val captor = callbackCaptor<LoginResponse>()
        verify(call).enqueue(captor.capture())
        val response = mockResponse<LoginResponse>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(null)
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.ERROR, result.value!!.status)
    }

    @Test
    fun `login successful response with body emits success`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        val captor = callbackCaptor<LoginResponse>()
        verify(call).enqueue(captor.capture())
        val body = LoginResponse(success = true, accessToken = "token-123")
        val response = mockResponse<LoginResponse>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(body)
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.SUCCESS, result.value!!.status)
        assertEquals(body, result.value!!.data)
    }

    // ---------------------------------------------------------------------------
    // login — network failure message mapping
    // ---------------------------------------------------------------------------

    @Test
    fun `login network timeout maps to request timeout message`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeFailure(call, RuntimeException("timeout"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "Request timed out. The server is not responding. Please try again later or contact support.",
            result.value!!.message
        )
    }

    @Test
    fun `login unable to resolve host maps to network error message`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeFailure(call, RuntimeException("Unable to resolve host api.example.com"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            result.value!!.message
        )
    }

    @Test
    fun `login unknown network failure falls back to default message`() {
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(any())).thenReturn(call)

        val result = repository.login("user", "pass", "dev-1")
        invokeFailure(call, RuntimeException("cryptic low level error"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Something went wrong", result.value!!.message)
    }

    @Test
    fun `login passes normalized phone password deviceId and android clientType in request`() {
        val requestCaptor = ArgumentCaptor.forClass(com.drivingschoolrwandaapp.models.request.LoginRequest::class.java)
        val call = mockCall<LoginResponse>()
        `when`(apiService.login(requestCaptor.capture())).thenReturn(call)

        repository.login("user", "pass", "dev-1")

        val request = requestCaptor.value
        assertEquals("+250700000000", request.identifier)
        assertEquals("pass", request.password)
        assertEquals("dev-1", request.deviceId)
        // The app always identifies itself as the Android client so the backend
        // can allow phone-only (shared) admin login while still requiring the
        // real password from the web console.
        assertEquals("android_app", request.clientType)
    }

    // ---------------------------------------------------------------------------
    // changePassword
    // ---------------------------------------------------------------------------

    @Test
    fun `changePassword unsuccessful response emits error`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.changePassword(any())).thenReturn(call)

        val result = repository.changePassword("old", "new", "new")
        invokeUnsuccessful(call, errorBodyJson("""{"message":"Wrong current password"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Wrong current password", result.value!!.message)
    }

    @Test
    fun `changePassword network timeout emits request timeout message`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.changePassword(any())).thenReturn(call)

        val result = repository.changePassword("old", "new", "new")
        invokeFailure(call, RuntimeException("timed out"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "Request timed out. The server is not responding. Please try again later or contact support.",
            result.value!!.message
        )
    }

    // ---------------------------------------------------------------------------
    // forgotPassword
    // ---------------------------------------------------------------------------

    @Test
    fun `forgotPassword unsuccessful response emits error`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.forgotPassword(any())).thenReturn(call)

        val result = repository.forgotPassword("user@example.com")
        invokeUnsuccessful(call, errorBodyJson("""{"message":"Email not found"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Email not found", result.value!!.message)
    }

    @Test
    fun `forgotPassword network failure maps to network error message`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.forgotPassword(any())).thenReturn(call)

        val result = repository.forgotPassword("user@example.com")
        invokeFailure(call, RuntimeException("network is unreachable"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            result.value!!.message
        )
    }

    // ---------------------------------------------------------------------------
    // verifyOtp
    // ---------------------------------------------------------------------------

    @Test
    fun `verifyOtp unsuccessful response emits error`() {
        val call = mockCall<ApiResponse<String>>()
        `when`(apiService.verifyOtp(any())).thenReturn(call)

        val result = repository.verifyOtp("user@example.com", "123456")
        invokeUnsuccessful(call, errorBodyJson("""{"message":"Invalid OTP"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Invalid OTP", result.value!!.message)
    }

    @Test
    fun `verifyOtp successful response with null body emits error`() {
        val call = mockCall<ApiResponse<String>>()
        `when`(apiService.verifyOtp(any())).thenReturn(call)

        val result = repository.verifyOtp("user@example.com", "123456")
        val captor = callbackCaptor<ApiResponse<String>>()
        verify(call).enqueue(captor.capture())
        val response = mockResponse<ApiResponse<String>>()
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(null)
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.ERROR, result.value!!.status)
    }

    // ---------------------------------------------------------------------------
    // resetPassword
    // ---------------------------------------------------------------------------

    @Test
    fun `resetPassword unsuccessful response emits error`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.resetPassword(any())).thenReturn(call)

        val result = repository.resetPassword("token", "new", "new")
        invokeUnsuccessful(call, errorBodyJson("""{"message":"Token expired"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Token expired", result.value!!.message)
    }

    // ---------------------------------------------------------------------------
    // sleepSubscription
    // ---------------------------------------------------------------------------

    @Test
    fun `sleepSubscription unsuccessful response emits error`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.sleepSubscription(anyInt())).thenReturn(call)

        val result = repository.sleepSubscription(41)
        invokeUnsuccessful(call, errorBodyJson("""{"message":"Not subscribed"}"""))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Not subscribed", result.value!!.message)
    }

    @Test
    fun `sleepSubscription network failure emits error`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.sleepSubscription(anyInt())).thenReturn(call)

        val result = repository.sleepSubscription(48)
        invokeFailure(call, RuntimeException("failed to connect"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals(
            "Connection failed. Please check your internet connection and try again.",
            result.value!!.message
        )
    }

    // ---------------------------------------------------------------------------
    // logout / deleteAccount — local logout always runs
    // ---------------------------------------------------------------------------

    @Test
    fun `logout onResponse clears db and tokens`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.logout()).thenReturn(call)

        repository.logout()
        val captor = callbackCaptor<ApiResponse<Void>>()
        verify(call).enqueue(captor.capture())
        captor.value.onResponse(mockCall(), mockResponse())

        // Local logout is executed on the repository's background executor.
        verify(userDao, timeout(2000)).deleteAll()
        verify(tokenManager, timeout(2000)).clearTokens()
    }

    @Test
    fun `logout onFailure still performs local logout`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.logout()).thenReturn(call)

        repository.logout()
        val captor = callbackCaptor<ApiResponse<Void>>()
        verify(call).enqueue(captor.capture())
        captor.value.onFailure(mockCall(), RuntimeException("network down"))

        verify(userDao, timeout(2000)).deleteAll()
        verify(tokenManager, timeout(2000)).clearTokens()
    }

    @Test
    fun `deleteAccount onResponse clears db and tokens`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.deleteAccount()).thenReturn(call)

        repository.deleteAccount()
        val captor = callbackCaptor<ApiResponse<Void>>()
        verify(call).enqueue(captor.capture())
        captor.value.onResponse(mockCall(), mockResponse())

        verify(userDao, timeout(2000)).deleteAll()
        verify(tokenManager, timeout(2000)).clearTokens()
    }

    @Test
    fun `deleteAccount onFailure still performs local logout`() {
        val call = mockCall<ApiResponse<Void>>()
        `when`(apiService.deleteAccount()).thenReturn(call)

        repository.deleteAccount()
        val captor = callbackCaptor<ApiResponse<Void>>()
        verify(call).enqueue(captor.capture())
        captor.value.onFailure(mockCall(), RuntimeException("boom"))

        verify(userDao, timeout(2000)).deleteAll()
        verify(tokenManager, timeout(2000)).clearTokens()
    }

    // ---------------------------------------------------------------------------
    // updateUser / executor lifecycle
    // ---------------------------------------------------------------------------

    @Test
    fun `updateUser inserts user on background executor`() {
        val user = DbUser(id = 1, firstName = "Alice")
        repository.updateUser(user)

        verify(userDao, timeout(2000)).insert(user)
    }

    @Test
    fun `updateUser after shutdown does not insert`() {
        repository.shutdown()

        repository.updateUser(DbUser(id = 2, firstName = "Bob"))

        verify(userDao, never()).insert(any())
    }

    @Test
    fun `shutdown can be called multiple times safely`() {
        repository.shutdown()
        // Second call must not throw.
        repository.shutdown()
    }

    // ---------------------------------------------------------------------------
    // loadFromDb
    // ---------------------------------------------------------------------------

    @Test
    fun `loadFromDb returns dao live data`() {
        val dbLiveData = MutableLiveData<DbUser>()
        `when`(userDao.getUser()).thenReturn(dbLiveData)

        val result = repository.loadFromDb()

        assertEquals(dbLiveData, result)
    }

    // ---------------------------------------------------------------------------
    // getProfile (NetworkBoundResource) — negative paths
    //
    // NOTE: only the LOADING + error paths are covered. The success path runs
    // saveCallResult on a background executor and then posts to
    // Handler(Looper.getMainLooper()), which is not available in plain JVM unit
    // tests (would need Robolectric), so it is intentionally excluded.
    // ---------------------------------------------------------------------------

    @Test
    fun `getProfile starts loading and fetches after db emits`() {
        val dbSource = MutableLiveData<DbUser>()
        `when`(userDao.getUser()).thenReturn(dbSource)
        val call = mockCall<ApiResponse<NetworkUser>>()
        `when`(apiService.getProfile()).thenReturn(call)

        val result = repository.getProfile()
        result.observeForever { }
        assertEquals(Resource.Status.LOADING, result.value!!.status)

        dbSource.setValue(DbUser(id = 1, firstName = "Alice"))

        // shouldFetch() == true → fetchFromNetwork → createCall().enqueue
        verify(call).enqueue(any())
    }

    @Test
    fun `getProfile unsuccessful response emits error with server message`() {
        val dbSource = MutableLiveData<DbUser>()
        `when`(userDao.getUser()).thenReturn(dbSource)
        val call = mockCall<ApiResponse<NetworkUser>>()
        `when`(apiService.getProfile()).thenReturn(call)

        val result = repository.getProfile()
        result.observeForever { }
        dbSource.setValue(DbUser(id = 1, firstName = "Alice"))

        val captor = callbackCaptor<ApiResponse<NetworkUser>>()
        verify(call).enqueue(captor.capture())
        val response = mockResponse<ApiResponse<NetworkUser>>()
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.message()).thenReturn("Unauthorized")
        captor.value.onResponse(mockCall(), response)

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertEquals("Unauthorized", result.value!!.message)
    }

    @Test
    fun `getProfile network failure emits friendly error`() {
        val dbSource = MutableLiveData<DbUser>()
        `when`(userDao.getUser()).thenReturn(dbSource)
        val call = mockCall<ApiResponse<NetworkUser>>()
        `when`(apiService.getProfile()).thenReturn(call)

        val result = repository.getProfile()
        result.observeForever { }
        dbSource.setValue(DbUser(id = 1, firstName = "Alice"))

        val captor = callbackCaptor<ApiResponse<NetworkUser>>()
        verify(call).enqueue(captor.capture())
        captor.value.onFailure(mockCall(), RuntimeException("timeout"))

        assertEquals(Resource.Status.ERROR, result.value!!.status)
        assertNotNull("Expected a friendly error message", result.value!!.message)
    }

    // ---------------------------------------------------------------------------
    // mapUser — persisted role sync (called by getProfile's success path)
    //
    // The full getProfile success path needs a main looper (Robolectric), so the
    // role-sync behaviour is verified directly through the package-private mapUser.
    // ---------------------------------------------------------------------------

    @Test
    fun `mapUser persists a valid role to the token manager`() {
        val networkUser = NetworkUser(id = 7, firstName = "Alice", role = 2)

        val dbUser = repository.mapUser(networkUser)

        assertEquals(2, dbUser.roleId)
        verify(tokenManager).saveRole(2)
    }

    @Test
    fun `mapUser does not wipe stored role when the response omits it`() {
        val networkUser = NetworkUser(id = 7, firstName = "Alice", role = 0)

        repository.mapUser(networkUser)

        // role 0 is not a known role id — the persisted admin role must survive.
        verify(tokenManager, never()).saveRole(anyInt())
    }

    @Test
    fun `mapUser syncs a student role downgrade`() {
        val networkUser = NetworkUser(id = 7, firstName = "Alice", role = 5)

        repository.mapUser(networkUser)

        verify(tokenManager).saveRole(5)
    }
}
