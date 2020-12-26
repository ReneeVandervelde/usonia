package usonia.foundation

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.kotlin.mapSet
import usonia.kotlin.singleOrThrow

data class Device(
    val id: Identifier,
    val name: String,
    val capabilities: Capabilities,
    val fixture: Fixture? = null,
    val siblings: Set<Identifier> = emptySet(),
    val parent: ExternalAssociation? = null,
)

class DeviceSerializer(
    private val archetypes: Set<Capabilities>,
): KSerializer<Device> {
    private val serializer = DeviceJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): Device {
        val device = serializer.deserialize(decoder)

        return Device(
            id = Identifier(device.id),
            name = device.name,
            capabilities = when (device.capabilitiesArchetype) {
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
            siblings = device.siblings.mapSet(::Identifier),
            parent = device.parentContext?.let {
                ExternalAssociation(
                    context = Identifier(it),
                    id = Identifier(device.parentId ?: throw IllegalArgumentException("Context specified with no ID")),
                )
            },
        )
    }

    override fun serialize(encoder: Encoder, value: Device) {
        val json = DeviceJson(
            id = value.id.value,
            name = value.name,
            capabilitiesArchetype = value.capabilities.archetypeId,
            actionTypes = value.capabilities.actions.mapSet { it.simpleName!! },
            eventTypes = value.capabilities.events.mapSet { it.simpleName!! },
            fixture = value.fixture?.name,
            siblings = value.siblings.mapSet(Identifier::value),
            parentContext = value.parent?.context?.value,
            parentId = value.parent?.id?.value,
        )

        encoder.encodeSerializableValue(serializer, json)
    }
}


@Serializable
internal data class DeviceJson(
    val id: String,
    val name: String,
    val capabilitiesArchetype: String? = null,
    val actionTypes: Set<String>? = null,
    val eventTypes: Set<String>? = null,
    val fixture: String? = null,
    val siblings: Set<String> = emptySet(),
    val parentContext: String? = null,
    val parentId: String? = null,
)
