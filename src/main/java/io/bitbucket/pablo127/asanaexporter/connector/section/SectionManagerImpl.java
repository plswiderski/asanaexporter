package io.bitbucket.pablo127.asanaexporter.connector.section;

import io.bitbucket.pablo127.asanaexporter.model.TaskMembership;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.util.ExecutorUtil;
import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
final class SectionManagerImpl implements SectionManager {
    private static final Logger logger = LoggerFactory.getLogger(SectionManagerImpl.class);
    private final SectionService sectionService;

    @Override
    public void addTasksToSections(List<TaskShort> originalTasks, Map<String, String> oldToNewTaskId) {
        Map<String, List<TaskMembership>> oldTaskIdToMemberships = originalTasks.stream()
                .filter(taskShort -> CollectionUtils.isNotEmpty(taskShort.getMemberships()))
                .collect(Collectors.toUnmodifiableMap(TaskShort::getGid, TaskShort::getMemberships));

        final ExecutorService executor = ExecutorUtil.createExecutorService();
        final AtomicInteger counter = new AtomicInteger(0);
        logger.info(String.format("Start adding tasks to sections. '%d' tasks to add to section.", oldTaskIdToMemberships.size()));
        try (ProgressBar progressBar = new ProgressBar("Adding tasks to sections", oldTaskIdToMemberships.size())) {
            for (String oldTaskId : oldTaskIdToMemberships.keySet()) {
                String newTaskId = oldToNewTaskId.get(oldTaskId);
                if (newTaskId != null) {
                    List<TaskMembership> memberships = oldTaskIdToMemberships.get(oldTaskId);

                    for (TaskMembership membership : memberships) {
                        executor.submit(() -> {
                            try {
                                sectionService.addTaskToSection(membership.getSection().getGid(), newTaskId);
                                progressBar.stepTo(counter.incrementAndGet());
                            } catch (Exception e) {
                                logger.error(String.format("Could not add task to section. oldTaskId: '%s', newTaskId: '%s', membership: '%s'",
                                        oldTaskId, newTaskId, membership), e);
                            }
                        });
                    }
                } else {
                    logger.error(String.format("Could not map oldTaskId '%s'", oldTaskId));
                }
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while adding tasks to sections.", e);
        } finally {
            logger.info(String.format("Correctly added '%d' tasks to sections.", counter.get()));
        }
    }
}
