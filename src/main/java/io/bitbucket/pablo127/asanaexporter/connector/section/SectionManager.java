package io.bitbucket.pablo127.asanaexporter.connector.section;

import io.bitbucket.pablo127.asanaexporter.model.TaskShort;

import java.util.List;
import java.util.Map;

public interface SectionManager {
    void addTasksToSections(List<TaskShort> originalTasks, Map<String, String> oldToNewTaskId);
}
