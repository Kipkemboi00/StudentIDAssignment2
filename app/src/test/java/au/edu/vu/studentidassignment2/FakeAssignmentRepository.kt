package au.edu.vu.studentidassignment2

import au.edu.vu.studentidassignment2.config.ClassLocation
import au.edu.vu.studentidassignment2.data.AssignmentRepository
import au.edu.vu.studentidassignment2.data.DashboardParser
import au.edu.vu.studentidassignment2.domain.Dashboard
import com.google.gson.JsonParser
import kotlinx.coroutines.CompletableDeferred

class FakeAssignmentRepository : AssignmentRepository {
    var authenticationGate: CompletableDeferred<String>? = null
    var dashboardGate: CompletableDeferred<Dashboard>? = null
    var authError: Exception? = null
    var dashboardError: Exception? = null
    var dashboard = sampleDashboard()
    var authCalls = 0
    var dashboardCalls = 0
    var submittedLocation: ClassLocation? = null
    var submittedUsername: String? = null
    var submittedPassword: String? = null
    var requestedKeypass: String? = null

    override suspend fun authenticate(location: ClassLocation, username: String, password: String): String {
        authCalls++
        submittedLocation = location
        submittedUsername = username
        submittedPassword = password
        authError?.let { throw it }
        return authenticationGate?.await() ?: "test-topic"
    }

    override suspend fun loadDashboard(keypass: String): Dashboard {
        dashboardCalls++
        requestedKeypass = keypass
        dashboardError?.let { throw it }
        return dashboardGate?.await() ?: dashboard
    }
}

fun sampleDashboard(): Dashboard = DashboardParser().parse(JsonParser.parseString(
    """{"entities":[{"name":"Sample item","year":2024,"description":"Full sample description"}],"entityTotal":1}"""
).asJsonObject)
