package usonia.todoist.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import usonia.client.ktor.PlatformEngine
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@OptIn(ExperimentalTime::class)
internal class TodoistApiClient: TodoistApi {
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
    }

    private val httpClient = HttpClient(PlatformEngine) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20.seconds.toLongMilliseconds()
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun getTasks(
        token: String,
        projectId: Long?,
        labelId: Long?,
    ): List<Task> {
        return httpClient.get("https://api.todoist.com/rest/v1/tasks") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            if (projectId != null) parameter("project_id", projectId)
            if (labelId != null) parameter("label_id", labelId)
        }.body()
    }

    override suspend fun create(
        token: String,
        task: TaskParameters,
    ): Task {
        return httpClient.post("https://api.todoist.com/rest/v1/tasks") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(task)
        }.body()
    }

    override suspend fun close(token: String, taskId: Long) {
        httpClient.post("https://api.todoist.com/rest/v1/tasks/$taskId/close") {
            header("Authorization", "Bearer $token")
        }
    }
}
