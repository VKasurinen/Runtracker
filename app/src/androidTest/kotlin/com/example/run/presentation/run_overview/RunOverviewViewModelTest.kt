package com.example.run.presentation.run_overview

import com.example.android_test.TestMockEngine
import com.example.core.domain.SessionStorage
import com.example.core.domain.run.RunRepository
import com.example.core.domain.run.SyncRunScheduler
import com.example.run.presentation.run_overview.model.RunUi
import com.example.test.MainCoroutineExtension
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class RunOverviewViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainCoroutineExtension = MainCoroutineExtension()
    }

    private lateinit var viewModel: RunOverviewViewModel
    private lateinit var runRepository: RunRepository
    private lateinit var syncRunScheduler: SyncRunScheduler
    private lateinit var sessionStorage: SessionStorage
    private lateinit var mockEngine: TestMockEngine
    private lateinit var testScope: CoroutineScope
    private lateinit var testScheduler: TestCoroutineScheduler

    @BeforeEach
    fun setUp() {
        sessionStorage = mock(SessionStorage::class.java)
        syncRunScheduler = mock(SyncRunScheduler::class.java)
        runRepository = mock(RunRepository::class.java)

        `when`(runRepository.getRuns()).thenReturn(flowOf(emptyList()))

        val mockEngineConfig = MockEngineConfig().apply {
            requestHandlers.add { request ->
                val relativeUrl = request.url.encodedPath
                if (relativeUrl == "/runs") {
                    respond(
                        content = ByteReadChannel(
                            text = Json.encodeToString(emptyList<RunUi>())
                        ),
                        headers = headers {
                            set("Content-Type", "application/json")
                        }
                    )
                } else {
                    respond(
                        content = byteArrayOf(),
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }

        testScheduler = TestCoroutineScheduler()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        mockEngine = TestMockEngine(
            dispatcher = testDispatcher,
            mockEngineConfig = mockEngineConfig
        )

        testScope = TestScope(testDispatcher)
        viewModel = RunOverviewViewModel(runRepository, syncRunScheduler, testScope, sessionStorage)
    }

    @Test
    fun testInit() = runTest(testScheduler) {
        verify(runRepository).getRuns()
        verify(runRepository).syncPendingRuns()
        verify(runRepository).fetchRuns()
    }

    @Test
    fun testOnLogoutClick() = runTest(testScheduler) {
        viewModel.onAction(RunOverviewAction.OnLogoutClick)
        verify(syncRunScheduler).cancelAllSyncs()
        verify(runRepository).deleteAllRuns()
        verify(runRepository).logout()
        verify(sessionStorage).set(null)
    }

    /*@Test
    fun testOnDeleteRun() = runTest(testScheduler) {
        val runUi = RunUi(id = "testId", distance = 0.0, duration = 0L, timestamp = 0L)
        viewModel.onAction(RunOverviewAction.DeleteRun(runUi))
        verify(runRepository).deleteRun(runUi.id)
    }
    */
}