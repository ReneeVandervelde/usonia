package usonia.hue

import com.github.ajalt.colormath.model.RGB
import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import inkapplications.shade.lights.LightControls
import inkapplications.shade.lights.parameters.LightUpdateParameters
import inkapplications.shade.lights.structures.Light
import inkapplications.shade.structures.ResourceId
import inkapplications.shade.structures.ResourceReference
import inkapplications.shade.structures.ResourceType
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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
class HueLightHandlerTest {
    private val fakeHueBridge = FakeBridge.copy(
        service = HUE_SERVICE
    )

    private val fakeHueLight = FakeDevices.HueColorLight.copy(
        parent = ExternalAssociation(
            context = fakeHueBridge.id,
            id = Identifier("fake-hue-id")
        )
    )

    @Test
    fun notConfigured() = runTest {
        val shadeSpy = ShadeLightsSpy()
        val actionAccess = ActionAccessFake()
        val configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    rooms = setOf(
                        FakeRooms.LivingRoom.copy(
                            devices = setOf(FakeDevices.HueColorLight)
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

        val handler = HueLightHandler(client, shadeSpy, requestScope = this)

        val handlerJob = launch { handler.startDaemon() }
        runCurrent()
        actionAccess.mutableActions.emit(Action.Switch(
            target = FakeDevices.HueColorLight.id,
            state = SwitchState.ON,
        ))
        runCurrent()

        assertTrue(shadeSpy.updated.isEmpty(), "No actions taken when not configured.")

        handlerJob.cancelAndJoin()
    }

    @Test
    fun nonHueLight() = runTest {
        val shadeSpy = ShadeLightsSpy()
        val actionAccess = ActionAccessFake()
        val configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    rooms = setOf(
                        FakeRooms.LivingRoom.copy(
                            devices = setOf(FakeDevices.HueColorLight)
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

        val handler = HueLightHandler(client, shadeSpy, requestScope = backgroundScope)

        val handlerJob = launch { handler.startDaemon() }
        runCurrent()

        actionAccess.mutableActions.emit(Action.Switch(
            target = FakeDevices.HueColorLight.id,
            state = SwitchState.ON,
        ))
        runCurrent()

        assertTrue(shadeSpy.updated.isEmpty(), "Non lights should not be updated.")

        handlerJob.cancelAndJoin()
    }

    @Test
    fun nonLightAction() = runTest {
        val shadeSpy = ShadeLightsSpy()
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

        val handler = HueLightHandler(client, shadeSpy, requestScope = this)

        val handlerJob = launch { handler.startDaemon() }
        runCurrent()

        actionAccess.mutableActions.emit(Action.Lock(
            target = FakeDevices.HueColorLight.id,
            state = LockState.LOCKED,
        ))
        runCurrent()

        assertTrue(shadeSpy.updated.isEmpty(), "Should Not handle non-light actions.")

        handlerJob.cancelAndJoin()
    }

    @Test
    fun handleSwitch() = handlesAction(Action.Switch(
        target = FakeDevices.HueColorLight.id,
        state = SwitchState.ON,
    ))

    @Test
    fun handleDim() = handlesAction(Action.Dim(
        target = FakeDevices.HueColorLight.id,
        level = 50.percent,
    ))

    @Test
    fun handleColorTemperature() = handlesAction(Action.ColorTemperatureChange(
        target = FakeDevices.HueColorLight.id,
        temperature = 5.kelvin,
    ))

    @Test
    fun handleColor() = handlesAction(Action.ColorChange(
        target = FakeDevices.HueColorLight.id,
        color = RGB(1, 2, 3)
    ))

    private fun handlesAction(action: Action) = runTest {
        val shadeSpy = ShadeLightsSpy()
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

        val handler = HueLightHandler(client, shadeSpy, requestScope = this)

        val handlerJob = launch { handler.startDaemon() }
        runCurrent()

        actionAccess.mutableActions.emit(action)
        runCurrent()

        assertEquals("fake-hue-id", shadeSpy.updated.single())

        handlerJob.cancelAndJoin()
    }

    class ShadeLightsSpy: LightControls {
        val updated = mutableListOf<String>()
        override suspend fun getLight(id: ResourceId): Light = TODO()
        override suspend fun listLights(): List<Light> = TODO()
        override suspend fun updateLight(id: ResourceId, parameters: LightUpdateParameters): ResourceReference {
            updated += id.value

            return ResourceReference(id, ResourceType.Light)
        }
    }
}
