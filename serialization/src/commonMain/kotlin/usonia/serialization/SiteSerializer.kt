package usonia.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.foundation.*

/**
 * Serialize/Deserialize site configuration.
 *
 * All of the data classes here are manually deserialized due to:
 * https://github.com/Kotlin/kotlinx.serialization/issues/532
 */
object SiteSerializer: KSerializer<Site> {
    private val serializer = SiteJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): Site {
        val json = decoder.decodeSerializableValue(serializer)

        return Site(
            id = json.id.let(::Uuid),
            name = json.name ?: json.id,
            users = json.users.mapSet { user ->
                User(
                    id = user.id.let(::Uuid),
                    name = user.name ?: user.id,
                    parameters = user.parameters
                )
            },
            rooms = json.rooms.mapSet { room ->
                Room(
                    id = room.id.let(::Uuid),
                    name = room.name ?: room.id,
                    type = room.type?.let { Room.Type.valueOf(it) } ?: Room.Type.Generic,
                    adjacentRooms = room.adjacentRooms.mapSet(::Uuid),
                    devices = room.devices.mapSet { device ->
                        Device(
                            id = device.id.let(::Uuid),
                            name = device.name ?: device.id,
                            capabilities = Capabilities(),
                            fixture = device.fixture?.let { Fixture.valueOf(it) },
                            siblings = device.siblings.mapSet(::Uuid)
                        )
                    }
                )
            },
            bridges = json.bridges.mapSet { bridge ->
                Bridge(
                    id = bridge.id.let(::Uuid),
                    name = bridge.name,
                    host = bridge.host,
                    port = bridge.port,
                    actionsPath = bridge.actionsPath,
                    parameters = bridge.parameters,
                )
            }
        )
    }

    override fun serialize(encoder: Encoder, value: Site) {
        val json = SiteJson(
            id = value.id.value,
            name = value.name,
            users = value.users.mapSet { user ->
                UserJson(
                    id = user.id.value,
                    name = user.name,
                    parameters = user.parameters
                )
            },
            rooms = value.rooms.mapSet { room ->
                RoomJson(
                    id = room.id.value,
                    name = room.name,
                    type = room.type.name,
                    adjacentRooms = room.adjacentRooms.mapSet(Uuid::value),
                    devices = room.devices.mapSet { device ->
                        DeviceJson(
                            id = device.id.value,
                            name = device.name,
                            capabilitiesArchetype = null,
                            fixture = device.fixture?.name,
                            siblings = device.siblings.mapSet(Uuid::value)
                        )
                    }
                )
            },
            bridges = value.bridges.mapSet { bridge ->
                BridgeJson(
                    id = bridge.id.value,
                    name = bridge.name,
                    host = bridge.host,
                    port = bridge.port,
                    actionsPath = bridge.actionsPath,
                    parameters = bridge.parameters,
                )
            }
        )

        encoder.encodeSerializableValue(serializer, json)
    }

    private inline fun <T, R> Set<T>.mapSet(mapping: (T) -> R): Set<R> = map(mapping).toSet()
}

@Serializable
internal data class SiteJson(
    val id: String,
    val name: String? = null,
    val users: Set<UserJson> = emptySet(),
    val rooms: Set<RoomJson> = emptySet(),
    val bridges: Set<BridgeJson> = emptySet(),
)

@Serializable
data class BridgeJson(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val actionsPath: String?,
    val parameters: ParameterBag,
)

@Serializable
internal data class UserJson(
    val id: String,
    val name: String? = null,
    val parameters: ParameterBag = emptyMap(),
)

@Serializable
internal data class RoomJson(
    val id: String,
    val name: String? = null,
    val type: String? = null,
    val adjacentRooms: Set<String> = emptySet(),
    val devices: Set<DeviceJson> = emptySet(),
)

@Serializable
internal data class DeviceJson(
    val id: String,
    val name: String? = null,
    val capabilitiesArchetype: String? = null,
    val fixture: String? = null,
    val siblings: Set<String> = emptySet(),
)
