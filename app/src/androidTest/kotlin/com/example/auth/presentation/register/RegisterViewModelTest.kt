package com.example.auth.presentation.register

import androidx.compose.foundation.ExperimentalFoundationApi
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.example.android_test.SessionStorageFake
import com.example.android_test.TestMockEngine
import com.example.auth.data.AuthRepositoryImpl
import com.example.auth.data.EmailPatternValidator
import com.example.auth.data.RegisterRequest
import com.example.auth.domain.UserDataValidator
import com.example.core.data.networking.HttpClientFactory
import com.example.test.MainCoroutineExtension
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class RegisterViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainCoroutineExtension = MainCoroutineExtension()
    }

    private lateinit var viewModel: RegisterViewModel
    private lateinit var repository: AuthRepositoryImpl
    private lateinit var sessionStorageFake: SessionStorageFake
    private lateinit var mockEngine: TestMockEngine

    @BeforeEach
    fun setUp() {
        sessionStorageFake = SessionStorageFake()

        val mockEngineConfig = MockEngineConfig().apply {
            requestHandlers.add { request ->
                val relativeUrl = request.url.encodedPath
                if (relativeUrl == "/register") {
                    respond(
                        content = ByteReadChannel(
                            text = Json.encodeToString(Unit)
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
        mockEngine = TestMockEngine(
            dispatcher = mainCoroutineExtension.testDispatcher,
            mockEngineConfig = mockEngineConfig
        )

        val httpClient = HttpClientFactory(
            sessionStorage = sessionStorageFake
        ).build(mockEngine)
        repository = AuthRepositoryImpl(
            httpClient = httpClient,
            sessionStorage = sessionStorageFake
        )

        viewModel = RegisterViewModel(
            userDataValidator = UserDataValidator(
                patternValidator = EmailPatternValidator
            ),
            repository = repository
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testRegister() = runTest {
        assertThat(viewModel.state.canRegister).isFalse()

        viewModel.state.email.edit {
            append("test@test.com")
        }
        viewModel.state.password.edit {
            append("Test12345")
        }

        viewModel.onAction(RegisterAction.OnRegisterClick)

        assertThat(viewModel.state.isRegistering).isFalse()
        assertThat(viewModel.state.email.text.toString()).isEqualTo("test@test.com")
        assertThat(viewModel.state.password.text.toString()).isEqualTo("Test12345")

        val registerRequest = mockEngine.mockEngine.requestHistory.find {
            it.url.encodedPath == "/register"
        }
        assertThat(registerRequest).isNotNull()
        assertThat(registerRequest!!.headers.contains("x-api-key")).isTrue()

        val registerBody = Json.decodeFromString<RegisterRequest>(
            registerRequest.body.toByteArray().decodeToString()
        )
        assertThat(registerBody.email).isEqualTo("test@test.com")
        assertThat(registerBody.password).isEqualTo("Test12345")
    }
}