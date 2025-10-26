package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.Tasks;
import io.bitbucket.pablo127.asanaexporter.util.SleepUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class TasksImporter {
    private static final Logger logger = LoggerFactory.getLogger(TasksImporter.class);

    private final String workspaceId;
    private final String userId;
    private final Map<String, String> projectIdToProjectNameMap;

    private final String modifiedSince;

    private final Set<TaskShort> tasks = ConcurrentHashMap.newKeySet();

    public Set<TaskShort> importTasks() {
        AtomicInteger shutdownCounter = new AtomicInteger(0);

        ExecutorService executorService = createExecutorService();
        executorService.submit(() -> {
            try {
                shutdownCounter.incrementAndGet();
                shutdownCounter.incrementAndGet();
                startGettingTasks(new UriBuilder().findTasks(workspaceId, userId, modifiedSince),
                        executorService, shutdownCounter);

                for (String projectId : projectIdToProjectNameMap.keySet()) {
                    shutdownCounter.incrementAndGet();
                    startGettingTasks(new UriBuilder().findTasksByProject(projectId, modifiedSince), executorService, shutdownCounter);
                }
                if (shutdownCounter.decrementAndGet() == 0)
                    executorService.shutdown();
            } catch (IOException e) {
                logger.error("Unexpected exception occurred.", e);
            }
        });

        waitToTheEndOfImport(executorService);
        return tasks;
    }

    private void startGettingTasks(UriBuilder uriBuilder, ExecutorService executorService, AtomicInteger shutdownCounter) throws IOException {
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
            startGettingTasks(new UriBuilder().findSubtasks(task.getGid(), modifiedSince), executorService, shutdownCounter);
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
}
