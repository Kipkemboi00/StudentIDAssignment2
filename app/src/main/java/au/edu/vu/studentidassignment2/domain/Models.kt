package au.edu.vu.studentidassignment2.domain

data class EntityField(val name: String, val value: String?)

/** Fields keep API order and support heterogeneous objects, nested JSON and null values. */
data class Entity(val fields: List<EntityField>, val rawJson: String) {
    val summaryFields: List<EntityField>
        get() = fields.filterNot { it.name.equals("description", ignoreCase = true) }
}

data class Dashboard(val entities: List<Entity>, val entityTotal: Int)

enum class FailureReason {
    INVALID_REQUEST, INVALID_CREDENTIALS, NETWORK, TIMEOUT, DNS, SECURE_CONNECTION, SERVER, UNEXPECTED_RESPONSE, SESSION_EXPIRED
}

class AppException(
    val reason: FailureReason,
    cause: Throwable? = null,
    val httpStatus: Int? = null
) : Exception(reason.name, cause)
