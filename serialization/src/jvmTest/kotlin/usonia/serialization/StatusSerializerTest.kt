package usonia.serialization

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals


class StatusSerializerTest {
    @Test
    fun deserialize() {
        val json = """
            {
                "code": 420,
                "message": "Test"
            }
        """

        val result = Json.decodeFromString(StatusSerializer, json)

        assertEquals(420, result.code)
        assertEquals("Test", result.message)
    }
}
