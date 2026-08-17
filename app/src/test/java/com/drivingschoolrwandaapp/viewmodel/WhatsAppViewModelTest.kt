package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup
import com.drivingschoolrwandaapp.repository.Resource
import com.drivingschoolrwandaapp.repository.WhatsAppRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Unit tests for [WhatsAppViewModel].
 *
 * The ViewModel owns a single [MutableLiveData], enqueues the repository's
 * [Call], and publishes LOADING / SUCCESS / ERROR resources onto it. The
 * repository returns a raw [Call] (no LiveData), so these tests drive the
 * retrofit callback directly to verify the state flow.
 */
class WhatsAppViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: WhatsAppRepository
    private lateinit var viewModel: WhatsAppViewModel

    @Before
    fun setUp() {
        repository = mock(WhatsAppRepository::class.java)
        val application = mock(Application::class.java)
        `when`(application.getString(anyInt())).thenReturn("localized_error")
        viewModel = WhatsAppViewModel(application, repository)
    }

    /**
     * Stubs the repository to return [call] and captures the enqueued callback.
     */
    private fun stubFetch(call: Call<List<WhatsAppGroup>>): Callback<List<WhatsAppGroup>> {
        `when`(repository.getWhatsAppGroups()).thenReturn(call)
        var captured: Callback<List<WhatsAppGroup>>? = null
        doAnswer { invocation ->
            captured = invocation.getArgument<Callback<List<WhatsAppGroup>>>(0)
            null
        }.`when`(call).enqueue(any())
        return object : Callback<List<WhatsAppGroup>> {
            override fun onResponse(call: Call<List<WhatsAppGroup>>, response: Response<List<WhatsAppGroup>>) {
                captured?.onResponse(call, response)
            }

            override fun onFailure(call: Call<List<WhatsAppGroup>>, t: Throwable) {
                captured?.onFailure(call, t)
            }
        }
    }

    private fun successCall(): Call<List<WhatsAppGroup>> {
        val call = mock(Call::class.java) as Call<List<WhatsAppGroup>>
        `when`(call.isCanceled).thenReturn(false)
        return call
    }

    @Test
    fun `getWhatsAppGroups returns the same ViewModel-owned LiveData`() {
        assertSame(
            "The ViewModel must expose one stable LiveData instance",
            viewModel.getWhatsAppGroups(),
            viewModel.getWhatsAppGroups()
        )
    }

    @Test
    fun `fetchWhatsAppGroups enqueues the repository call and publishes success`() {
        val groups = listOf(WhatsAppGroup(id = "1", name = "Driving School A"))
        val call = successCall()
        val callback = stubFetch(call)

        viewModel.fetchWhatsAppGroups()
        verify(repository).getWhatsAppGroups()
        verify(call).enqueue(any())

        callback.onResponse(call, Response.success(groups))

        assertEquals(Resource.Status.SUCCESS, viewModel.getWhatsAppGroups().value?.status)
        assertEquals(groups, viewModel.getWhatsAppGroups().value?.data)
    }

    @Test
    fun `fetchWhatsAppGroups publishes loading before the call enqueues`() {
        val call = successCall()
        stubFetch(call)

        viewModel.fetchWhatsAppGroups()

        assertEquals(Resource.Status.LOADING, viewModel.getWhatsAppGroups().value?.status)
        assertTrue(viewModel.getWhatsAppGroups().value?.data == null)
    }

    @Test
    fun `fetchWhatsAppGroups publishes error on network failure`() {
        val call = successCall()
        val callback = stubFetch(call)

        viewModel.fetchWhatsAppGroups()
        callback.onFailure(call, java.io.IOException("timeout"))

        assertEquals(Resource.Status.ERROR, viewModel.getWhatsAppGroups().value?.status)
        assertNotNull(viewModel.getWhatsAppGroups().value?.message)
    }

    @Test
    fun `fetchWhatsAppGroups publishes error on empty successful body`() {
        val call = successCall()
        val callback = stubFetch(call)

        viewModel.fetchWhatsAppGroups()
        callback.onResponse(call, Response.success(null))

        assertEquals(Resource.Status.ERROR, viewModel.getWhatsAppGroups().value?.status)
    }

    @Test
    fun `fetchWhatsAppGroups ignores a response from a cancelled call`() {
        val call = successCall()
        val callback = stubFetch(call)

        viewModel.fetchWhatsAppGroups()
        `when`(call.isCanceled).thenReturn(true)
        callback.onResponse(call, Response.success(listOf(WhatsAppGroup(id = "1", name = "A"))))

        // A cancelled call must not overwrite the current (loading) state.
        assertEquals(Resource.Status.LOADING, viewModel.getWhatsAppGroups().value?.status)
    }
}
