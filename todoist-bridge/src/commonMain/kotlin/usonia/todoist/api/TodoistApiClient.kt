package usonia.todoist.api

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal class TodoistApiClient: TodoistApi {
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
    }
    private val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
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
        }
    }

    override suspend fun create(
        token: String,
        task: TaskParameters,
    ): Task {
        return httpClient.post("https://api.todoist.com/rest/v1/tasks") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            body = task
        }
    }

    override suspend fun close(token: String, taskId: Long) {
        httpClient.post<HttpResponse>("https://api.todoist.com/rest/v1/tasks/$taskId/close") {
            header("Authorization", "Bearer $token")
        }
    }
}
