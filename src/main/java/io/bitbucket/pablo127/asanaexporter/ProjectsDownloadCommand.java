package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.Project;
import io.bitbucket.pablo127.asanaexporter.model.ProjectRequest;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ProjectsDownloadCommand {

    private static final Logger logger = LoggerFactory.getLogger(ProjectsDownloadCommand.class);

    private static final Map<String, String> ASANA_OLD_TO_NEW_COLORS_MAPPING = Map.ofEntries(
            Map.entry("none", "none"),
            Map.entry("red", "dark-red"),
            Map.entry("orange", "dark-orange"),
            Map.entry("yellow-orange", "light-orange"),
            Map.entry("yellow", "light-yellow"),
            Map.entry("yellow-green", "light-green"),
            Map.entry("green", "dark-green"),
            Map.entry("blue-green", "light-teal"),
            Map.entry("aqua", "dark-teal"),
            Map.entry("blue", "light-blue"),
            Map.entry("indigo", "dark-purple"),
            Map.entry("purple", "light-purple"),
            Map.entry("magenta", "light-pink"),
            Map.entry("hot-pink", "dark-pink"),
            Map.entry("pink", "light-red"),
            Map.entry("cool-gray", "light-warm-gray"));

    private final String workspaceId;

    public ProjectsDownloadCommand(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    DownloadedProjects run() {
        try {
            Requester<ProjectRequest> requester = new Requester<>(ProjectRequest.class);
            List<Project> projects = requester.requestGet(new UriBuilder().findProjects(workspaceId))
                    .getData();

            fixColors(projects);

            Map<String, String> projectIdToProjectNameMap = projects.stream()
                    .collect(Collectors.toUnmodifiableMap(Project::getGid, Project::getName));
            Map<String, Project> projectIdToProjectMap = projects.stream()
                    .collect(Collectors.toUnmodifiableMap(Project::getGid, project -> project));
            logger.info("Downloaded {} projects.", projects.size());

            return new DownloadedProjects(projectIdToProjectNameMap, projectIdToProjectMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fixColors(List<Project> projects) {
        for (Project project : projects) {
            String color = project.getColor();
            if (color != null) {
                String newColor = ASANA_OLD_TO_NEW_COLORS_MAPPING.getOrDefault(color, color);
                project.setColor(newColor);
            }
        }
    }

    @Data
    public static final class DownloadedProjects {
        private final Map<String, String> projectIdToProjectNameMap;
        private final Map<String, Project> projectIdToProjectMap;
    }
}
