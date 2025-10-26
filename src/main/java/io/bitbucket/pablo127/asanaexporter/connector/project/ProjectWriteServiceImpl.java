package io.bitbucket.pablo127.asanaexporter.connector.project;

import io.bitbucket.pablo127.asanaexporter.Requester;
import io.bitbucket.pablo127.asanaexporter.model.Project;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URL;

@RequiredArgsConstructor
final class ProjectWriteServiceImpl implements ProjectWriteService {

    private final String workspaceId;

    @Override
    public Project createProject(String projectName) throws IOException {
        Requester<CreateProjectResponse> requester = new Requester<>(CreateProjectResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/projects");

        CreateProjectRequest body = new CreateProjectRequest(CreateProjectToCreate.builder()
                .name(projectName)
                .workspaceGid(workspaceId)
                .build());

        CreateProjectResponse response = requester.requestPost(() -> url, body);
        return response.getData();
    }
}
