package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.model.TaskShort;

import java.util.List;

public interface TaskWriteManager {
    CreateTasksResult createTasks(List<TaskShort> tasks) throws InterruptedException;
}
