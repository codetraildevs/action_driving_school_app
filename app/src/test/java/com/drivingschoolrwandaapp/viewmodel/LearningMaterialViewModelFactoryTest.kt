package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.drivingschoolrwandaapp.repository.LearningMaterialRepository
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

/**
 * Unit tests for [LearningMaterialViewModelFactory].
 *
 * Verifies the factory creates a [LearningMaterialViewModel] wired to the
 * injected [Application] / [LearningMaterialRepository], and that an unknown
 * ViewModel class is rejected with [IllegalArgumentException].
 */
@Suppress("DEPRECATION") // LearningMaterialViewModelFactory is deprecated but still tested
class LearningMaterialViewModelFactoryTest {

    private lateinit var application: Application
    private lateinit var repository: LearningMaterialRepository
    private lateinit var factory: LearningMaterialViewModelFactory

    @Before
    fun setUp() {
        application = mock(Application::class.java)
        repository = mock(LearningMaterialRepository::class.java)
        `when`(application.getSharedPreferences("DownloadPrefs", Context.MODE_PRIVATE))
            .thenReturn(mock(SharedPreferences::class.java))
        factory = LearningMaterialViewModelFactory(application, repository)
    }

    @Test
    fun `create returns a LearningMaterialViewModel for the expected class`() {
        val viewModel = factory.create(LearningMaterialViewModel::class.java)

        assertTrue(
            "Factory must return a LearningMaterialViewModel instance",
            viewModel is LearningMaterialViewModel
        )
    }

    @Test
    fun `create throws for an unknown ViewModel class`() {
        try {
            factory.create(PdfViewModel::class.java)
            org.junit.Assert.fail("Expected IllegalArgumentException for unknown ViewModel class")
        } catch (e: IllegalArgumentException) {
            assertTrue("Unknown ViewModel class", e.message!!.contains("Unknown ViewModel class"))
        }
    }
}
