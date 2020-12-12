package usonia.foundation

import com.github.ajalt.colormath.Color
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.Percentage

/**
 * Instructions for devices to do something or change state.
 */
sealed class Action {
    /**
     * The device intended to receive the action.
     */
    abstract val target: Uuid

    data class Switch(
        override val target: Uuid,
        val state: SwitchState
    ): Action()

    data class Dim(
        override val target: Uuid,
        val level: Percentage,
        val switchState: SwitchState? = null
    ): Action()

    data class ColorTemperatureChange(
        override val target: Uuid,
        val temperature: ColorTemperature,
        val switchState: SwitchState? = null,
        val level: Percentage? = null
    ): Action()

    data class ColorChange(
        override val target: Uuid,
        val color: Color,
        val switchState: SwitchState? = null,
        val level: Percentage? = null
    ): Action()

    data class Lock(
        override val target: Uuid,
        val state: LockState
    ): Action()

    data class Intent(
        override val target: Uuid,
        val action: String
    ): Action()

    data class Alert(
        override val target: Uuid,
        val message: String
    ): Action()
}

