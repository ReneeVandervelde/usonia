package usonia.serialization

import kimchi.logger.LogLevel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogMessageSerializerTest {
    @Test
    fun full() {
        val json = """
            {
                "level": "DEBUG",
                "message": "test-message",
                "stackTrace": "test-stacktrace"
            }
        """
        val result = Json.decodeFromString(LogMessageSerializer, json)

        assertEquals(LogLevel.DEBUG, result.level)
        assertEquals("test-message", result.message)
        assertEquals("test-stacktrace", result.stackTrace)
    }

    @Test
    fun minimal() {
        val json = """
            {
                "level": "DEBUG",
                "message": "test-message"
            }
        """
        val result = Json.decodeFromString(LogMessageSerializer, json)

        assertEquals(LogLevel.DEBUG, result.level)
        assertEquals("test-message", result.message)
        assertNull(result.stackTrace)
    }
}
