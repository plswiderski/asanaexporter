package io.bitbucket.pablo127.asanaexporter.connector.user_task_list;

import io.bitbucket.pablo127.asanaexporter.Requester;

import java.io.IOException;
import java.net.URL;

final class UserTaskListServiceImpl implements UserTaskListService {
    @Override
    public UserTaskListTO getUserTaskList(String userId, String workspaceId) throws IOException {
        Requester<UserTaskListResponse> requester = new Requester<>(UserTaskListResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/users/" + userId + "/user_task_list?workspace=" + workspaceId);

        UserTaskListResponse response = requester.requestGet(() -> url);
        return response.getData();
    }
}
