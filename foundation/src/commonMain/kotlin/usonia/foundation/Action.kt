package usonia.foundation

import com.github.ajalt.colormath.Color
import com.github.ajalt.colormath.RGB
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.Percentage

/**
 * Instructions for devices to do something or change state.
 */
sealed class Action {
    companion object Metadata {
        val subClasses = setOf(
            Switch::class,
            Dim::class,
            ColorTemperatureChange::class,
            ColorChange::class,
            Lock::class,
            Intent::class,
            Alert::class,
        )
    }

    /**
     * The device intended to receive the action.
     */
    abstract val target: Identifier

    abstract fun withTarget(target: Identifier): Action

    data class Switch(
        override val target: Identifier,
        val state: SwitchState
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    data class Dim(
        override val target: Identifier,
        val level: Percentage,
        val switchState: SwitchState? = null
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    data class ColorTemperatureChange(
        override val target: Identifier,
        val temperature: ColorTemperature,
        val switchState: SwitchState? = null,
        val level: Percentage? = null
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    data class ColorChange(
        override val target: Identifier,
        val color: Color,
        val switchState: SwitchState? = null,
        val level: Percentage? = null
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    data class Lock(
        override val target: Identifier,
        val state: LockState
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    data class Intent(
        override val target: Identifier,
        val action: String
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    data class Alert(
        override val target: Identifier,
        val message: String,
        val level: Level = Level.Debug,
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)

        enum class Level {
            Debug,
            Info,
            Warning,
            Emergency,
        }
    }
}

object ActionSerializer: KSerializer<Action> {
    private val serializer = ActionJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): Action {
        val json = decoder.decodeSerializableValue(serializer)
        val target = json.target.let(::Identifier)

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
                message = json.alertMessage!!,
                level = json.alertLevel?.let { Action.Alert.Level.valueOf(it) } ?: Action.Alert.Level.Info,
            )
            else -> throw IllegalArgumentException("Unknown type: ${json.type}")
        }
    }

    override fun serialize(encoder: Encoder, value: Action) {
        val prototype = ActionJson(
            type = value::class.simpleName!!,
            target = value.target.value,
        )
        val json = when (value) {
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
                alertLevel = value.level.name,
            )
        }

        encoder.encodeSerializableValue(serializer, json)
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
    val alertLevel: String? = null,
)
