package io.bitbucket.pablo127.asanaexporter.connector.project;

import io.bitbucket.pablo127.asanaexporter.Requester;
import io.bitbucket.pablo127.asanaexporter.model.Project;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
final class ProjectWriteServiceImpl implements ProjectWriteService {

    private final String workspaceId;

    @Override
    public Project createProject(Project project) throws IOException {
        Requester<CreateProjectResponse> requester = new Requester<>(CreateProjectResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/projects");

        String notes = project.getNotes() + "\n" + prepareNotesImportInformation(project);
        CreateProjectRequest body = new CreateProjectRequest(CreateProjectToCreate.builder()
                .name(project.getName())
                .notes(notes)
                .color(project.getColor())
                .dueOn(project.getDueOn())
                .archived(project.getArchived())
                .workspaceGid(workspaceId)
                .build());

        CreateProjectResponse response = requester.requestPost(() -> url, body);
        return response.getData();
    }

    private String prepareNotesImportInformation(Project project) {
        List<ImmutablePair<String, String>> properties = List.of(
                ImmutablePair.of("ProjectId", project.getGid()),
                ImmutablePair.of("Created at", project.getCreatedAt()),
                ImmutablePair.of("Modified at", project.getModifiedAt()),
                ImmutablePair.of("Icon", project.getIcon()));

        final String propertiesString = properties.stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        return "---\n" + "Project restored from backup. Original properties:\n" + propertiesString;
    }
}
