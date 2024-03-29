package usonia.todoist.api

internal interface TodoistApi {
    suspend fun getTasks(
        token: String,
        projectId: String? = null,
        label: String? = null,
    ): List<Task>

    suspend fun create(
        token: String,
        task: TaskCreateParameters,
    ): Task

    suspend fun close(
        token: String,
        taskId: String,
    )

    suspend fun update(
        token: String,
        taskId: String,
        paramters: TaskUpdateParameters,
    ): Task
}
