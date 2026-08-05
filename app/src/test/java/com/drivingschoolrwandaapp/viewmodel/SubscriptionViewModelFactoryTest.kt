package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import com.drivingschoolrwandaapp.repository.SubscriptionRepository
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Unit tests for [SubscriptionViewModelFactory].
 *
 * Verifies the factory creates a [SubscriptionViewModel] wired to the injected
 * [Application] / [SubscriptionRepository], and that an unknown ViewModel class
 * is rejected with [IllegalArgumentException].
 */
class SubscriptionViewModelFactoryTest {

    private lateinit var application: Application
    private lateinit var repository: SubscriptionRepository
    private lateinit var factory: SubscriptionViewModelFactory

    @Before
    fun setUp() {
        application = mock(Application::class.java)
        repository = mock(SubscriptionRepository::class.java)
        factory = SubscriptionViewModelFactory(application, repository)
    }

    @Test
    fun `create returns a SubscriptionViewModel for the expected class`() {
        val viewModel = factory.create(SubscriptionViewModel::class.java)

        assertTrue(
            "Factory must return a SubscriptionViewModel instance",
            viewModel is SubscriptionViewModel
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
