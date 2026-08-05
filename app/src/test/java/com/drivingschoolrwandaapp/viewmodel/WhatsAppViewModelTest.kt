package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup
import com.drivingschoolrwandaapp.repository.Resource
import com.drivingschoolrwandaapp.repository.WhatsAppRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Unit tests for [WhatsAppViewModel].
 *
 * The ViewModel exposes the repository's [LiveData] directly (no transformation
 * or caching). These tests verify the passthrough contract — the exact same
 * [LiveData] instance is returned, so all LOADING / SUCCESS / ERROR resources
 * published by [WhatsAppRepository] flow to the UI untouched.
 */
class WhatsAppViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: WhatsAppRepository
    private lateinit var viewModel: WhatsAppViewModel

    @Before
    fun setUp() {
        repository = mock(WhatsAppRepository::class.java)
        viewModel = WhatsAppViewModel(mock(Application::class.java), repository)
    }

    @Test
    fun `getWhatsAppGroups returns the exact repository live data`() {
        val liveData = MutableLiveData<Resource<List<WhatsAppGroup>>>()
        `when`(repository.getWhatsAppGroups()).thenReturn(liveData)

        val result: LiveData<Resource<List<WhatsAppGroup>>> = viewModel.getWhatsAppGroups()

        assertSame(
            "ViewModel must expose the repository LiveData unchanged",
            liveData,
            result
        )
    }

    @Test
    fun `getWhatsAppGroups loads groups from the repository`() {
        val liveData = MutableLiveData<Resource<List<WhatsAppGroup>>>()
        `when`(repository.getWhatsAppGroups()).thenReturn(liveData)

        viewModel.getWhatsAppGroups()

        verify(repository).getWhatsAppGroups()
    }

    @Test
    fun `getWhatsAppGroups exposes error resources from repository`() {
        val liveData = MutableLiveData<Resource<List<WhatsAppGroup>>>()
        `when`(repository.getWhatsAppGroups()).thenReturn(liveData)

        val result = viewModel.getWhatsAppGroups()
        liveData.setValue(Resource.error("Failed to fetch groups", null))

        assertEquals(Resource.Status.ERROR, result.value?.status)
        assertEquals("Failed to fetch groups", result.value?.message)
    }

    @Test
    fun `getWhatsAppGroups exposes success resources from repository`() {
        val liveData = MutableLiveData<Resource<List<WhatsAppGroup>>>()
        `when`(repository.getWhatsAppGroups()).thenReturn(liveData)

        val result = viewModel.getWhatsAppGroups()
        val groups = listOf(WhatsAppGroup(id = "1", name = "Driving School A"))
        liveData.setValue(Resource.success(groups))

        assertEquals(Resource.Status.SUCCESS, result.value?.status)
        assertEquals(groups, result.value?.data)
    }

    @Test
    fun `getWhatsAppGroups exposes loading resource from repository`() {
        val liveData = MutableLiveData<Resource<List<WhatsAppGroup>>>()
        `when`(repository.getWhatsAppGroups()).thenReturn(liveData)

        val result = viewModel.getWhatsAppGroups()
        liveData.setValue(Resource.loading(null))

        assertEquals(Resource.Status.LOADING, result.value?.status)
    }
}
