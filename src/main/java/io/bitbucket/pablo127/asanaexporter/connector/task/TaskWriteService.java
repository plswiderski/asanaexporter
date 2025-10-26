package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.model.TaskShort;

import java.io.IOException;

interface TaskWriteService {

    TaskShort createTask(TaskToCreateTO task) throws IOException;

    TaskShort createSubtask(String parentTaskId, TaskToCreateTO subtask) throws IOException;

    TaskShort updateTask(String taskGid, TaskToCreateTO task) throws IOException;
}
