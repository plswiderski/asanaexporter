package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sort that Tasks without parent are first, then those with parents in order depending on parents dependency connections.
 * Dependency cycle is not allowed in input data.
 * Parent may not be available in the tasks.
 */
final class TasksToProcessSorter {

    public List<TaskShort> sort(List<TaskShort> taskShortList) {
        final Map<String, Integer> indexByTaskShortId = new LinkedHashMap<>(taskShortList.size());
        for (int i = 0; i < taskShortList.size(); i++) {
            int index = i + 1;
            final TaskShort taskShort = taskShortList.get(i);
            if (taskShort.getParent() == null) {
                index = 0;
            }
            indexByTaskShortId.put(taskShort.getGid(), index);
        }

        final Map<String, TaskShort> taskShortById = taskShortList.stream()
                .collect(Collectors.toUnmodifiableMap(TaskShort::getGid, taskShort -> taskShort));

        boolean changeRequire = true;
        while (changeRequire) {
            changeRequire = false;
            for (TaskShort taskShort : taskShortList) {
                if (taskShort.getParent() != null) {
                    if (processTask(taskShort.getGid(), taskShort.getParent().getGid(), indexByTaskShortId, taskShortById)) {
                        changeRequire = true;
                    }
                }
            }
        }

        return indexByTaskShortId.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(taskIdToIndex -> taskShortById.get(taskIdToIndex.getKey()))
                .collect(Collectors.toUnmodifiableList());
    }

    private static boolean processTask(String successorTaskId, String predecessorTaskId,
                                       Map<String, Integer> indexByTaskShortId, Map<String, TaskShort> taskShortById) {
        if (shouldConsider(successorTaskId, predecessorTaskId, indexByTaskShortId)) {
            List<TasksToUpdate> tasksToUpdates = new ArrayList<>();
            String newPredecessorTaskId = predecessorTaskId;
            boolean processFurther = true;
            while (processFurther) {
                processFurther = false;
                TaskShort newPredecessorTask = taskShortById.get(newPredecessorTaskId);
                if (newPredecessorTask.getParent() != null) {
                    if (shouldConsider(newPredecessorTaskId, newPredecessorTask.getParent().getGid(), indexByTaskShortId)) {
                        tasksToUpdates.add(new TasksToUpdate(newPredecessorTaskId, newPredecessorTask.getParent().getGid()));
                        newPredecessorTaskId = newPredecessorTask.getParent().getGid();
                        processFurther = true;
                    }
                }
            }

            for (int i = tasksToUpdates.size() - 1; i >= 0 ; i--) {
                TasksToUpdate tasksToUpdate = tasksToUpdates.get(i);
                updateIndexes(tasksToUpdate, indexByTaskShortId);
            }

            updateIndexes(new TasksToUpdate(successorTaskId, predecessorTaskId), indexByTaskShortId);
            return true;
        }

        return false;
    }

    private static boolean shouldConsider(String successorTaskId, String predecessorTaskId, Map<String, Integer> indexByTaskShortId) {
        Integer predecessorIndex = indexByTaskShortId.get(predecessorTaskId);
        Integer successorIndex = indexByTaskShortId.get(successorTaskId);
        if (predecessorIndex == null || successorIndex == null) {
            return false;
        }

        return successorIndex <= predecessorIndex;
    }

    private static void updateIndexes(TasksToUpdate tasksToUpdate, Map<String, Integer> indexByTaskShortId) {
        int predecessorIndex = indexByTaskShortId.get(tasksToUpdate.getPredecessorTaskId());
        indexByTaskShortId.put(tasksToUpdate.getSuccessorTaskId(), predecessorIndex + 1);
    }

    @Data
    private static final class TasksToUpdate {
        private final String successorTaskId;
        private final String predecessorTaskId;
    }
}