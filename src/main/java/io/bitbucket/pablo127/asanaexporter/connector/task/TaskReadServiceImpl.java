package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.Requester;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.Tasks;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URL;
import java.util.List;

@RequiredArgsConstructor
final class TaskReadServiceImpl implements TaskReadService {

    private final String workspaceId;

    @Override
    public List<TaskShort> searchTasks(SearchTasksRequest request) throws IOException {
        Requester<Tasks> requester = new Requester<>(Tasks.class);
        URL url = new URL("https://app.asana.com/api/1.0/workspaces/" + workspaceId + "/tasks/search?" +
                "text=" + request.getText());
// TODO[PS] to jest platne, wiec musimy przeszukać calosc
        Tasks response = requester.requestGet(() -> url);
        return response.getData();// TODO[PS] pagination?
    }
    // TODO[PS]
}
