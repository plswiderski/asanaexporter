package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.Project;
import io.bitbucket.pablo127.asanaexporter.model.ProjectRequest;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectsDownloadCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProjectsDownloadCommand.class);

    private final String workspaceId;

    @Getter
    private Map<String, String> projectIdToProjectNameMap = new HashMap<>();

    public ProjectsDownloadCommand(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public void run() {
        try {
            Requester<ProjectRequest> requester = new Requester<>(ProjectRequest.class);
            List<Project> projects = requester.request(new UriBuilder().findProjects(workspaceId))
                    .getData();

            projects.forEach(project -> projectIdToProjectNameMap.put(project.getGid(), project.getName()));
            logger.info("Downloaded {} projects.", projects.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
