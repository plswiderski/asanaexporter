package io.bitbucket.pablo127.asanaexporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.Recurrence;
import io.bitbucket.pablo127.asanaexporter.model.TaskAttachment;
import io.bitbucket.pablo127.asanaexporter.model.TaskMembership;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortAssignee;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortProject;
import io.bitbucket.pablo127.asanaexporter.model.user.UserData;
import io.bitbucket.pablo127.asanaexporter.util.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
final class CsvReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CsvReportGenerator.class);

    private final Set<TaskShort> tasks;
    private final Map<String, String> projectIdToProjectNameMap;
    private final List<UserData> users;

    void generateCsv() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id;createdAt;completedAt;dueOn;modifiedAt;name;assignee;notes;" +
                "projects;parentTask;recurrence;sections;attachments");

        for (TaskShort task : tasks) {
            lines.add(
                    String.join(";", fixNewLines(task.getGid(), task.getCreatedAt(),
                            task.getCompletedAt(), task.getDueOn(), task.getModifiedAt(), task.getName(),
                            getAssigneeName(task.getAssignee()), task.getNotes(),
                            getProjectNames(task.getProjects()),
                            getParent(task.getParent()),
                            getRecurrence(task.getRecurrence()),
                            getSections(task.getMemberships()),
                            getAttachments(task.getAttachments()))));
        }
        Files.write(Main.RESULT_FILE.toPath(), lines, StandardCharsets.UTF_8);
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

    private String getAttachments(List<TaskAttachment> attachments) throws JsonProcessingException {
        if (CollectionUtils.isEmpty(attachments)) {
            return "";
        }

        return JsonMapper.INSTANCE.writeValueAsString(attachments);
    }

    private String getRecurrence(Recurrence recurrence) throws JsonProcessingException {
        if (recurrence == null || recurrence.getData() == null) {
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

    private String[] fixNewLines(String... texts) {
        List<String> fixedTexts = new ArrayList<>();
        for (String text : texts)
            fixedTexts.add(text == null
                    ? ""
                    : StringEscapeUtils.escapeCsv(
                    text.replace('\n', ' ')
                            .replace('\r', ' ')
                            .replace(';', ' ')));
        return fixedTexts.toArray(new String[0]);
    }

    private String getProjectNames(List<TaskShortProject> projects) {
        if (projects.isEmpty())
            return "";

        return projects.stream()
                .map(taskShortProject -> {
                    String projectName = projectIdToProjectNameMap.get(taskShortProject.getGid());
                    if (projectName == null) {
                        throw new RuntimeException("ProjectId " + taskShortProject.getGid() + " was not " +
                                "recognized as an ID from user's projects.");
                    }
                    return projectName;
                })
                .collect(Collectors.joining(", "));
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
