package usonia.todoist

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import usonia.todoist.api.Task
import usonia.todoist.api.TaskParameters
import usonia.todoist.api.TodoistApi
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class AwolDeviceReporterTest {
    val config = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(FakeRooms.LivingRoom.copy(
                devices = setOf(FakeDevices.WaterSensor.copy(
                    id = Identifier("fake-sensor"),
                    name = "Fake Sensor",
                    capabilities = FakeDevices.WaterSensor.capabilities.copy(
                        heartbeat = 1.minutes,
                    )
                ))
            )),
            bridges = setOf(
                FakeBridge.copy(
                    service = "todoist",
                    parameters = mapOf(
                        "token" to "test-token",
                        "project" to "666",
                        "label" to "420",
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
            var projectUsed: Long? = null
            var labelUsed: Long? = null

            override suspend fun getTasks(token: String, projectId: Long?, labelId: Long?): List<Task> {
                tokenUsed = token
                projectUsed = projectId
                labelUsed = labelId

                return emptyList()
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals("test-token", api.tokenUsed)
        assertEquals(666, api.projectUsed)
        assertEquals(420, api.labelUsed)
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
        val api = object: TodoistApi by ApiStub {
            val created = mutableListOf<TaskParameters>()
            override suspend fun create(token: String, task: TaskParameters): Task {
                created += task
                return ApiStub.create(token, task)
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals(0, api.created.size)
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
        val api = object: TodoistApi by ApiStub {
            val created = mutableListOf<TaskParameters>()
            override suspend fun create(token: String, task: TaskParameters): Task {
                created += task
                return ApiStub.create(token, task)
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals(1, api.created.size)
        val parameters = api.created.single()
        assertEquals("Replace Batteries in Fake Sensor (id: fake-sensor)", parameters.content)
        assertEquals(666L, parameters.projectId)
        assertEquals(listOf(420L), parameters.labels)
    }

    @Test
    fun taskCreatedWithoutHistoryWhenStale() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
        }
        val api = object: TodoistApi by ApiStub {
            val created = mutableListOf<TaskParameters>()
            override suspend fun create(token: String, task: TaskParameters): Task {
                created += task
                return ApiStub.create(token, task)
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals(1, api.created.size)
    }

    @Test
    fun notCreatedWithoutHistory() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(time.instant)
        }
        val api = object: TodoistApi by ApiStub {
            val created = mutableListOf<TaskParameters>()
            override suspend fun create(token: String, task: TaskParameters): Task {
                created += task
                return ApiStub.create(token, task)
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals(0, api.created.size)
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
        val api = object: TodoistApi by ApiStub {
            val closed = mutableListOf<Long>()
            override suspend fun getTasks(token: String, projectId: Long?, labelId: Long?): List<Task> {
                return listOf(Task(
                    id = 432,
                    content = "Test Task (id: fake-sensor)",
                    completed = false,
                ))
            }

            override suspend fun close(token: String, taskId: Long) {
                closed += taskId
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals(1, api.closed.size)
        assertEquals(432, api.closed.single())
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
        val api = object: TodoistApi by ApiStub {
            val closed = mutableListOf<Long>()
            val created = mutableListOf<TaskParameters>()

            override suspend fun create(token: String, task: TaskParameters): Task {
                created += task
                return ApiStub.create(token, task)
            }

            override suspend fun getTasks(token: String, projectId: Long?, labelId: Long?): List<Task> {
                return listOf(Task(
                    id = 432,
                    content = "Test Task (id: fake-sensor)",
                    completed = false,
                ))
            }

            override suspend fun close(token: String, taskId: Long) {
                closed += taskId
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals(0, api.closed.size)
        assertEquals(0, api.created.size)
    }

    @Test
    fun notClosedWithoutHistory() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(time.instant)
        }
        val api = object: TodoistApi by ApiStub {
            val closed = mutableListOf<Long>()
            override suspend fun getTasks(token: String, projectId: Long?, labelId: Long?): List<Task> {
                return listOf(Task(
                    id = 432,
                    content = "Test Task (id: fake-sensor)",
                    completed = false,
                ))
            }

            override suspend fun close(token: String, taskId: Long) {
                closed += taskId
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals(0, api.closed.size)
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
        val api = object: TodoistApi by ApiStub {
            val closed = mutableListOf<Long>()
            override suspend fun getTasks(token: String, projectId: Long?, labelId: Long?): List<Task> {
                return listOf(Task(
                    id = 432,
                    content = "Test Task (id: fake-sensor)",
                    completed = false,
                ))
            }

            override suspend fun close(token: String, taskId: Long) {
                closed += taskId
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(client, api).runCron(time)

        assertEquals(0, api.closed.size)
    }
}


private object ApiStub: TodoistApi {
    override suspend fun getTasks(token: String, projectId: Long?, labelId: Long?): List<Task> {
        return emptyList()
    }

    override suspend fun create(token: String, task: TaskParameters): Task {
        return Task(
            id = 123,
            projectId = task.projectId,
            content = task.content,
            completed = false,
        )
    }

    override suspend fun close(token: String, taskId: Long) {}
}
