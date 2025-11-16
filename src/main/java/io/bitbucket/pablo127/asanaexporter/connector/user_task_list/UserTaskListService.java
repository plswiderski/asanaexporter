package io.bitbucket.pablo127.asanaexporter.connector.user_task_list;

import java.io.IOException;

public interface UserTaskListService {
    UserTaskListTO getUserTaskList(String userId, String workspaceId) throws IOException;
}
