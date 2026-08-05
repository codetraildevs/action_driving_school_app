package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.drivingschoolrwandaapp.data.models.LearningMaterial
import com.drivingschoolrwandaapp.data.models.LearningMaterialResponse
import com.drivingschoolrwandaapp.repository.LearningMaterialRepository
import com.google.gson.Gson
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Unit tests for [LearningMaterialViewModel] covering negative / error paths.
 *
 * [LearningMaterialRepository] is mocked and the enqueued Retrofit [Callback]s are
 * captured and driven with mocked [Response]s. [Application] is mocked so the
 * ViewModel's constructor can obtain SharedPreferences and cache directories.
 *
 * Uses [InstantTaskExecutorRule] so [androidx.lifecycle.LiveData.postValue] /
 * setValue dispatch synchronously on the test thread.
 */
class LearningMaterialViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: LearningMaterialRepository
    private lateinit var viewModel: LearningMaterialViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tempDir: java.io.File
    private var logMock: MockedStatic<Log>? = null

    @Before
    fun setUp() {
        try { logMock?.close() } catch (_: Exception) { }
        try { logMock = mockStatic(Log::class.java) } catch (_: Exception) { }

        repository = mock(LearningMaterialRepository::class.java)
        val application = mock(Application::class.java)
        sharedPreferences = mock(SharedPreferences::class.java)
        `when`(application.getSharedPreferences("DownloadPrefs", Context.MODE_PRIVATE))
            .thenReturn(sharedPreferences)
        // No material has ever been downloaded
        `when`(sharedPreferences.getLong(any(), anyLong())).thenReturn(-1L)
        // Unique per-test temp dir so the ViewModel's cache file is guaranteed ABSENT at
        // test start. Without this, a cache file written by a prior test run (or by the
        // success test's async cacheMaterialsAsync) would make the offline-fallback path
        // post cached materials, racing the assertNull(materials) assertions below.
        tempDir = java.nio.file.Files.createTempDirectory("learning-material-vm-test").toFile()
        `when`(application.cacheDir).thenReturn(tempDir)
        `when`(application.filesDir).thenReturn(tempDir)

        viewModel = LearningMaterialViewModel(application, repository)
    }

    @After
    fun tearDown() {
        try {
            // Clean up the per-test cache dir so nothing leaks into later test runs.
            tempDir.deleteRecursively()
        } catch (_: Exception) { }
        try { logMock?.close() } catch (_: Exception) { }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Build a LearningMaterial with a specific id using Gson (no public setter exists). */
    private fun materialWithId(id: Int): LearningMaterial {
        return Gson().fromJson("""{"id":$id,"title":"PDF guide","fileType":"application/pdf"}""", LearningMaterial::class.java)
    }

    /** Build a LearningMaterialResponse with the given materials (no setter exists). */
    private fun responseWithMaterials(vararg materials: LearningMaterial): LearningMaterialResponse {
        val json = materials.joinToString(prefix = "", postfix = "") {
            """{"id":${it.id},"title":"PDF guide","fileType":"application/pdf"}"""
        }
        return Gson().fromJson("""{"materials":[$json]}""", LearningMaterialResponse::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> callbackCaptor(): ArgumentCaptor<Callback<T>> =
        ArgumentCaptor.forClass(Callback::class.java) as ArgumentCaptor<Callback<T>>

    // ---------------------------------------------------------------------------
    // downloadLearningMaterial — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `downloadLearningMaterial null material does nothing`() {
        viewModel.downloadLearningMaterial(null)

        assertNull("No download state should be published for null material", viewModel.getDownloadStatus().value)
    }

    @Test
    fun `downloadLearningMaterial already downloaded material does nothing`() {
        val material = materialWithId(1).apply { isDownloaded = true }

        viewModel.downloadLearningMaterial(material)

        assertNull("Already-downloaded material must not start a new download", viewModel.getDownloadStatus().value)
    }

    @Test
    fun `downloadLearningMaterial network failure posts failure state`() {
        val material = materialWithId(5)
        val call = mock(Call::class.java) as Call<ResponseBody>
        `when`(repository.downloadLearningMaterial(5)).thenReturn(call)

        viewModel.downloadLearningMaterial(material)
        val captor = callbackCaptor<ResponseBody>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value

        callback.onFailure(call, RuntimeException("timeout"))

        val state = viewModel.getDownloadStatus().value
        assertEquals(DownloadState.Status.FAILURE, state!!.status)
        assertEquals(5, state.materialId)
    }

    @Test
    fun `downloadLearningMaterial unsuccessful response posts failure state`() {
        val material = materialWithId(6)
        val call = mock(Call::class.java) as Call<ResponseBody>
        `when`(repository.downloadLearningMaterial(6)).thenReturn(call)

        viewModel.downloadLearningMaterial(material)
        val captor = callbackCaptor<ResponseBody>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mock(Response::class.java) as Response<ResponseBody>
        `when`(response.isSuccessful).thenReturn(false)

        callback.onResponse(call, response)

        assertEquals(DownloadState.Status.FAILURE, viewModel.getDownloadStatus().value!!.status)
    }

    @Test
    fun `downloadLearningMaterial response with null body posts failure state`() {
        val material = materialWithId(7)
        val call = mock(Call::class.java) as Call<ResponseBody>
        `when`(repository.downloadLearningMaterial(7)).thenReturn(call)

        viewModel.downloadLearningMaterial(material)
        val captor = callbackCaptor<ResponseBody>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mock(Response::class.java) as Response<ResponseBody>
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(null)

        callback.onResponse(call, response)

        assertEquals(DownloadState.Status.FAILURE, viewModel.getDownloadStatus().value!!.status)
    }

    // ---------------------------------------------------------------------------
    // fetchLearningMaterials — negative paths
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchLearningMaterials sets loading before request`() {
        val call = mock(Call::class.java) as Call<LearningMaterialResponse>
        `when`(repository.getLearningMaterials(anyInt(), anyInt())).thenReturn(call)

        viewModel.fetchLearningMaterials(1, 20)

        assertTrue(viewModel.getIsLoading().value!!)
    }

    @Test
    fun `fetchLearningMaterials network failure stops loading and does not publish materials`() {
        val call = mock(Call::class.java) as Call<LearningMaterialResponse>
        `when`(repository.getLearningMaterials(anyInt(), anyInt())).thenReturn(call)

        viewModel.fetchLearningMaterials(1, 20)
        val captor = callbackCaptor<LearningMaterialResponse>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value

        callback.onFailure(call, RuntimeException("network is unreachable"))

        assertFalse("Loading must stop after failure", viewModel.getIsLoading().value!!)
        assertNull("No materials should be published on failure", viewModel.getMaterials().value)
    }

    @Test
    fun `fetchLearningMaterials unsuccessful response stops loading without materials`() {
        val call = mock(Call::class.java) as Call<LearningMaterialResponse>
        `when`(repository.getLearningMaterials(anyInt(), anyInt())).thenReturn(call)

        viewModel.fetchLearningMaterials(1, 20)
        val captor = callbackCaptor<LearningMaterialResponse>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mock(Response::class.java) as Response<LearningMaterialResponse>
        `when`(response.isSuccessful).thenReturn(false)

        callback.onResponse(call, response)

        assertFalse(viewModel.getIsLoading().value!!)
        assertNull(viewModel.getMaterials().value)
    }

    @Test
    fun `fetchLearningMaterials response with null body stops loading without materials`() {
        val call = mock(Call::class.java) as Call<LearningMaterialResponse>
        `when`(repository.getLearningMaterials(anyInt(), anyInt())).thenReturn(call)

        viewModel.fetchLearningMaterials(1, 20)
        val captor = callbackCaptor<LearningMaterialResponse>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mock(Response::class.java) as Response<LearningMaterialResponse>
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(null)

        callback.onResponse(call, response)

        assertFalse(viewModel.getIsLoading().value!!)
        assertNull(viewModel.getMaterials().value)
    }

    // ---------------------------------------------------------------------------
    // Success path (contrast to the negative paths above)
    // ---------------------------------------------------------------------------

    @Test
    fun `fetchLearningMaterials successful response publishes materials`() {
        val call = mock(Call::class.java) as Call<LearningMaterialResponse>
        `when`(repository.getLearningMaterials(anyInt(), anyInt())).thenReturn(call)

        val body = responseWithMaterials(materialWithId(1))

        viewModel.fetchLearningMaterials(1, 20)
        val captor = callbackCaptor<LearningMaterialResponse>()
        verify(call).enqueue(captor.capture())
        val callback = captor.value
        val response = mock(Response::class.java) as Response<LearningMaterialResponse>
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body()).thenReturn(body)

        callback.onResponse(call, response)

        assertFalse(viewModel.getIsLoading().value!!)
        assertEquals(1, viewModel.getMaterials().value?.size)
    }
}
