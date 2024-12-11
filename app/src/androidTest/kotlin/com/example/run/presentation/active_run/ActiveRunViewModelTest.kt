package com.example.run.presentation.active_run

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.run.domain.RunningTracker
import com.example.core.domain.run.RunRepository
import com.example.run.domain.LocationObserver
import com.example.test.MainCoroutineExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.mock
import kotlinx.coroutines.flow.first
import org.mockito.Mockito.verify

class ActiveRunViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainCoroutineExtension = MainCoroutineExtension()
    }

    private lateinit var viewModel: ActiveRunViewModel
    private lateinit var runningTracker: RunningTracker
    private lateinit var runRepository: RunRepository

    @BeforeEach
    fun setUp() {
        val applicationScope = CoroutineScope(Dispatchers.Default)
        val locationObserver = mock(LocationObserver::class.java)
        //runningTracker = RunningTracker(locationObserver, applicationScope)
        //runningTracker = mock(RunningTracker::class.java) // Mock RunningTracker
        runRepository = mock(RunRepository::class.java)
        //viewModel = ActiveRunViewModel(runningTracker, runRepository)
    }
    @Test
    fun testStartAndStopTracking() = runTest {
        assertThat(viewModel.state.shouldTrack).isFalse()

        viewModel.onAction(ActiveRunAction.OnToggleRunClick)
        assertThat(viewModel.state.shouldTrack).isTrue()

        viewModel.onAction(ActiveRunAction.OnToggleRunClick)
        assertThat(viewModel.state.shouldTrack).isFalse()
    }
    @Test
    fun testFinishRun() = runTest {
        viewModel.onAction(ActiveRunAction.OnToggleRunClick)
        viewModel.onAction(ActiveRunAction.OnFinishRunClick)

        assertThat(viewModel.state.isRunFinished).isTrue()
        assertThat(viewModel.state.isSavingRun).isTrue()
    }
    @Test
    fun testResumeRun() = runTest {
        viewModel.onAction(ActiveRunAction.OnResumeRunClick)
        assertThat(viewModel.state.shouldTrack).isTrue()
    }

    @Test
    fun testBackClick() = runTest {
        viewModel.onAction(ActiveRunAction.OnBackClick)
        assertThat(viewModel.state.shouldTrack).isFalse()
    }

    @Test
    fun testDismissRationaleDialog() = runTest {
        viewModel.onAction(ActiveRunAction.DismissRationaleDialog)
        assertThat(viewModel.state.showNotificationRationale).isFalse()
        assertThat(viewModel.state.showLocationRationale).isFalse()
    }




}