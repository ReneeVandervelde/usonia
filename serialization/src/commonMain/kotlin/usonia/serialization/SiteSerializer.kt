package usonia.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.foundation.*
import usonia.kotlin.singleOrThrow

/**
 * Serialize/Deserialize site configuration.
 *
 * All of the data classes here are manually deserialized due to:
 * https://github.com/Kotlin/kotlinx.serialization/issues/532
 */
class SiteSerializer(
    private val archetypes: Set<Capabilities>,
): KSerializer<Site> {
    private val serializer = SiteJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): Site {
        val json = decoder.decodeSerializableValue(serializer)

        return Site(
            id = json.id.let(::Identifier),
            name = json.name ?: json.id,
            users = json.users.mapSet { user ->
                User(
                    id = user.id.let(::Identifier),
                    name = user.name ?: user.id,
                    parameters = user.parameters
                )
            },
            rooms = json.rooms.mapSet { room ->
                Room(
                    id = room.id.let(::Identifier),
                    name = room.name ?: room.id,
                    type = room.type?.let { Room.Type.valueOf(it) } ?: Room.Type.Generic,
                    adjacentRooms = room.adjacentRooms.mapSet(::Identifier),
                    devices = room.devices.mapSet { device ->
                        Device(
                            id = device.id.let(::Identifier),
                            name = device.name ?: device.id,
                            capabilities = when(device.capabilitiesArchetype) {
                                null -> Capabilities(
                                    archetypeId = null,
                                    actions = device.actionTypes.orEmpty().mapSet { action ->
                                        Action.subClasses.singleOrThrow("No Action of type $action") { it.simpleName == action }
                                    },
                                    events = device.eventTypes.orEmpty().mapSet { event ->
                                        Event.subClasses.singleOrThrow("No event of type: $event") { it.simpleName == event }
                                    }
                                )
                                else -> archetypes.singleOrThrow("No archetype of ID: ${device.capabilitiesArchetype}") { it.archetypeId == device.capabilitiesArchetype }
                            },
                            fixture = device.fixture?.let { Fixture.valueOf(it) },
                            siblings = device.siblings.mapSet { Identifier(it) },
                            parent = device.parentContext?.let {
                                ExternalAssociation(
                                    context = Identifier(it),
                                    id = Identifier(device.parentId ?: throw IllegalArgumentException("Context specified with no ID")),
                                )
                            }
                        )
                    }
                )
            },
            bridges = json.bridges.mapSet { bridge ->
                Bridge(
                    id = Identifier(bridge.id),
                    name = bridge.name ?: bridge.id,
                    service = bridge.service,
                    parameters = bridge.parameters.orEmpty(),
                )
            },
            parameters = json.parameters
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
                    adjacentRooms = room.adjacentRooms.mapSet(Identifier::value),
                    devices = room.devices.mapSet { device ->
                        DeviceJson(
                            id = device.id.value,
                            name = device.name,
                            capabilitiesArchetype = device.capabilities.archetypeId,
                            actionTypes = device.capabilities.actions.mapSet { it.simpleName!! },
                            eventTypes = device.capabilities.events.mapSet { it.simpleName!! },
                            fixture = device.fixture?.name,
                            siblings = device.siblings.mapSet(Identifier::value),
                            parentContext = device.parent?.context?.value,
                            parentId = device.parent?.id?.value,
                        )
                    }
                )
            },
            bridges = value.bridges.mapSet { bridge ->
                BridgeJson(
                    id = bridge.id.value,
                    service = bridge.service,
                    name = bridge.name,
                    parameters = bridge.parameters,
                )
            },
            parameters = value.parameters,
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
    val parameters: ParameterBag = emptyMap(),
)

@Serializable
data class BridgeJson(
    val id: String,
    val name: String? = null,
    val service: String,
    val parameters: ParameterBag? = null,
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
    val actionTypes: Set<String>? = null,
    val eventTypes: Set<String>? = null,
    val fixture: String? = null,
    val siblings: Set<String> = emptySet(),
    val parentContext: String? = null,
    val parentId: String? = null,
)
