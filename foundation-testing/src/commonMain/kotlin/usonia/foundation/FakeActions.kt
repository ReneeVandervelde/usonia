package usonia.foundation

object FakeActions {
    val SwitchOff = Action.Switch(
        target = FakeDevices.Switch.id,
        state = SwitchState.OFF
    )
}
