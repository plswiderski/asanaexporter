package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.Tasks;
import io.bitbucket.pablo127.asanaexporter.util.SleepUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final File LAST_MODIFICATION_FILE = new File("lastModification.txt");

    static String personalAccessToken;
    private static String workspaceName;
    private static String modifiedSince;

    private final ExecutorService executorService;

    @Getter
    private final Set<TaskShort> tasks = ConcurrentHashMap.newKeySet();

    private final UserDownloadCommand userDownloadCommand;
    private final ProjectsDownloadCommand projectsDownloadCommand;

    public static void main(String[] args) {
        setDefaultUncaughtExceptionHandler();
        personalAccessToken = args[0];
        workspaceName = args.length < 2 ? "Personal Projects" : args[1];
        modifiedSince = loadModifiedSince();

        long startTimestamp = System.currentTimeMillis();
        try {
            ExecutorService executorService = createExecutorService();
            Main main = new Main(executorService);

            logger.info("Start importing tasks" +
                    (modifiedSince != null
                            ? " from last modification date " + modifiedSince +"."
                            : " from the beginning."));
            main.importTasks();

            waitToTheEndOfImport(executorService);

            main.removeTaskWithModifiedSinceDateTime();

            logger.info("Imported " + main.getTasks().size() + " tasks in "
                    + (System.currentTimeMillis() - startTimestamp) / 1000 + " s.");

            main.saveAttachments();

            logger.info("Start generate csv.");
            main.generateCsv();
            logger.info("Csv file is generated.");

            logger.info("Start writing last modification dateTime.");
            main.writeLastModificationDateTime();
            logger.info("Last modification dateTime is written.");

            System.exit(0);
        } catch (Exception e) {
            logger.error("Error occurred while running AsanaExporter.", e);
        }
    }

    private void saveAttachments() throws IOException {
        AttachmentsSaver attachmentSaver = new AttachmentsSaver(tasks);
        attachmentSaver.save();
    }

    private void generateCsv() throws IOException {
        final CsvReportGenerator csvReportGenerator = new CsvReportGenerator(tasks, projectsDownloadCommand.getProjectIdToProjectNameMap(),
                userDownloadCommand.getUsers());

        csvReportGenerator.generateCsv();
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

    private static void waitToTheEndOfImport(ExecutorService executorService) {
        while (!executorService.isShutdown())
            SleepUtil.sleep(500);

        while (!executorService.isTerminated())
            SleepUtil.sleep(500);
    }

    private static ExecutorService createExecutorService() {
        int numberOfCores = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(numberOfCores,
                numberOfCores * 10,
                5000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>());
    }

    private void removeTaskWithModifiedSinceDateTime() {
        if (modifiedSince != null) {
            Set<TaskShort> tasksToRemove = tasks.stream()
                    .filter(taskShort -> taskShort.getModifiedAt().equals(modifiedSince))
                    .collect(Collectors.toSet());

            tasks.removeAll(tasksToRemove);
        }
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

    private Main(ExecutorService executorService) {
        this.executorService = executorService;
        userDownloadCommand = new UserDownloadCommand(workspaceName);
        userDownloadCommand.run();

        projectsDownloadCommand = new ProjectsDownloadCommand(userDownloadCommand.getWorkspaceId());
        projectsDownloadCommand.run();
    }

    private void importTasks() {
        AtomicInteger shutdownCounter = new AtomicInteger(0);

        executorService.submit(() -> {
            try {
                shutdownCounter.incrementAndGet();
                shutdownCounter.incrementAndGet();
                startGettingTasks(new UriBuilder()
                                .findTasks(userDownloadCommand.getWorkspaceId(), userDownloadCommand.getUserId(), modifiedSince),
                        shutdownCounter);

                for (String projectId : projectsDownloadCommand.getProjectIdToProjectNameMap().keySet()) {
                    shutdownCounter.incrementAndGet();
                    startGettingTasks(new UriBuilder().findTasksByProject(projectId, modifiedSince), shutdownCounter);
                }
                if (shutdownCounter.decrementAndGet() == 0)
                    executorService.shutdown();
            } catch (IOException e) {
                logger.error("Unexpected exception occurred.", e);
            }
        });
    }

    private void startGettingTasks(UriBuilder uriBuilder, AtomicInteger shutdownCounter) throws IOException {
        Requester<Tasks> requester = new Requester<>(Tasks.class);
        Tasks tasks = requester.requestGet(uriBuilder);

        executorService.submit(new NextTasksDownloadCommand(executorService, tasks.getNextPage(), this.tasks,
                shutdownCounter));
        this.tasks.addAll(tasks.getData());

        List<TaskShort> tasksWithSubtasks = tasks.getData()
                .stream()
                .filter(taskShort -> taskShort.getSubtasksCount() > 0)
                .collect(Collectors.toUnmodifiableList());

        for (TaskShort task : tasksWithSubtasks) {
            startGettingTasks(new UriBuilder().findSubtasks(task.getGid(), modifiedSince), shutdownCounter);
        }
    }

    private static void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, e) -> logger.error("Uncaught exception occurred in thread " + thread.getName(), e));
    }
}
