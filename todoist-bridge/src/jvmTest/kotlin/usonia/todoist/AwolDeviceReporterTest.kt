package usonia.todoist

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccess
import usonia.core.state.EventAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.datetime.UtcClock
import usonia.kotlin.datetime.current
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import usonia.todoist.ApiStub.StubTask
import usonia.todoist.api.Task
import usonia.todoist.api.TodoistApi
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class AwolDeviceReporterTest {
    val config = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(FakeRooms.LivingRoom.copy(
                devices = setOf(FakeDevices.WaterSensor.copy(
                    id = Identifier("fake-sensor"),
                    name = "Fake Sensor",
                    capabilities = FakeDevices.WaterSensor.capabilities.copy(
                        heartbeat = 1.minutes,
                        events = setOf(
                            Event.Water::class,
                            Event.Battery::class,
                        )
                    )
                ))
            )),
            bridges = setOf(
                FakeBridge.copy(
                    service = "todoist",
                    parameters = mapOf(
                        "token" to "test-token",
                        "project" to "666",
                        "label" to "Nice",
                    )
                )
            )
        ))
    }
    val testClient = DummyClient.copy(
        configurationAccess = config,
    )
    val time = UtcClock.current

    @Test
    fun correctParameters() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(null)
        }
        val api = object: TodoistApi by ApiStub {
            var tokenUsed: String? = null
            var projectUsed: String? = null
            var labelUsed: String? = null

            override suspend fun getTasks(token: String, projectId: String?, label: String?): List<Task> {
                tokenUsed = token
                projectUsed = projectId
                labelUsed = label

                return emptyList()
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals("test-token", api.tokenUsed)
        assertEquals("666", api.projectUsed)
        assertEquals("Nice", api.labelUsed)
    }

    @Test
    fun noAwolDevices() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant
                    ) as T
                } else null
            }
        }
        val api = ApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(0, api.created.size, "created")
    }

    @Test
    fun tasksCreated() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant - 2.minutes
                    ) as T
                } else null
            }
        }
        val api = ApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(1, api.created.size, "created")
        val parameters = api.created.single()
        assertEquals("Replace Batteries in Fake Sensor", parameters.content)
        assertEquals("(id: fake-sensor)", parameters.description)
        assertEquals("Today", parameters.dueString)
        assertEquals("666", parameters.projectId)
        assertEquals(listOf("Nice"), parameters.labels)
    }

    @Test
    fun taskCreatedWithoutHistoryWhenStale() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
        }
        val api = ApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(1, api.created.size, "created")
    }

    @Test
    fun notCreatedWithoutHistory() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(time.instant)
        }
        val api = ApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(0, api.created.size, "created")
    }

    @Test
    fun taskClosed() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant
                    ) as T
                } else null
            }
        }
        val api = object: ApiSpy() {
            override suspend fun getTasks(token: String, projectId: String?, label: String?): List<Task> {
                return listOf(Task(
                    id = "432",
                    content = "Test Task",
                    description = "(id: fake-sensor)",
                    completed = false,
                ))
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(1, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(0, api.created.size, "created")
        assertEquals("432", api.closed.single())
    }

    @Test
    fun stillAwol() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant - 2.minutes
                    ) as T
                } else null
            }
        }
        val api = object: ApiSpy() {
            override suspend fun getTasks(token: String, projectId: String?, label: String?): List<Task> {
                return listOf(Task(
                    id = "432",
                    content = "Test Task",
                    description = "(id: fake-sensor)",
                    completed = false,
                ))
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(0, api.created.size, "created")
    }

    @Test
    fun notClosedWithoutHistory() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(time.instant)
        }
        val api = ApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(0, api.created.size, "created")
    }

    @Test
    fun noClosableTask() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant - 2.minutes
                    ) as T
                } else null
            }
        }
        val api = object: ApiSpy() {
            override suspend fun getTasks(token: String, projectId: String?, label: String?): List<Task> {
                return listOf(StubTask.copy(
                    description = "(id: fake-sensor)",
                ))
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(0, api.created.size, "created")
    }

    @Test
    fun lowBatteryEvent() = runTest {
        val api = ApiSpy()
        val events = object: EventAccess by EventAccessStub {
            override val events: OngoingFlow<Event> = ongoingFlowOf(
                FakeEvents.LowBattery.copy(
                    source = Identifier("fake-sensor"),
                ),
            )
        }
        val client = testClient.copy(
            eventAccess = events,
            configurationAccess = config,
        )
        val daemon = launch { AwolDeviceReporter(client, api).startDaemon() }
        advanceUntilIdle()

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(1, api.created.size, "created")
        daemon.cancel()
    }

    @Test
    fun lowBatteryCleared() = runTest {
        val api = object: ApiSpy() {
            override suspend fun getTasks(token: String, projectId: String?, label: String?): List<Task> {
                return listOf(Task(
                    id = "432",
                    content = "Low battery in Test",
                    description = "(id: fake-sensor)",
                    completed = false,
                ))
            }
        }
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Battery::class) {
                    FakeEvents.FullBattery.copy(
                        timestamp = time.instant,
                    ) as T
                } else null
            }
        }
        val client = testClient.copy(
            eventAccess = events,
            configurationAccess = config,
        )
        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(1, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(0, api.created.size, "created")
    }

    @Test
    fun lowBatteryNotCleared() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant
                    ) as T
                } else null
            }
        }
        val api = object: ApiSpy() {
            override suspend fun getTasks(token: String, projectId: String?, label: String?): List<Task> {
                return listOf(Task(
                    id = "432",
                    content = "Low battery for Test Task",
                    description = "(id: fake-sensor)",
                    completed = false,
                ))
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size, "closed")
        assertEquals(0, api.updated.size, "updated")
        assertEquals(0, api.created.size, "created")
    }

    @Test
    fun lowBatteryUpgraded() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.LowBattery.copy(
                        timestamp = time.instant - 2.minutes
                    ) as T
                } else null
            }
        }
        val api = object: ApiSpy() {
            override suspend fun getTasks(token: String, projectId: String?, label: String?): List<Task> {
                return listOf(Task(
                    id = "432",
                    content = "Low battery for Test Task",
                    description = "(id: fake-sensor)",
                    completed = false,
                ))
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time.localDateTime, time.zone)

        assertEquals(0, api.closed.size)
        assertEquals(1, api.updated.size)
        assertEquals(0, api.created.size)
    }
}
