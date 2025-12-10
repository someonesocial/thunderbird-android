package com.fsck.k9.ui.foldable

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.Logger
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoldableStateObserverTest {

    private lateinit var activity: Activity
    private lateinit var logger: Logger
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var windowInfoTracker: WindowInfoTracker
    private lateinit var windowLayoutInfoFlow: MutableStateFlow<WindowLayoutInfo>
    private lateinit var observer: FoldableStateObserver
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        FoldableStateObserver.resetStateForTesting()
        activity = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        lifecycleOwner = mockk()
        lifecycleRegistry = LifecycleRegistry(lifecycleOwner)

        every { lifecycleOwner.lifecycle } returns lifecycleRegistry

        // Mock WindowInfoTracker
        windowLayoutInfoFlow = MutableStateFlow(createEmptyWindowLayoutInfo())
        windowInfoTracker = mockk()
        every { windowInfoTracker.windowLayoutInfo(activity) } returns windowLayoutInfoFlow

        observer = FoldableStateObserver(activity, logger)
    }

    @Test
    fun `initial state should be UNKNOWN`() {
        assertEquals(FoldableState.UNKNOWN, observer.currentState)
    }

    @Test
    fun `state should be UNKNOWN when no folding feature is present`() = testScope.runTest {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        windowLayoutInfoFlow.value = createEmptyWindowLayoutInfo()
        advanceTimeBy(400) // Debounce + processing time

        assertEquals(FoldableState.UNKNOWN, observer.currentState)
    }

    @Test
    fun `state should be UNFOLDED when FoldingFeature is FLAT`() = testScope.runTest {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val foldingFeature = createFoldingFeature(FoldingFeature.State.FLAT)
        windowLayoutInfoFlow.value = createWindowLayoutInfo(listOf(foldingFeature))
        advanceTimeBy(400)

        assertEquals(FoldableState.UNFOLDED, observer.currentState)
    }

    @Test
    fun `state should be UNFOLDED when FoldingFeature is HALF_OPENED`() = testScope.runTest {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val foldingFeature = createFoldingFeature(FoldingFeature.State.HALF_OPENED)
        windowLayoutInfoFlow.value = createWindowLayoutInfo(listOf(foldingFeature))
        advanceTimeBy(400)

        assertEquals(FoldableState.UNFOLDED, observer.currentState)
    }

    @Test
    fun `debouncing should prevent rapid state changes`() = testScope.runTest {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        // Emit multiple rapid changes
        windowLayoutInfoFlow.value = createWindowLayoutInfo(listOf(createFoldingFeature(FoldingFeature.State.FLAT)))
        advanceTimeBy(100)

        windowLayoutInfoFlow.value = createEmptyWindowLayoutInfo()
        advanceTimeBy(100)

        windowLayoutInfoFlow.value = createWindowLayoutInfo(listOf(createFoldingFeature(FoldingFeature.State.FLAT)))
        advanceTimeBy(100)

        // State should still be UNKNOWN (no change processed yet due to debounce)
        assertEquals(FoldableState.UNKNOWN, observer.currentState)

        // After debounce period, last state should be applied
        advanceTimeBy(200)
        assertEquals(FoldableState.UNFOLDED, observer.currentState)
    }

    @Test
    fun `observer should stop collecting when lifecycle is stopped`() = testScope.runTest {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceTimeBy(100)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        advanceTimeBy(100)

        // Change should not be processed after stop
        val initialState = observer.currentState
        windowLayoutInfoFlow.value = createWindowLayoutInfo(listOf(createFoldingFeature(FoldingFeature.State.FLAT)))
        advanceTimeBy(400)

        assertEquals(initialState, observer.currentState)
    }

    @Test
    fun `observer should log state changes`() = testScope.runTest {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val foldingFeature = createFoldingFeature(FoldingFeature.State.FLAT)
        windowLayoutInfoFlow.value = createWindowLayoutInfo(listOf(foldingFeature))
        advanceTimeBy(400)

        verify { logger.debug(eq("FoldableStateObserver"), any(), any()) }
    }

    // Helper functions to create mock objects
    private fun createEmptyWindowLayoutInfo(): WindowLayoutInfo {
        return mockk {
            every { displayFeatures } returns emptyList()
        }
    }

    private fun createWindowLayoutInfo(features: List<FoldingFeature>): WindowLayoutInfo {
        return mockk {
            every { displayFeatures } returns features
        }
    }

    private fun createFoldingFeature(state: FoldingFeature.State): FoldingFeature {
        return mockk {
            every { this@mockk.state } returns state
        }
    }
}
