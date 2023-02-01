package usonia.todoist.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import usonia.client.ktor.PlatformEngine
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

internal class TodoistApiClient: TodoistApi {
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
    }

    private val httpClient = HttpClient(PlatformEngine) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20.seconds.inWholeMilliseconds
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun getTasks(
        token: String,
        projectId: String?,
        label: String?,
    ): List<Task> {
        return httpClient.get("https://api.todoist.com/rest/v2/tasks") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            if (projectId != null) parameter("project_id", projectId)
            if (label != null) parameter("label", label)
        }.body()
    }

    override suspend fun create(
        token: String,
        task: TaskParameters,
    ): Task {
        return httpClient.post("https://api.todoist.com/rest/v2/tasks") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(task)
        }.body()
    }

    override suspend fun close(token: String, taskId: String) {
        httpClient.post("https://api.todoist.com/rest/v2/tasks/$taskId/close") {
            header("Authorization", "Bearer $token")
        }
    }
}
