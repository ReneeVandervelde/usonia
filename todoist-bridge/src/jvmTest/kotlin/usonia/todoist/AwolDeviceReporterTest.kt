package usonia.todoist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.core.state.EventAccessStub
import usonia.foundation.*
import usonia.kotlin.suspendedFlow
import usonia.server.DummyClient
import usonia.todoist.api.Task
import usonia.todoist.api.TaskParameters
import usonia.todoist.api.TodoistApi
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

@OptIn(ExperimentalTime::class)
class AwolDeviceReporterTest {
    val config = object: ConfigurationAccess {
        override val site: Flow<Site> = suspendedFlow(FakeSite.copy(
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
    val zone = TimeZone.UTC
    val now = Clock.System.now()
    val time = now.toLocalDateTime(zone)

    @Test
    fun correctParameters() = runBlockingTest {
        val events = EventAccessStub
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

        AwolDeviceReporter(client, api).run(time, zone)

        assertEquals("test-token", api.tokenUsed)
        assertEquals(666, api.projectUsed)
        assertEquals(420, api.labelUsed)
    }

    @Test
    fun noAwolDevices() = runBlockingTest {
        val events = object: EventAccess by EventAccessStub {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = now
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

        AwolDeviceReporter(client, api).run(time, zone)

        assertEquals(0, api.created.size)
    }

    @Test
    fun tasksCreated() = runBlockingTest {
        val events = object: EventAccess by EventAccessStub {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = now - 2.minutes
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

        AwolDeviceReporter(client, api).run(time, zone)

        assertEquals(1, api.created.size)
        val parameters = api.created.single()
        assertEquals("Replace Batteries in Fake Sensor (id: fake-sensor)", parameters.content)
        assertEquals(666L, parameters.projectId)
        assertEquals(listOf(420L), parameters.labels)
    }

    @Test
    fun notCreatedWithoutHistory() = runBlockingTest {
        val events = EventAccessStub
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

        AwolDeviceReporter(client, api).run(time, zone)

        assertEquals(0, api.created.size)
    }

    @Test
    fun taskClosed() = runBlockingTest {
        val events = object: EventAccess by EventAccessStub {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = now
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

        AwolDeviceReporter(client, api).run(time, zone)

        assertEquals(1, api.closed.size)
        assertEquals(432, api.closed.single())
    }

    @Test
    fun stillAwol() = runBlockingTest {
        val events = object: EventAccess by EventAccessStub {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = now - 2.minutes
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

        AwolDeviceReporter(client, api).run(time, zone)

        assertEquals(0, api.closed.size)
    }

    @Test
    fun notClosedWithoutHistory() = runBlockingTest {
        val events = EventAccessStub
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

        AwolDeviceReporter(client, api).run(time, zone)

        assertEquals(0, api.closed.size)
    }

    @Test
    fun noClosableTask() = runBlockingTest {
        val events = object: EventAccess by EventAccessStub {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = now
                    ) as T
                } else null
            }
        }
        val api = object: TodoistApi by ApiStub {
            val closed = mutableListOf<Long>()
            override suspend fun getTasks(token: String, projectId: Long?, labelId: Long?): List<Task> {
                return listOf(Task(
                    id = 432,
                    content = "Test Task",
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

        AwolDeviceReporter(client, api).run(time, zone)

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
