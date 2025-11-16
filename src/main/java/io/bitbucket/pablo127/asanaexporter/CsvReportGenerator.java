package io.bitbucket.pablo127.asanaexporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
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
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class CsvReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CsvReportGenerator.class);

    private final Set<TaskShort> tasks;
    private final Map<String, Project> projectIdToProjectMap;
    private final List<UserData> users;

    void generateCsv() throws IOException {
        CSVParser csvParser = new CSVParserBuilder()
                .withSeparator(';')
                .build();

        try (ICSVWriter csvWriter = createCsvWriter(csvParser)) {
            final String[] headers = new String[] {"id", "createdAt", "completedAt", "dueOn", "modifiedAt", "name", "assignee", "notes",
                    "projects", "parentTask", "recurrence", "sections", "attachments", "assigneeSection"};

            csvWriter.writeNext(headers);

            for (TaskShort task : tasks) {
                final String[] taskLine = Stream.of(
                                task.getGid(), task.getCreatedAt(),
                                task.getCompletedAt(), task.getDueOn(), task.getModifiedAt(), task.getName(),
                                getAssigneeName(task.getAssignee()), task.getNotes(),
                                getProjects(task.getProjects()),
                                getParent(task.getParent()),
                                getRecurrence(task.getRecurrence()),
                                getSections(task.getMemberships()),
                                getAttachments(task.getAttachments()),
                                getAssigneeSection(task.getAssigneeSection()))
                        .map(s -> s == null ? "" : s)
                        .toArray(String[]::new);

                csvWriter.writeNext(taskLine);
            }
        }
    }

    private static ICSVWriter createCsvWriter(CSVParser csvParser) throws IOException {
        Writer writer = new OutputStreamWriter(Files.newOutputStream(Main.RESULT_FILE.toPath()));
        return new CSVWriterBuilder(writer)
                .withParser(csvParser)
                .build();
    }

    private String getSections(List<TaskMembership> memberships) {
        return Optional.ofNullable(memberships)
                .filter(taskMemberships -> !taskMemberships.isEmpty())
                .map(taskMemberships -> taskMemberships.stream()
                        .filter(taskMembership -> !"Untitled section".equals(taskMembership.getSection().getName()))
                        .collect(Collectors.toUnmodifiableList()))
                .map(taskMemberships -> {
                    try {
                        return JsonMapper.INSTANCE.writeValueAsString(taskMemberships);
                    } catch (JsonProcessingException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .orElse(null);
    }

    private String getAssigneeSection(TaskAssigneeSection assigneeSection) throws JsonProcessingException {
        if (assigneeSection == null) {
            return "";
        }

        return JsonMapper.INSTANCE.writeValueAsString(assigneeSection);
    }

    private String getAttachments(List<TaskAttachment> attachments) throws JsonProcessingException {
        if (CollectionUtils.isEmpty(attachments)) {
            return "";
        }

        return JsonMapper.INSTANCE.writeValueAsString(attachments);
    }

    private String getRecurrence(Recurrence recurrence) throws JsonProcessingException {
        if (recurrence == null) {
            return "";
        }

        return JsonMapper.INSTANCE.writeValueAsString(recurrence);
    }

    private String getParent(Parent parent) throws JsonProcessingException {
        if (parent == null) {
            return "";
        }

        return JsonMapper.INSTANCE.writeValueAsString(parent);
    }

    private String getProjects(List<TaskShortProject> projects) throws JsonProcessingException {
        if (projects.isEmpty())
            return "";

        final List<Project> projectsToSave = projects.stream()
                .map(taskShortProject -> {
                    Project project = projectIdToProjectMap.get(taskShortProject.getGid());
                    if (project == null) {
                        throw new RuntimeException("ProjectId " + taskShortProject.getGid() + " was not " +
                                "recognized as an ID from user's projects.");
                    }
                    return project;
                })
                .collect(Collectors.toUnmodifiableList());

        if (projectsToSave.isEmpty()) {
            return "";
        }

        return JsonMapper.INSTANCE.writeValueAsString(projectsToSave);
    }

    private String getAssigneeName(TaskShortAssignee assignee) {
        if (assignee == null || assignee.getGid() == null) {
            return "";
        } else {
            return users.stream()
                    .filter(userData -> assignee.getGid().equals(userData.getGid()))
                    .map(UserData::getName)
                    .findFirst()
                    .orElse("");
        }
    }

    private String getTaskName(Parent parent) {
        if (parent == null)
            return "";

        Optional<TaskShort> first = tasks.stream()
                .filter(taskShort -> taskShort.getGid().equals(parent.getGid()))
                .findFirst();

        if (first.isEmpty()) {
            logger.error("Could not find subtask with id " + parent.getGid());
            return parent.getGid();
        }
        return first.get()
                .getName();
    }
}
