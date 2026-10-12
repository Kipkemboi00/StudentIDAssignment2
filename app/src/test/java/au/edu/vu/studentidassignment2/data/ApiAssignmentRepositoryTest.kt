package au.edu.vu.studentidassignment2.data

import au.edu.vu.studentidassignment2.config.ClassLocation
import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.FailureReason
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiAssignmentRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ApiAssignmentRepository

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(AssignmentApi::class.java)
        repository = ApiAssignmentRepository(api, DashboardParser())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun `all class endpoints post exactly the entered credentials`() = runTest {
        ClassLocation.entries.forEach { location ->
            server.enqueue(MockResponse().setBody("""{"keypass":"test-topic"}"""))
            assertEquals("test-topic", repository.authenticate(location, "12345678", "TestName"))
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/${location.apiPath}/auth", request.path)
            val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals(setOf("username", "password"), body.keySet())
            assertEquals("12345678", body["username"].asString)
            assertEquals("TestName", body["password"].asString)
            assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        }
    }

    @Test fun `dashboard uses returned keypass as an encoded path segment`() = runTest {
        server.enqueue(MockResponse().setBody("""{"entities":[{"title":"One","description":"Full"}],"entityTotal":1}"""))
        val dashboard = repository.loadDashboard("test topic")
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/dashboard/test%20topic", request.path)
        assertEquals(1, dashboard.entities.size)
    }

    @Test fun `authentication rejection maps to invalid credentials`() = runTest {
        listOf(400, 401, 403).forEach { status ->
            server.enqueue(MockResponse().setResponseCode(status))
            assertEquals(FailureReason.INVALID_CREDENTIALS, failure {
                repository.authenticate(ClassLocation.BR, "12345678", "TestName")
            }.reason)
        }
    }

    @Test fun `server failure and expired session remain distinguishable`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals(FailureReason.SERVER, failure { repository.loadDashboard("topic") }.reason)
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(FailureReason.SESSION_EXPIRED, failure { repository.loadDashboard("topic") }.reason)
    }

    @Test fun `missing blank and wrong type keypass are rejected`() = runTest {
        listOf("{}", """{"keypass":" "}""", """{"keypass":42}""", """{"keypass":null}""").forEach {
            server.enqueue(MockResponse().setBody(it))
            assertEquals(FailureReason.UNEXPECTED_RESPONSE, failure {
                repository.authenticate(ClassLocation.BR, "12345678", "TestName")
            }.reason)
        }
    }

    @Test fun `malformed or wrong shaped responses are unexpected responses`() = runTest {
        listOf("not json", "[]", "null", "{}", "{\"entities\":").forEach {
            server.enqueue(MockResponse().setBody(it))
            assertEquals(FailureReason.UNEXPECTED_RESPONSE, failure { repository.loadDashboard("topic") }.reason)
        }
    }

    @Test fun `transport IOException maps to network error`() = runTest {
        val failing = ApiAssignmentRepository(FailingApi(IOException("offline")), DashboardParser())
        assertEquals(FailureReason.NETWORK, failure { failing.loadDashboard("topic") }.reason)
    }

    @Test fun `connection failures distinguish DNS TLS and timeout from rejected credentials`() = runTest {
        val failures = listOf(
            UnknownHostException("unresolved") to FailureReason.DNS,
            SSLHandshakeException("handshake failed") to FailureReason.SECURE_CONNECTION,
            SocketTimeoutException("read timed out") to FailureReason.TIMEOUT,
            InterruptedIOException("call timed out") to FailureReason.TIMEOUT
        )
        failures.forEach { (cause, reason) ->
            val failing = ApiAssignmentRepository(FailingApi(cause), DashboardParser())
            val error = failure { failing.authenticate(ClassLocation.BR, "12345678", "TestName") }
            assertEquals(reason, error.reason)
            assertSame(cause, error.cause)
        }
    }

    @Test fun `coroutine cancellation is propagated without wrapping`() = runTest {
        val cancelled = ApiAssignmentRepository(FailingApi(CancellationException("cancelled")), DashboardParser())
        try {
            cancelled.loadDashboard("topic")
            fail("Cancellation should propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    private suspend fun failure(block: suspend () -> Any): AppException {
        try { block() } catch (error: AppException) { return error }
        throw AssertionError("Expected AppException")
    }

    private class FailingApi(private val error: Exception) : AssignmentApi {
        override suspend fun authenticate(location: String, request: LoginRequest): JsonObject = throw error
        override suspend fun dashboard(keypass: String): JsonObject = throw error
    }
}
