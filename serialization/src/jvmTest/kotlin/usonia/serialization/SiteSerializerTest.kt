package usonia.serialization

import kotlinx.serialization.json.Json
import usonia.foundation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SiteSerializerTest {
    @Test
    fun full() {
        val json = """
            {
              "id": "fake-site-id",
              "name": "Fake Site",
              "users": [
                {
                  "id": "fake-user-id",
                  "name": "Fake User",
                  "parameters": {
                    "foo": "bar",
                    "baz": "qux"
                  }
                }
              ],
              "rooms": [
                {
                  "id": "fake-room-id",
                  "name": "Fake Room",
                  "type": "Office",
                  "adjacentRooms": [
                    "adjacent-room-id"
                  ],
                  "devices": [
                    {
                      "id": "fake-device-id",
                      "name": "Fake Device",
                      "fixture": "Light",
                      "siblings": [
                        "fake-sibling-id"
                      ]
                    }
                  ]
                }
              ]
            }
        """
        val result = Json.decodeFromString(SiteSerializer, json)

        assertEquals("fake-site-id", result.id.value)
        assertEquals("Fake Site", result.name)
        val user = result.users.single()
        assertEquals("fake-user-id", user.id.value)
        assertEquals("Fake User", user.name)
        assertEquals(
            mapOf("foo" to "bar", "baz" to "qux"),
            user.parameters
        )
        val room = result.rooms.single()
        assertEquals("fake-room-id", room.id.value)
        assertEquals("Fake Room", room.name)
        assertEquals(Room.Type.Office, room.type)
        assertEquals("adjacent-room-id", room.adjacentRooms.single().value)
        val device = room.devices.single()
        assertEquals("fake-device-id", device.id.value)
        assertEquals("Fake Device", device.name)
        assertEquals(emptySet(), device.capabilities.actions)
        assertEquals(emptySet(), device.capabilities.events)
    }

    @Test
    fun minimal() {
        val json = """
            {
              "id": "fake-site-id"
            }
        """
        val result = Json.decodeFromString(SiteSerializer, json)

        assertEquals("fake-site-id", result.id.value)
        assertEquals("fake-site-id", result.name)
        assertEquals(emptySet(), result.users)
        assertEquals(emptySet(), result.rooms)
    }

    @Test
    fun minimalRoom() {
        val json = """
            {
              "id": "fake-site-id",
              "rooms": [
                {
                  "id": "fake-room-id"
                }
              ]
            }
        """
        val result = Json.decodeFromString(SiteSerializer, json)

        val room = result.rooms.single()
        assertEquals("fake-room-id", room.id.value)
        assertEquals("fake-room-id", room.name)
        assertEquals(Room.Type.Generic, room.type)
        assertEquals(emptySet(), room.adjacentRooms)
        assertEquals(emptySet(), room.devices)
    }

    @Test
    fun minimalDevice() {
        val json = """
            {
              "id": "fake-site-id",
              "rooms": [
                {
                  "id": "fake-room-id",
                  "devices": [
                    {"id": "fake-device-id"}
                  ]
                }
              ]
            }
        """
        val result = Json.decodeFromString(SiteSerializer, json)

        val room = result.rooms.single()
        val device = room.devices.single()

        assertEquals("fake-device-id", device.id.value)
        assertEquals("fake-device-id", device.name)
        assertNull(device.fixture)
        assertEquals(emptySet(), device.siblings)
        assertEquals(emptySet(), device.capabilities.actions)
        assertEquals(emptySet(), device.capabilities.events)
    }
}
