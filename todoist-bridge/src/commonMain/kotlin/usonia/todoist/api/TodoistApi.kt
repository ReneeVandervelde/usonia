package usonia.todoist.api

internal interface TodoistApi {
    suspend fun getTasks(
        token: String,
        projectId: Long? = null,
        labelId: Long? = null,
    ): List<Task>

    suspend fun create(
        token: String,
        task: TaskParameters
    ): Task

    suspend fun close(
        token: String,
        taskId: Long,
    )
}
