package au.edu.vu.studentidassignment2.data

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class LoginRequest(val username: String, val password: String)

interface AssignmentApi {
    @POST("{location}/auth")
    suspend fun authenticate(@Path("location") location: String, @Body request: LoginRequest): JsonObject

    @GET("dashboard/{keypass}")
    suspend fun dashboard(@Path("keypass") keypass: String): JsonObject
}
