package usonia.foundation

import kotlin.test.Test
import kotlin.test.assertNotEquals

class UuidTest {
    @Test
    fun randomUuid() {
        val first = createUuid()
        val second = createUuid()

        assertNotEquals(first, second, "UUID's are unique")
    }
}
