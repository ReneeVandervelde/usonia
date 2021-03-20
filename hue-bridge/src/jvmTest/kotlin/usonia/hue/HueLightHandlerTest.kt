package usonia.hue

import com.github.ajalt.colormath.RGB
import inkapplications.shade.constructs.Scan
import inkapplications.shade.lights.Light
import inkapplications.shade.lights.LightStateModification
import inkapplications.shade.lights.ShadeLights
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
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
    fun notConfigured() = runBlockingTest {
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

        val handlerJob = launch { handler.start() }

        pauseDispatcher {
            actionAccess.mutableActions.emit(Action.Switch(
                target = FakeDevices.HueColorLight.id,
                state = SwitchState.ON,
            ))
        }

        assertTrue(shadeSpy.updated.isEmpty(), "No actions taken when not configured.")

        handlerJob.cancelAndJoin()
    }

    @Test
    fun nonHueLight() = runBlockingTest {
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

        val handlerJob = launch { handler.start() }

        pauseDispatcher {
            actionAccess.mutableActions.emit(Action.Switch(
                target = FakeDevices.HueColorLight.id,
                state = SwitchState.ON,
            ))
        }

        assertTrue(shadeSpy.updated.isEmpty(), "Non lights should not be updated.")

        handlerJob.cancelAndJoin()
    }

    @Test
    fun nonLightAction() = runBlockingTest {
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

        val handlerJob = launch { handler.start() }

        pauseDispatcher {
            actionAccess.mutableActions.emit(Action.Lock(
                target = FakeDevices.HueColorLight.id,
                state = LockState.LOCKED,
            ))
        }

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
        temperature = ColorTemperature(5),
    ))

    @Test
    fun handleColor() = handlesAction(Action.ColorChange(
        target = FakeDevices.HueColorLight.id,
        color = RGB(1, 2, 3)
    ))

    private fun handlesAction(action: Action) = runBlockingTest {
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

        val handlerJob = launch { handler.start() }

        pauseDispatcher {
            actionAccess.mutableActions.emit(action)
        }

        assertEquals("fake-hue-id", shadeSpy.updated.single())

        handlerJob.cancelAndJoin()
    }

    class ShadeLightsSpy: ShadeLights {
        val updated = mutableListOf<String>()
        override suspend fun setState(id: String, state: LightStateModification) {
            updated += id
        }
        override suspend fun delete(id: String) = TODO()
        override suspend fun getLight(id: String): Light = TODO()
        override suspend fun getLights(): Map<String, Light> = TODO()
        override suspend fun getNewLights(): Scan = TODO()
        override suspend fun rename(id: String, name: String) = TODO()
        override suspend fun search(vararg deviceIds: String) = TODO()
    }
}
