package usonia.foundation

import com.github.ajalt.colormath.Color
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
        val message: String
    ): Action() {
        override fun withTarget(target: Identifier): Action = copy(target = target)
    }
}

