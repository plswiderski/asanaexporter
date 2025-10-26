package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.model.TaskShort;

import java.io.IOException;
import java.util.List;

public interface TaskReadService {
    List<TaskShort> searchTasks(SearchTasksRequest request) throws IOException;// TODO[PS] to remove the whole service
}
