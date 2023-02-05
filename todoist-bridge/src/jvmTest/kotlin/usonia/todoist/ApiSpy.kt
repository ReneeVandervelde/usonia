package usonia.todoist

import usonia.todoist.api.Task
import usonia.todoist.api.TaskCreateParameters
import usonia.todoist.api.TaskUpdateParameters
import usonia.todoist.api.TodoistApi

internal open class ApiSpy: TodoistApi by ApiStub {
    val created = mutableListOf<TaskCreateParameters>()
    val closed = mutableListOf<String>()
    val updated = mutableListOf<Pair<String, TaskUpdateParameters>>()
    override suspend fun create(token: String, task: TaskCreateParameters): Task {
        created += task

        return ApiStub.StubTask
    }

    override suspend fun close(token: String, taskId: String) {
        closed += taskId
    }

    override suspend fun update(token: String, taskId: String, paramters: TaskUpdateParameters): Task {
        updated += taskId to paramters

        return ApiStub.StubTask
    }

}
