package au.edu.vu.studentidassignment2.ui

import au.edu.vu.studentidassignment2.domain.Entity
import au.edu.vu.studentidassignment2.domain.EntityField
import java.util.Locale

/** Prefer a human-readable name over an API identifier, without requiring a fixed schema. */
fun Entity.titleField(): EntityField? {
    val available = summaryFields.filter { !it.value.isNullOrBlank() }
    return available.firstOrNull {
        val key = it.name.lowercase(Locale.ROOT)
        key.endsWith("name") || key.endsWith("title")
    } ?: available.firstOrNull()
}
