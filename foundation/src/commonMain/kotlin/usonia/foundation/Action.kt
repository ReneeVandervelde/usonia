package usonia.foundation

import com.github.ajalt.colormath.Color
import inkapplications.spondee.measure.ColorTemperature
import inkapplications.spondee.scalar.Percentage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Instructions for devices to do something or change state.
 */
@Serializable
sealed class Action
{
    /**
     * The device intended to receive the action.
     */
    abstract val target: Identifier

    abstract fun withTarget(target: Identifier): Action

    @Serializable
    @SerialName("Switch")
    data class Switch(
        override val target: Identifier,
        @SerialName("switchState")
        val state: SwitchState
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    @Serializable
    @SerialName("Valve")
    data class Valve(
        override val target: Identifier,
        @SerialName("valveState")
        val state: ValveState,
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    @Serializable
    @SerialName("Dim")
    data class Dim(
        override val target: Identifier,
        @SerialName("dimLevel")
        @Serializable(with = DecimalPercentageSerializer::class)
        val level: Percentage,
        val switchState: SwitchState? = null
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    @Serializable
    @SerialName("ColorTemperatureChange")
    data class ColorTemperatureChange(
        override val target: Identifier,
        @Serializable(with = ColorTemperatureKelvinRoundedIntSerializer::class)
        @SerialName("colorTemperature")
        val temperature: ColorTemperature,
        val switchState: SwitchState? = null,
        @SerialName("dimLevel")
        @Serializable(with = DecimalPercentageSerializer::class)
        val level: Percentage? = null
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    @Serializable
    @SerialName("ColorChange")
    data class ColorChange(
        override val target: Identifier,
        @Serializable(with = ColorSrgbArraySerializer::class)
        val color: Color,
        val switchState: SwitchState? = null,
        @SerialName("dimLevel")
        @Serializable(with = DecimalPercentageSerializer::class)
        val level: Percentage? = null
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    @Serializable
    @SerialName("Lock")
    data class Lock(
        override val target: Identifier,
        @SerialName("lockState")
        val state: LockState
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    @Serializable
    @SerialName("Intent")
    data class Intent(
        override val target: Identifier,
        @SerialName("intentAction")
        val action: String
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }

    @Serializable
    @SerialName("Alert")
    data class Alert(
        override val target: Identifier,
        @SerialName("alertMessage")
        val message: String,
        @SerialName("alertLevel")
        val level: Level = Level.Debug,
        val icon: Icon? = null,
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)

        enum class Icon {
            Suspicious,
            Flood,
            Pipes,
            Bot,
            Confused,
            Disallowed,
            Danger,
            Panic,
            Sleep,
            Wake,
            Entertained,
            Wave,
        }

        enum class Level {
            Debug,
            Info,
            Warning,
            Emergency,
        }
    }
}
