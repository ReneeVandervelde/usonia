package usonia.hue

import com.github.ajalt.colormath.model.RGB
import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import inkapplications.shade.groupedlights.GroupedLightControls
import inkapplications.shade.groupedlights.parameters.GroupedLightUpdateParameters
import inkapplications.shade.groupedlights.structures.GroupedLight
import inkapplications.shade.structures.ResourceId
import inkapplications.shade.structures.ResourceReference
import inkapplications.shade.structures.ResourceType
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import usonia.core.state.ActionAccessFake
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
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

        val handlerJob = launch { handler.startDaemon() }
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

        val handlerJob = launch { handler.startDaemon() }
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

        val handlerJob = launch { handler.startDaemon() }
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
        temperature = 5.kelvin,
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

        val handlerJob = launch { handler.startDaemon() }
        advanceUntilIdle()

        actionAccess.mutableActions.emit(action)
        advanceUntilIdle()

        assertEquals("fake-hue-id", shadeSpy.groupsUpdated.single())

        handlerJob.cancelAndJoin()
    }

    class ShadeGroupsSpy: GroupedLightControls {
        val groupsUpdated = mutableListOf<String>()

        override suspend fun getGroup(id: ResourceId): GroupedLight = TODO()
        override suspend fun listGroups(): List<GroupedLight> = TODO()
        override suspend fun updateGroup(id: ResourceId, parameters: GroupedLightUpdateParameters): ResourceReference {
            groupsUpdated += id.value

            return ResourceReference(id, ResourceType.GroupedLight)
        }
    }
}
