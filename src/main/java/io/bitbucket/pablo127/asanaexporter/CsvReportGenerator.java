package io.bitbucket.pablo127.asanaexporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.Recurrence;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortAssignee;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortProject;
import io.bitbucket.pablo127.asanaexporter.model.user.UserData;
import io.bitbucket.pablo127.asanaexporter.util.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

    private static final File RESULT_FILE = new File("asanaTasks.csv");

    private final Set<TaskShort> tasks;
    private final Map<String, String> projectIdToProjectNameMap;
    private final List<UserData> users;

    void generateCsv() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id;createdAt;completedAt;dueOn;modifiedAt;name;assignee;notes;" +
                "projects;parentTask;recurrence");

        for (TaskShort task : tasks) {
            lines.add(
                    String.join(";", fixNewLines(task.getGid(), task.getCreatedAt(),
                            task.getCompletedAt(), task.getDueOn(), task.getModifiedAt(), task.getName(),
                            getAssigneeName(task.getAssignee()), task.getNotes(),
                            getProjectNames(task.getProjects()),
                            getTaskName(task.getParent()),
                            getRecurrence(task.getRecurrence()))));
        }
        Files.write(RESULT_FILE.toPath(), lines, StandardCharsets.UTF_8);
    }

    private String getRecurrence(Recurrence recurrence) throws JsonProcessingException {
        return JsonMapper.INSTANCE.writeValueAsString(recurrence);
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

        if (!first.isPresent()) {
            logger.error("Could not find subtask with id " + parent.getGid());
            return parent.getGid();
        }
        return first.get()
                .getName();
    }
}
