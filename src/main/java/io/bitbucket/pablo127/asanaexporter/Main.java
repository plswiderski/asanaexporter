package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortAssignee;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortProject;
import io.bitbucket.pablo127.asanaexporter.model.Tasks;
import io.bitbucket.pablo127.asanaexporter.model.user.UserData;
import io.bitbucket.pablo127.asanaexporter.util.SleepUtil;
import lombok.Getter;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static String personalAccessToken;
    private static String workspaceName;

    private final ExecutorService executorService;

    @Getter
    private final Set<TaskShort> tasks = ConcurrentHashMap.newKeySet();

    private final UserDownloadCommand userDownloadCommand;
    private final ProjectsDownloadCommand projectsDownloadCommand;

    public static void main(String[] args) {
        setDefaultUncaughtExceptionHandler();
        personalAccessToken = args[0];
        workspaceName = args.length < 2 ? "Personal Projects" : args[1];

        long startTimestamp = System.currentTimeMillis();
        try {
            ExecutorService executorService = createExecutorService();
            Main main = new Main(executorService);

            logger.info("Start importing tasks.");
            main.importTasks();

            waitToTheEndOfImport(executorService);

            logger.info("Imported " + main.getTasks().size() + " tasks in "
                    + (System.currentTimeMillis() - startTimestamp) / 1000 + " s.");

            logger.info("Start generate csv.");
            main.generateCsv();
            logger.info("Csv file is generated.");
        } catch (Exception e) {
            logger.error("Error occurred while running AsanaExporter.", e);
        }
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

    private void generateCsv() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id;createdAt;completedAt;dueOn;modifiedAt;name;assignee;notes;" +
                "projects;parentTask");

        for (TaskShort task : tasks) {
            lines.add(
                    String.join(";", fixNewLines(task.getGid(), task.getCreatedAt(),
                            task.getCompletedAt(), task.getDueOn(), task.getModifiedAt(), task.getName(),
                            getAssigneeName(task.getAssignee()), task.getNotes(),
                            getProjectNames(task.getProjects()),
                            getTaskName(task.getParent()))));
        }
        Files.write(new File("asanaTasks.csv").toPath(), lines, StandardCharsets.UTF_8);
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

    private String getProjectNames(List<TaskShortProject> projects) {
        if (projects.isEmpty())
            return "";

        return projects.stream()
                .map(taskShortProject -> {
                    String projectName = projectsDownloadCommand.getProjectIdToProjectNameMap()
                            .get(taskShortProject.getGid());
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
            return userDownloadCommand.getUsers()
                    .stream()
                    .filter(userData -> assignee.getGid().equals(userData.getGid()))
                    .map(UserData::getName)
                    .findFirst()
                    .orElse("");
        }
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
                                .findTasks(userDownloadCommand.getWorkspaceId(), userDownloadCommand.getUserId()),
                        shutdownCounter);

                for (String projectId : projectsDownloadCommand.getProjectIdToProjectNameMap().keySet()) {
                    shutdownCounter.incrementAndGet();
                    startGettingTasks(new UriBuilder().findTasksByProject(projectId), shutdownCounter);
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
        Tasks tasks = requester.request(uriBuilder);

        executorService.submit(new NextTasksDownloadCommand(executorService, tasks.getNextPage(), this.tasks,
                shutdownCounter));
        this.tasks.addAll(tasks.getData());
    }

    private static void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, e) -> logger.error("Uncaught exception occurred in thread " + thread.getName(), e));
    }
}
