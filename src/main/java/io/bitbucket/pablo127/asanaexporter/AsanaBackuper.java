package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.TaskAttachment;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class AsanaBackuper implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(AsanaBackuper.class);

    private static final File LAST_MODIFICATION_FILE = new File("lastModification.txt");

    private final UserDownloadCommand userDownloadCommand;
    private final ProjectsDownloadCommand.DownloadedProjects downloadedProjects;

    private final String modifiedSince;

    @Getter
    private Set<TaskShort> tasks;

    public AsanaBackuper(String workspaceName) {
        modifiedSince = loadModifiedSince();

        userDownloadCommand = new UserDownloadCommand(workspaceName);
        userDownloadCommand.run();

        ProjectsDownloadCommand projectsDownloadCommand = new ProjectsDownloadCommand(userDownloadCommand.getWorkspaceId());
        downloadedProjects = projectsDownloadCommand.run();
    }

    private static String loadModifiedSince() {
        try {
            final List<String> strings = Files.readAllLines(LAST_MODIFICATION_FILE.toPath(), StandardCharsets.UTF_8);
            return !strings.isEmpty() ? strings.get(0) : null;
        } catch (IOException e) {
            logger.warn("Could not load lastModification.txt. All tasks will be downloaded.");
        }
        return null;
    }

    @Override
    public void process() throws IOException {
        logger.info("Start importing tasks" +
                (modifiedSince != null
                        ? " from last modification date " + modifiedSince +"."
                        : " from the beginning."));
        long startTimestamp = System.currentTimeMillis();

        TasksImporter tasksImporter = new TasksImporter(userDownloadCommand.getWorkspaceId(), userDownloadCommand.getUserId(),
                downloadedProjects.getProjectIdToProjectNameMap(), modifiedSince);
        tasks = tasksImporter.importTasks();

        removeTaskWithModifiedSinceDateTime();
        removeExternalAttachments();

        logger.info("Imported " + tasks.size() + " tasks in "
                + (System.currentTimeMillis() - startTimestamp) / 1000 + " s.");

        saveAttachments();

        logger.info("Start generate csv.");
        generateCsv();
        logger.info("Csv file is generated.");

        logger.info("Start writing last modification dateTime.");
        writeLastModificationDateTime();
        logger.info("Last modification dateTime is written.");
    }

    private void saveAttachments() {
        AttachmentsSaver attachmentSaver = new AttachmentsSaver(tasks);
        attachmentSaver.save();
    }

    private void generateCsv() throws IOException {
        final CsvReportGenerator csvReportGenerator = new CsvReportGenerator(tasks, downloadedProjects.getProjectIdToProjectMap(),
                userDownloadCommand.getUsers());

        csvReportGenerator.generateCsv();
    }

    private void removeTaskWithModifiedSinceDateTime() {
        if (modifiedSince != null) {
            Set<TaskShort> tasksToRemove = tasks.stream()
                    .filter(taskShort -> taskShort.getModifiedAt().equals(modifiedSince))
                    .collect(Collectors.toSet());

            tasks.removeAll(tasksToRemove);
        }
    }

    private void removeExternalAttachments() {
        final List<TaskShort> tasksToReprocess = tasks.stream()
                .filter(taskShort -> CollectionUtils.emptyIfNull(taskShort.getAttachments())
                        .stream()
                        .anyMatch(taskAttachment -> "external".equals(taskAttachment.getResourceSubtype())))
                .collect(Collectors.toUnmodifiableList());

        tasks.removeAll(tasksToReprocess);

        tasksToReprocess.forEach(taskShort -> {
            List<TaskAttachment> newAttachments = taskShort.getAttachments().stream()
                    .filter(taskAttachment -> !"external".equals(taskAttachment.getResourceSubtype()))
                    .collect(Collectors.toUnmodifiableList());

            taskShort.setAttachments(newAttachments);
        });

        tasks.addAll(tasksToReprocess);
    }

    private void writeLastModificationDateTime() throws IOException {
        List<String> sortedModifiedAt = tasks.stream()
                .map(TaskShort::getModifiedAt)
                .sorted(String::compareTo)
                .collect(Collectors.toList());
        if (!sortedModifiedAt.isEmpty()) {
            String lastModificationDateTime = sortedModifiedAt.get(sortedModifiedAt.size() - 1);
            Files.write(LAST_MODIFICATION_FILE.toPath(), Collections.singletonList(lastModificationDateTime),
                    StandardCharsets.UTF_8);
        }
    }
}
