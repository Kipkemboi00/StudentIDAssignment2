package au.edu.vu.studentidassignment2.data

import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.FailureReason
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class DashboardParserTest {
    private val parser = DashboardParser()

    @Test fun `heterogeneous entities keep all their own fields`() {
        val dashboard = parse("""{"entities":[{"title":"One","description":"Full text"},{"scientist":"Two","year":1999}],"entityTotal":2}""")
        assertEquals(listOf("title", "description"), dashboard.entities[0].fields.map { it.name })
        assertEquals(listOf("scientist", "year"), dashboard.entities[1].fields.map { it.name })
        assertEquals("1999", dashboard.entities[1].fields[1].value)
        assertEquals(2, dashboard.entityTotal)
    }

    @Test fun `description is available in details but never in summary`() {
        val entity = parser.parseEntity("""{"name":"One","Description":"Secret description","year":2024}""")
        assertEquals(listOf("name", "year"), entity.summaryFields.map { it.name })
        assertTrue(entity.fields.any { it.value == "Secret description" })
    }

    @Test fun `null blank nested and boolean fields remain readable`() {
        val entity = parser.parseEntity("""{"a":null,"b":"","c":{"unit":"m"},"d":[1,2],"e":true}""")
        assertNull(entity.fields[0].value)
        assertEquals("", entity.fields[1].value)
        assertTrue(entity.fields[2].value!!.contains("\"unit\": \"m\""))
        assertTrue(entity.fields[3].value!!.contains("1"))
        assertEquals("true", entity.fields[4].value)
    }

    @Test fun `detail navigation round trip preserves selected data`() {
        val entity = parser.parseEntity("""{"name":"A","description":"Details","extra":null}""")
        assertEquals(entity, parser.parseEntity(entity.rawJson))
    }

    @Test fun `description only and empty objects are valid`() {
        assertTrue(parser.parseEntity("""{"description":"Details"}""").summaryFields.isEmpty())
        assertTrue(parser.parseEntity("{}").fields.isEmpty())
    }

    @Test fun `empty collection is valid`() {
        val dashboard = parse("""{"entities":[],"entityTotal":0}""")
        assertTrue(dashboard.entities.isEmpty())
        assertEquals(0, dashboard.entityTotal)
    }

    @Test fun `invalid schema is rejected instead of looking empty`() {
        listOf(
            "{}", """{"entities":null,"entityTotal":0}""",
            """{"entities":{},"entityTotal":0}""",
            """{"entities":[],"entityTotal":"2"}""",
            """{"entities":[],"entityTotal":-1}""",
            """{"entities":[],"entityTotal":1.5}""",
            """{"entities":[],"entityTotal":99999999999999999999}""",
            """{"entities":[null],"entityTotal":1}""",
            """{"entities":["bad"],"entityTotal":1}"""
        ).forEach { payload ->
            val error = assertThrows(AppException::class.java) { parse(payload) }
            assertEquals(FailureReason.UNEXPECTED_RESPONSE, error.reason)
        }
    }

    private fun parse(payload: String) = parser.parse(JsonParser.parseString(payload).asJsonObject)
}
