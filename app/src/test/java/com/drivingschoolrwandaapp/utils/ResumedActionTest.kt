package com.drivingschoolrwandaapp.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ResumedActionTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private class Screen : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test fun `ad dismissal waits for screen resume and navigates only once`() {
        val screen = Screen()
        screen.registry.currentState = Lifecycle.State.CREATED
        var opened = 0
        ResumedAction.run(screen) { opened++ }
        assertEquals(0, opened)
        screen.registry.currentState = Lifecycle.State.STARTED
        assertEquals(0, opened)
        screen.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(1, opened)
        screen.registry.currentState = Lifecycle.State.STARTED
        screen.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(1, opened)
    }

    @Test fun `destroyed screen never navigates`() {
        val screen = Screen()
        screen.registry.currentState = Lifecycle.State.CREATED
        var opened = 0
        ResumedAction.run(screen) { opened++ }
        screen.registry.currentState = Lifecycle.State.DESTROYED
        ResumedAction.run(screen) { opened++ }
        assertEquals(0, opened)
    }

    @Test fun `already resumed screen opens immediately`() {
        val screen = Screen()
        screen.registry.currentState = Lifecycle.State.RESUMED
        var opened = 0
        ResumedAction.run(screen) { opened++ }
        assertEquals(1, opened)
    }
}
