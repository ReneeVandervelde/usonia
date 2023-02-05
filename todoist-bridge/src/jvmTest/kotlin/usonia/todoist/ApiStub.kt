package usonia.todoist

import usonia.todoist.api.Task
import usonia.todoist.api.TaskCreateParameters
import usonia.todoist.api.TaskUpdateParameters
import usonia.todoist.api.TodoistApi

internal object ApiStub: TodoistApi {
    val StubTask = Task(
        id = "123",
        projectId = "test-project",
        content = "test-content",
        completed = false,
    )
    override suspend fun getTasks(token: String, projectId: String?, labelId: String?): List<Task> {
        return emptyList()
    }

    override suspend fun create(token: String, task: TaskCreateParameters): Task {
        return StubTask
    }

    override suspend fun close(token: String, taskId: String) {}
    override suspend fun update(token: String, taskId: String, paramters: TaskUpdateParameters): Task {
        return StubTask
    }
}
