package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.Requester;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URL;

@RequiredArgsConstructor
final class TaskWriteServiceImpl implements TaskWriteService {

    @Override
    public TaskShort createTask(TaskToCreateTO task) throws IOException {
        Requester<CreateTaskResponse> requester = new Requester<>(CreateTaskResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/tasks");

        CreateTaskRequest body = new CreateTaskRequest(task);
        CreateTaskResponse response = requester.requestPost(() -> url, body);
        return response.getData();
    }

    @Override
    public TaskShort createSubtask(String parentTaskId, TaskToCreateTO subtask) throws IOException {
        Requester<CreateTaskResponse> requester = new Requester<>(CreateTaskResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/tasks/" + parentTaskId + "/subtasks");

        CreateTaskRequest body = new CreateTaskRequest(subtask);
        CreateTaskResponse response = requester.requestPost(() -> url, body);
        return response.getData();
    }

    @Override
    public TaskShort updateTask(String taskGid, TaskToCreateTO task) throws IOException {
        task.setProjects(null);

        Requester<CreateTaskResponse> requester = new Requester<>(CreateTaskResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/tasks/" + taskGid);

        UpdateTaskRequest body = new UpdateTaskRequest(task);
        CreateTaskResponse response = requester.requestPut(() -> url, body);
        return response.getData();
    }
}
