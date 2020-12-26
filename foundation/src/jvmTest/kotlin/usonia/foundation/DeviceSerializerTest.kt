package usonia.foundation

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceSerializerTest {
    @Test
    fun customCapabilities() {
        val json = """
            {
              "id": "fake-device-id",
              "name": "Fake Device",
              "actionTypes": ["Switch"],
              "eventTypes": ["Motion"],
              "fixture": "Light",
              "siblings": [
                "fake-sibling-id"
              ],
              "parentContext": "test-parent-context",
              "parentId": "test-parent-id"
            }
        """
        val result = Json.decodeFromString(DeviceSerializer(emptySet()), json)

        assertEquals("fake-device-id", result.id.value)
        assertEquals("Fake Device", result.name)
        assertNull(result.capabilities.archetypeId)
        assertEquals(setOf(Action.Switch::class), result.capabilities.actions)
        assertEquals(setOf(Event.Motion::class), result.capabilities.events)
        assertEquals("test-parent-context", result.parent?.context?.value)
        assertEquals("test-parent-id", result.parent?.id?.value)
    }

    @Test
    fun deviceArchetype() {
        val json = """
            {
                "id": "fake-device-id",
                "name": "Fake Device",
                "capabilitiesArchetype": "test"
            }
        """
        val archetype = Capabilities(
            archetypeId = "test",
            actions = setOf(Action.Lock::class),
            events = setOf(Event.Water::class),
        )
        val device = Json.decodeFromString(DeviceSerializer(setOf(archetype)), json)

        assertEquals(emptySet(), device.siblings)
        assertEquals(archetype, device.capabilities)
    }

    @Test(expected = Exception::class)
    fun invalidArchetype() {
        val json = """
            {
                "id": "fake-device-id",
                "name": "Fake Device",
                "capabilitiesArchetype": "not-real"
            }
        """
        val archetype = Capabilities(
            archetypeId = "test",
            actions = setOf(Action.Lock::class),
            events = setOf(Event.Water::class),
        )
        Json.decodeFromString(DeviceSerializer(setOf(archetype)), json)
    }
}
