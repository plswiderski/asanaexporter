package io.bitbucket.pablo127.asanaexporter;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.Project;
import io.bitbucket.pablo127.asanaexporter.model.Recurrence;
import io.bitbucket.pablo127.asanaexporter.model.TaskAssigneeSection;
import io.bitbucket.pablo127.asanaexporter.model.TaskAttachment;
import io.bitbucket.pablo127.asanaexporter.model.TaskMembership;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortAssignee;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortProject;
import io.bitbucket.pablo127.asanaexporter.model.user.UserData;
import io.bitbucket.pablo127.asanaexporter.util.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
final class CsvReportReader {

    private final List<UserData> users;
    private final Function<Project, String> projectToProjectIdProvider;

    List<TaskShort> readTasks() throws IOException  {
        final List<TaskShort> taskShorts = new ArrayList<>();

        try (CSVReader csvReader = createCsvReader()) {
            boolean header = false;
            for (String[] columns : csvReader) {
                if (!header) {
                    header = true;
                    continue;
                }

                TaskShort task = TaskShort.builder()
                        .gid(columns[0])
                        .createdAt(columns[1])
                        .completedAt(columns[2])
                        .dueOn(columns[3])
                        .modifiedAt(columns[4])
                        .name(columns[5].trim())
                        .assignee(getAssignee(columns[6]))
                        .notes(columns[7])
                        .projects(getProjects(columns[8]))
                        .recurrence(getRecurrence(columns[10]))
                        .memberships(getSections(columns[11]))
                        .attachments(getAttachments(columns[12]))
                        .parent(getParent(columns[9]))
                        .assigneeSection(getAssigneeSection(columns[13]))
                        .build();

                taskShorts.add(task);
            }
        }

        return taskShorts;
    }

    private TaskAssigneeSection getAssigneeSection(String text) throws IOException {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        return JsonMapper.INSTANCE.readValue(text, TaskAssigneeSection.class);
    }

    private Parent getParent(String text) throws IOException {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        return JsonMapper.INSTANCE.readValue(text, Parent.class);
    }

    private List<TaskMembership> getSections(String text) throws IOException {
        if (StringUtils.isEmpty(text)) {
            return List.of();
        }

        final TypeFactory typeFactory = JsonMapper.INSTANCE.getTypeFactory();
        CollectionType javaType = typeFactory.constructCollectionType(List.class, TaskMembership.class);

        return JsonMapper.INSTANCE.readValue(text, javaType);
    }

    private List<TaskAttachment> getAttachments(String text) throws IOException {
        if (StringUtils.isEmpty(text)) {
            return List.of();
        }

        final TypeFactory typeFactory = JsonMapper.INSTANCE.getTypeFactory();
        CollectionType javaType = typeFactory.constructCollectionType(List.class, TaskAttachment.class);

        return JsonMapper.INSTANCE.readValue(text, javaType);
    }

    private Recurrence getRecurrence(String text) throws IOException {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        return JsonMapper.INSTANCE.readValue(text, Recurrence.class);
    }

    private List<TaskShortProject> getProjects(String text) throws IOException {
        if (StringUtils.isEmpty(text)) {
            return List.of();
        }

        CollectionType projectListType = JsonMapper.INSTANCE.getTypeFactory().constructCollectionType(List.class, Project.class);
        List<Project> projects = JsonMapper.INSTANCE.readValue(text, projectListType);

        return projects.stream()
                .map(projectToProjectIdProvider)
                .map(projectId ->  TaskShortProject.builder()
                        .gid(projectId)
                        .build())
                .collect(Collectors.toUnmodifiableList());
    }

    private TaskShortAssignee getAssignee(String column) {
        if (StringUtils.isEmpty(column)) {
            return null;
        }

        return users.stream()
                .filter(userData -> userData.getName().equals(column))
                .findFirst()
                .map(userData -> TaskShortAssignee.builder()
                        .gid(userData.getGid())
                        .build())
                .orElseThrow(() -> new RuntimeException(String.format("Could not find assignee for '%s'", column)));
    }

    private CSVReader createCsvReader() throws IOException {
        CSVParser csvParser = new CSVParserBuilder()
                .withSeparator(';')
                .build();

        Reader reader = new InputStreamReader(Files.newInputStream(Main.RESULT_FILE.toPath()));
        return new CSVReaderBuilder(reader)
                .withCSVParser(csvParser)
                .build();
    }
}
