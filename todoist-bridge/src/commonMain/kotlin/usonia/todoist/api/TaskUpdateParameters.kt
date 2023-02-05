package usonia.todoist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TaskUpdateParameters(
    val content: String? = null,
    @SerialName("project_id")
    val projectId: String? = null,
    @SerialName("labels")
    val labels: List<String>? = null,
    @SerialName("due_string")
    val dueString: String? = null,
    @SerialName("description")
    val description: String? = null,
)
