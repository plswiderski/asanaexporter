package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.Project;
import io.bitbucket.pablo127.asanaexporter.model.ProjectRequest;
import lombok.Getter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectsDownloadCommand implements Runnable {

    @Getter
    private Map<String, String> projectIdToProjectNameMap = new HashMap<>();

    @Override
    public void run() {
        try {
            Requester<ProjectRequest> requester = new Requester<>(ProjectRequest.class);
            List<Project> projects = requester.request(new UriBuilder().uri("https://app.asana.com/api/1.0/projects"))
                    .getData();

            projects.forEach(project -> projectIdToProjectNameMap.put(project.getGid(), project.getName()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
