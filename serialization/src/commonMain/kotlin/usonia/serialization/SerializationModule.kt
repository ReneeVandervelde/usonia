package usonia.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import usonia.foundation.DeviceSerializer
import usonia.hue.HueArchetypes
import usonia.schlage.SchlageArchetypes
import usonia.smartthings.SmartThingsArchetypes
import usonia.xiaomi.XiaomiArchetypes

object SerializationModule {
    private val archetypes = setOf(
        *SmartThingsArchetypes.ALL.toTypedArray(),
        HueArchetypes.group,
        SchlageArchetypes.connectLock,
        XiaomiArchetypes.temperature,
    )

    val json = Json {
        serializersModule = SerializersModule {
            contextual(DeviceSerializer(archetypes))
        }
    }
}
