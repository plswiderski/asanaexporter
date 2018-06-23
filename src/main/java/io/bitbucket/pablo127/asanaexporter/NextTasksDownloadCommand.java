package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.NextPage;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

class NextTasksDownloadCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NextTasksDownloadCommand.class);

    private final Set<TaskShort> tasks;
    private final AtomicInteger shutdownCounter;
    private ExecutorService executorService;
    private NextPage nextPage;

    NextTasksDownloadCommand(ExecutorService executorService, NextPage nextPage, Set<TaskShort> tasks,
                             AtomicInteger shutdownCounter) {
        this.executorService = executorService;
        this.nextPage = nextPage;
        this.tasks = tasks;
        this.shutdownCounter = shutdownCounter;
    }

    @Override
    public void run() {
        if (nextPage == null) {
            if (shutdownCounter.decrementAndGet() == 0)
                executorService.shutdown();
            return;
        }

        try {
            UriBuilder uriBuilder = new UriBuilder().uri(nextPage.getUri());
            executorService.submit(() -> {
                try {
                    Tasks tasks = new Requester<>(Tasks.class).request(uriBuilder);

                    executorService.submit(new NextTasksDownloadCommand(executorService, tasks.getNextPage(),
                            this.tasks, shutdownCounter));
                    this.tasks.addAll(tasks.getData());
                } catch (IOException e) {
                    logger.error("Unexpected exception occurred.", e);
                }
            });
        } catch (Exception e) {
            logger.error("Unexpected exception.", e);
        }
    }
}