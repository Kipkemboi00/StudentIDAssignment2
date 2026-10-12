package au.edu.vu.studentidassignment2.data

import au.edu.vu.studentidassignment2.config.ClassLocation
import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.domain.FailureReason
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import java.io.EOFException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

interface AssignmentRepository {
    suspend fun authenticate(location: ClassLocation, username: String, password: String): String
    suspend fun loadDashboard(keypass: String): Dashboard
}

class ApiAssignmentRepository(
    private val api: AssignmentApi,
    private val parser: DashboardParser
) : AssignmentRepository {
    override suspend fun authenticate(location: ClassLocation, username: String, password: String): String =
        request(authenticating = true) {
            val keypass = api.authenticate(location.apiPath, LoginRequest(username, password)).get("keypass")
            if (keypass == null || !keypass.isJsonPrimitive || !keypass.asJsonPrimitive.isString ||
                keypass.asString.isBlank()
            ) throw AppException(FailureReason.UNEXPECTED_RESPONSE)
            keypass.asString
        }

    override suspend fun loadDashboard(keypass: String): Dashboard = request {
        parser.parse(api.dashboard(keypass))
    }

    private suspend fun <T> request(authenticating: Boolean = false, block: suspend () -> T): T {
        try {
            return block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: AppException) {
            throw error
        } catch (error: HttpException) {
            val reason = when {
                authenticating && error.code() in listOf(400, 401, 403) -> FailureReason.INVALID_CREDENTIALS
                !authenticating && error.code() in listOf(401, 403) -> FailureReason.SESSION_EXPIRED
                error.code() >= 500 -> FailureReason.SERVER
                else -> FailureReason.UNEXPECTED_RESPONSE
            }
            throw AppException(reason, error)
        } catch (error: JsonParseException) {
            throw AppException(FailureReason.UNEXPECTED_RESPONSE, error)
        } catch (error: MalformedJsonException) {
            throw AppException(FailureReason.UNEXPECTED_RESPONSE, error)
        } catch (error: EOFException) {
            throw AppException(FailureReason.UNEXPECTED_RESPONSE, error)
        } catch (error: IOException) {
            throw AppException(FailureReason.NETWORK, error)
        } catch (error: Exception) {
            throw AppException(FailureReason.UNEXPECTED_RESPONSE, error)
        }
    }
}
