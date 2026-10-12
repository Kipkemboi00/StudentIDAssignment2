package au.edu.vu.studentidassignment2.data

import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.domain.Entity
import au.edu.vu.studentidassignment2.domain.EntityField
import au.edu.vu.studentidassignment2.domain.FailureReason
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class DashboardParser {
    private val prettyJson = GsonBuilder().setPrettyPrinting().create()

    fun parse(payload: JsonObject): Dashboard {
        val entities = payload.get("entities")
        val total = payload.get("entityTotal")
        if (entities == null || !entities.isJsonArray || total == null ||
            !total.isJsonPrimitive || !total.asJsonPrimitive.isNumber
        ) unexpected()
        val count = try {
            total.asBigDecimal.intValueExact()
        } catch (_: ArithmeticException) {
            unexpected()
        }
        if (count < 0) unexpected()
        val parsed = entities.asJsonArray.map {
            if (!it.isJsonObject) unexpected()
            parseEntity(it.asJsonObject)
        }
        return Dashboard(parsed, count)
    }

    fun parseEntity(payload: JsonObject): Entity = Entity(
        fields = payload.entrySet().map { (name, value) ->
            EntityField(name, when {
                value.isJsonNull -> null
                value.isJsonPrimitive -> value.asString
                else -> prettyJson.toJson(value)
            })
        },
        rawJson = payload.toString()
    )

    fun parseEntity(rawJson: String): Entity {
        val payload = JsonParser.parseString(rawJson)
        if (!payload.isJsonObject) unexpected()
        return parseEntity(payload.asJsonObject)
    }

    private fun unexpected(): Nothing = throw AppException(FailureReason.UNEXPECTED_RESPONSE)
}
