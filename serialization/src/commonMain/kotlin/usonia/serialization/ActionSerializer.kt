package usonia.serialization

import com.github.ajalt.colormath.RGB
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.foundation.Action
import usonia.foundation.LockState
import usonia.foundation.SwitchState
import usonia.foundation.Uuid
import usonia.foundation.unit.ColorTemperature
import usonia.foundation.unit.Percentage

object ActionSerializer: KSerializer<Action> {
    private val serializer = ActionJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): Action {
        val json = decoder.decodeSerializableValue(serializer)
        val target = json.target.let(::Uuid)

        return when(json.type) {
            Action.Switch::class.simpleName -> Action.Switch(
                target = target,
                state = json.switchState!!.let { SwitchState.valueOf(it) },
            )
            Action.Dim::class.simpleName -> Action.Dim(
                target = target,
                level = json.dimLevel!!.let(::Percentage),
                switchState = json.switchState?.let { SwitchState.valueOf(it) },
            )
            Action.ColorTemperatureChange::class.simpleName -> Action.ColorTemperatureChange(
                target = target,
                temperature = json.colorTemperature!!.let(::ColorTemperature),
                level = json.dimLevel!!.let(::Percentage),
                switchState = json.switchState?.let { SwitchState.valueOf(it) },
            )
            Action.ColorChange::class.simpleName -> Action.ColorChange(
                target = target,
                color = RGB(json.color!![0], json.color[1], json.color[2]),
                level = json.dimLevel!!.let(::Percentage),
                switchState = json.switchState?.let { SwitchState.valueOf(it) },
            )
            Action.Lock::class.simpleName -> Action.Lock(
                target = target,
                state = json.lockState!!.let { LockState.valueOf(it) },
            )
            Action.Intent::class.simpleName -> Action.Intent(
                target = target,
                action = json.intentAction!!,
            )
            Action.Alert::class.simpleName -> Action.Alert(
                target = target,
                message = json.alertMessage!!
            )
            else -> throw IllegalArgumentException("Unknown type: ${json.type}")
        }
    }

    override fun serialize(encoder: Encoder, value: Action) {
        val prototype = ActionJson(
            type = value::class.simpleName!!,
            target = value.target.value,
        )
        when (value) {
            is Action.Switch -> prototype.copy(
                switchState = value.state.name,
            )
            is Action.Dim -> prototype.copy(
                dimLevel = value.level.fraction,
                switchState = value.switchState?.name,
            )
            is Action.ColorTemperatureChange -> prototype.copy(
                colorTemperature = value.temperature.kelvinValue,
                switchState = value.switchState?.name,
                dimLevel = value.level?.fraction,
            )
            is Action.ColorChange -> prototype.copy(
                color = value.color.toRGB().let { listOf(it.r, it.g, it.b) },
                switchState = value.switchState?.name,
                dimLevel = value.level?.fraction,
            )
            is Action.Lock -> prototype.copy(
                lockState = value.state.name,
            )
            is Action.Intent -> prototype.copy(
                intentAction = value.action,
            )
            is Action.Alert -> prototype.copy(
                alertMessage = value.message,
            )
        }
    }

}

@Serializable
internal data class ActionJson(
    val type: String,
    val target: String,
    val switchState: String? = null,
    val lockState: String? = null,
    val dimLevel: Float? = null,
    val color: List<Int>? = null,
    val colorTemperature: Int? = null,
    val intentAction: String? = null,
    val alertMessage: String? = null,
)
