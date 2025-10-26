package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
public final class CreateTasksResult {
    private final List<TaskShort> createdTasks;
    private final Map<String, String> oldToNewTaskId;
}
