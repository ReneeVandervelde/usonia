package usonia.hue

import com.github.ajalt.colormath.RGB
import inkapplications.shade.groups.Group
import inkapplications.shade.groups.GroupStateModification
import inkapplications.shade.groups.MutableGroupAttributes
import inkapplications.shade.groups.ShadeGroups
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import usonia.core.state.ActionAccessFake
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HueGroupHandlerTest {
    private val fakeHueBridge = FakeBridge.copy(
        service = HUE_SERVICE
    )

    private val fakeHueLight = FakeDevices.HueGroup.copy(
        parent = ExternalAssociation(
            context = fakeHueBridge.id,
            id = Identifier("fake-hue-id")
        )
    )

    @Test
    fun notConfigured() = runTest {
        val shadeSpy = ShadeGroupsSpy()
        val actionAccess = ActionAccessFake()
        val configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    rooms = setOf(
                        FakeRooms.LivingRoom.copy(
                            devices = setOf(FakeDevices.HueGroup)
                        )
                    ),
                    bridges = setOf(fakeHueBridge),
                )
            )
        }
        val client = DummyClient.copy(
            actionAccess = actionAccess,
            configurationAccess = configurationAccess,
        )

        val handler = HueGroupHandler(client, shadeSpy, requestScope = this)

        val handlerJob = launch { handler.start() }
        advanceUntilIdle()

        actionAccess.mutableActions.emit(Action.Switch(
            target = FakeDevices.HueGroup.id,
            state = SwitchState.ON,
        ))
        advanceUntilIdle()

        assertTrue(shadeSpy.groupsUpdated.isEmpty(), "No actions taken when not configured.")

        handlerJob.cancelAndJoin()
    }

    @Test
    fun nonHueGroup() = runTest {
        val shadeSpy = ShadeGroupsSpy()
        val actionAccess = ActionAccessFake()
        val configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    rooms = setOf(
                        FakeRooms.LivingRoom.copy(
                            devices = setOf(FakeDevices.HueGroup)
                        )
                    ),
                    bridges = setOf(fakeHueBridge),
                )
            )
        }
        val client = DummyClient.copy(
            actionAccess = actionAccess,
            configurationAccess = configurationAccess,
        )

        val handler = HueGroupHandler(client, shadeSpy, requestScope = this)

        val handlerJob = launch { handler.start() }
        advanceUntilIdle()

        actionAccess.mutableActions.emit(Action.Switch(
            target = FakeDevices.HueGroup.id,
            state = SwitchState.ON,
        ))
        advanceUntilIdle()

        assertTrue(shadeSpy.groupsUpdated.isEmpty(), "Non HueGroups should not be updated.")

        handlerJob.cancelAndJoin()
    }

    @Test
    fun nonLightAction() = runTest {
        val shadeSpy = ShadeGroupsSpy()
        val actionAccess = ActionAccessFake()
        val configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    rooms = setOf(
                        FakeRooms.LivingRoom.copy(
                            devices = setOf(fakeHueLight)
                        )
                    ),
                    bridges = setOf(fakeHueBridge),
                )
            )
        }
        val client = DummyClient.copy(
            actionAccess = actionAccess,
            configurationAccess = configurationAccess,
        )

        val handler = HueGroupHandler(client, shadeSpy, requestScope = this)

        val handlerJob = launch { handler.start() }
        advanceUntilIdle()

        actionAccess.mutableActions.emit(Action.Lock(
            target = FakeDevices.HueGroup.id,
            state = LockState.LOCKED,
        ))
        advanceUntilIdle()

        assertTrue(shadeSpy.groupsUpdated.isEmpty(), "Should Not handle non-light actions.")

        handlerJob.cancelAndJoin()
    }

    @Test
    fun handleSwitch() = handlesAction(Action.Switch(
        target = FakeDevices.HueGroup.id,
        state = SwitchState.ON,
    ))

    @Test
    fun handleDim() = handlesAction(Action.Dim(
        target = FakeDevices.HueGroup.id,
        level = 50.percent,
    ))

    @Test
    fun handleColorTemperature() = handlesAction(Action.ColorTemperatureChange(
        target = FakeDevices.HueGroup.id,
        temperature = ColorTemperature(5),
    ))

    @Test
    fun handleColor() = handlesAction(Action.ColorChange(
        target = FakeDevices.HueGroup.id,
        color = RGB(1, 2, 3)
    ))

    private fun handlesAction(action: Action) = runTest {
        val shadeSpy = ShadeGroupsSpy()
        val actionAccess = ActionAccessFake()
        val configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    rooms = setOf(
                        FakeRooms.LivingRoom.copy(
                            devices = setOf(fakeHueLight)
                        )
                    ),
                    bridges = setOf(fakeHueBridge),
                )
            )
        }
        val client = DummyClient.copy(
            actionAccess = actionAccess,
            configurationAccess = configurationAccess,
        )

        val handler = HueGroupHandler(client, shadeSpy, requestScope = this)

        val handlerJob = launch { handler.start() }
        advanceUntilIdle()

        actionAccess.mutableActions.emit(action)
        advanceUntilIdle()

        assertEquals("fake-hue-id", shadeSpy.groupsUpdated.single())

        handlerJob.cancelAndJoin()
    }

    class ShadeGroupsSpy: ShadeGroups {
        val groupsUpdated = mutableListOf<String>()
        override suspend fun setState(id: String, state: GroupStateModification) {
            groupsUpdated += id
        }
        override suspend fun createGroup(group: MutableGroupAttributes): String = TODO()
        override suspend fun deleteGroup(id: String) = TODO()
        override suspend fun getGroup(id: String): Group = TODO()
        override suspend fun getGroups(): Map<String, Group> = TODO()
        override suspend fun updateGroup(id: String, attributes: MutableGroupAttributes) = TODO()
    }
}
