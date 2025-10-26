package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentService;
import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentServiceFactory;
import io.bitbucket.pablo127.asanaexporter.connector.project.ProjectWriteService;
import io.bitbucket.pablo127.asanaexporter.connector.project.ProjectWriteServiceFactory;
import io.bitbucket.pablo127.asanaexporter.connector.section.CreatedSectionTO;
import io.bitbucket.pablo127.asanaexporter.connector.section.SectionDescriptorTO;
import io.bitbucket.pablo127.asanaexporter.connector.section.SectionService;
import io.bitbucket.pablo127.asanaexporter.connector.section.SectionServiceFactory;
import io.bitbucket.pablo127.asanaexporter.connector.task.CreateTasksResult;
import io.bitbucket.pablo127.asanaexporter.connector.task.TaskWriteManager;
import io.bitbucket.pablo127.asanaexporter.connector.task.TaskWriteManagerFactory;
import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.Project;
import io.bitbucket.pablo127.asanaexporter.model.TaskMembership;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
final class AsanaRestorer implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(AsanaRestorer.class);

    private final String workspaceName;

    @Override
    public void process() throws IOException {
        UserDownloadCommand userDownloadCommand = new UserDownloadCommand(workspaceName);
        userDownloadCommand.run();

        String workspaceId = userDownloadCommand.getWorkspaceId();

        ProjectsDownloadCommand projectsDownloadCommand = new ProjectsDownloadCommand(userDownloadCommand.getWorkspaceId());
        projectsDownloadCommand.run();

        final Map<String, String> projectIdToProjectNameMap = new HashMap<>(projectsDownloadCommand.getProjectIdToProjectNameMap());
        Function<String, String> projectNameToProjectIdProvider = new ProjectNameToProjectIdProvider(projectIdToProjectNameMap, workspaceId);

        List<TaskShort> tasks = new CsvReportReader(userDownloadCommand.getUsers(), projectNameToProjectIdProvider)
                .readTasks();

        TasksImporter tasksImporter = new TasksImporter(workspaceId, userDownloadCommand.getUserId(), projectIdToProjectNameMap, null);
        logger.info("Obtain all tasks in the given workspace.");
        Set<TaskShort> tasksAlreadyInWorkspace = tasksImporter.importTasks();

        tasks = filterOutTasksAlreadyImported(tasks, tasksAlreadyInWorkspace);

        findAndCreateNecessarySections(tasks, projectIdToProjectNameMap);
        restoreTasks(tasks, tasksAlreadyInWorkspace, workspaceId);
    }

    private List<TaskShort> filterOutTasksAlreadyImported(List<TaskShort> tasks, Set<TaskShort> tasksAlreadyInWorkspace) {
        Map<String, List<TaskShort>> tasksAlreadyInWorkspaceByNames = tasksAlreadyInWorkspace.stream()
                .collect(Collectors.groupingBy(TaskShort::getName));

        return tasks.stream()
                .filter(taskShort -> {
                    final List<TaskShort> tasksInWorkspace = tasksAlreadyInWorkspaceByNames.getOrDefault(taskShort.getName(), List.of());
                    final boolean toFurtherImport = tasksInWorkspace.stream()
                            .noneMatch(taskInWorkspace -> taskInWorkspace.getNotes().contains("TaskId: " + taskShort.getGid()));
                    if (!toFurtherImport) {
                        logger.info("Task '{}' (id: '{}') will be ignored. Already restored.", taskShort.getName(), taskShort.getGid());
                    }
                    return toFurtherImport;
                })
                .collect(Collectors.toUnmodifiableList());
    }

    private void restoreTasks(List<TaskShort> tasks, Set<TaskShort> tasksAlreadyInWorkspace, String workspaceId) throws IOException {
        AttachmentService attachmentService = AttachmentServiceFactory.create();

        final ParentToNewTaskIdProvider parentToNewTaskIdProvider = new ParentToNewTaskIdProvider(tasksAlreadyInWorkspace);
        TaskWriteManager taskWriteManager = TaskWriteManagerFactory.create(workspaceId, attachmentService, new AttachmentPathProvider(),
                parentToNewTaskIdProvider);
        CreateTasksResult createdTasksResult = taskWriteManager.createTasks(tasks);

        addTasksToSections(tasks, createdTasksResult);
    }

    private static void addTasksToSections(List<TaskShort> originalTasks, CreateTasksResult createdTasksResult) throws IOException {
        SectionService sectionService = SectionServiceFactory.create();

        Map<String, List<TaskMembership>> oldTaskIdToMemberships = originalTasks.stream()
                .filter(taskShort -> CollectionUtils.isNotEmpty(taskShort.getMemberships()))
                .collect(Collectors.toUnmodifiableMap(TaskShort::getGid, TaskShort::getMemberships));

        for (String oldTaskId : oldTaskIdToMemberships.keySet()) {
            String newTaskId = createdTasksResult.getOldToNewTaskId().get(oldTaskId);
            List<TaskMembership> memberships = oldTaskIdToMemberships.get(oldTaskId);

            for (TaskMembership membership : memberships) {
                sectionService.addTaskToSection(membership.getSection().getGid(), newTaskId);
            }
        }
    }

    private void findAndCreateNecessarySections(List<TaskShort> tasks, Map<String, String> projectIdToProjectNameMap) throws IOException {
        SectionService sectionService = SectionServiceFactory.create();

        List<TaskShort> tasksToProcess = tasks.stream()
                .filter(task -> CollectionUtils.isNotEmpty(task.getMemberships()))
                .collect(Collectors.toUnmodifiableList());

        Map<SectionNameProjectName, CreatedSectionTO> sectionsByNameAndProject = new HashMap<>();
        for (TaskShort task : tasksToProcess) {
            for (TaskMembership taskMembership : task.getMemberships()) {
                String sectionName = taskMembership.getSection().getName();
                String projectName = taskMembership.getProject().getName();

                final SectionNameProjectName sectionNameProjectName = new SectionNameProjectName(projectName, sectionName);
                CreatedSectionTO createdSectionTO;
                if (sectionsByNameAndProject.containsKey(sectionNameProjectName)) {
                    createdSectionTO = sectionsByNameAndProject.get(sectionNameProjectName);
                } else {
                    String projectId = projectIdToProjectNameMap.entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().equals(projectName))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElseThrow();

                    final SectionDescriptorTO sectionDescriptorTO = new SectionDescriptorTO(sectionName, projectId);
                    List<CreatedSectionTO> createdSections = sectionService.getSection(sectionDescriptorTO);
                    sectionsByNameAndProject.putAll(createdSections.stream()
                            .collect(Collectors.toUnmodifiableMap(sectionTO -> new SectionNameProjectName(sectionTO.getProject().getName(), sectionTO.getName()), Function.identity())));

                    createdSectionTO = sectionsByNameAndProject.get(sectionNameProjectName);
                    if (createdSectionTO == null) {
                        createdSectionTO = sectionService.createSection(sectionDescriptorTO);
                        sectionsByNameAndProject.put(sectionNameProjectName, createdSectionTO);
                    }
                }

                taskMembership.getSection().setGid(createdSectionTO.getId());
                taskMembership.getProject().setGid(createdSectionTO.getProject().getGid());
            }
        }
    }

    @Data
    private static final class SectionNameProjectName {
        private final String projectName;
        private final String sectionName;
    }

    private static final class ProjectNameToProjectIdProvider implements Function<String, String> {
        private final Map<String, String> projectIdToProjectNameMap;
        private final ProjectWriteService projectWriteService;

        public ProjectNameToProjectIdProvider(Map<String, String> projectIdToProjectNameMap, String workspaceId) {
            this.projectIdToProjectNameMap = projectIdToProjectNameMap;
            this.projectWriteService = ProjectWriteServiceFactory.create(workspaceId);
        }

        @Override
        public String apply(String projectName) {
            Optional<String> projectIdOpt = projectIdToProjectNameMap.entrySet()
                    .stream()
                    .filter(projectIdToProjectName -> projectIdToProjectName.getValue().equals(projectName))
                    .map(Map.Entry::getKey)
                    .findFirst();

            String projectId;
            if (projectIdOpt.isEmpty()) {
                try {
                    logger.info("Created project with name '{}'", projectName);

                    final Project project = projectWriteService.createProject(projectName);
                    projectId = project.getGid();
                    projectIdToProjectNameMap.put(projectId, projectName);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                projectId = projectIdOpt.get();
            }
            return projectId;
        }
    }

    @RequiredArgsConstructor
    private static final class ParentToNewTaskIdProvider implements Function<Parent, String> {

        private final Set<TaskShort> tasksAlreadyInWorkspace;

        @Override
        public String apply(Parent parent) {
            String parentNewId = null;
            try {
                parentNewId = tasksAlreadyInWorkspace.stream()
                        .filter(taskShort -> taskShort.getName().equals(parent.getName()))
                        .filter(taskShort -> taskShort.getNotes().contains(parent.getGid()))
                        .findFirst()
                        .map(TaskShort::getGid)
                        .orElseThrow(() -> new IOException("Could not find any task with name: '" + parent.getName()
                                + "' that has original id " + parent.getGid() + " in the task's description."));
            } catch (IOException e) {
                logger.error("Could not find parent task " + parent.getName(), e);
            }

            return parentNewId;
        }
    }
}
