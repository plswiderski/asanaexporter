package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentService;
import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentServiceFactory;
import io.bitbucket.pablo127.asanaexporter.connector.project.ProjectWriteService;
import io.bitbucket.pablo127.asanaexporter.connector.project.ProjectWriteServiceFactory;
import io.bitbucket.pablo127.asanaexporter.connector.section.CreatedSectionTO;
import io.bitbucket.pablo127.asanaexporter.connector.section.SectionDescriptorTO;
import io.bitbucket.pablo127.asanaexporter.connector.section.SectionManager;
import io.bitbucket.pablo127.asanaexporter.connector.section.SectionManagerFactory;
import io.bitbucket.pablo127.asanaexporter.connector.section.SectionService;
import io.bitbucket.pablo127.asanaexporter.connector.section.SectionServiceFactory;
import io.bitbucket.pablo127.asanaexporter.connector.task.CreateTasksResult;
import io.bitbucket.pablo127.asanaexporter.connector.task.TaskWriteManager;
import io.bitbucket.pablo127.asanaexporter.connector.task.TaskWriteManagerFactory;
import io.bitbucket.pablo127.asanaexporter.connector.user_task_list.UserTaskListService;
import io.bitbucket.pablo127.asanaexporter.connector.user_task_list.UserTaskListServiceFactory;
import io.bitbucket.pablo127.asanaexporter.connector.user_task_list.UserTaskListTO;
import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.Project;
import io.bitbucket.pablo127.asanaexporter.model.TaskAssigneeSection;
import io.bitbucket.pablo127.asanaexporter.model.TaskMembership;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class AsanaRestorer implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(AsanaRestorer.class);

    private final String workspaceName;

    @Override
    public void process() throws Exception {
        UserDownloadCommand userDownloadCommand = new UserDownloadCommand(workspaceName);
        userDownloadCommand.run();

        String workspaceId = userDownloadCommand.getWorkspaceId();

        ProjectsDownloadCommand projectsDownloadCommand = new ProjectsDownloadCommand(userDownloadCommand.getWorkspaceId());
        ProjectsDownloadCommand.DownloadedProjects downloadedProjects = projectsDownloadCommand.run();

        final Map<String, String> projectIdToProjectNameMap = new HashMap<>(downloadedProjects.getProjectIdToProjectNameMap());
        Function<Project, String> projectToProjectIdProvider = new ProjectToProjectIdProvider(projectIdToProjectNameMap, workspaceId);

        List<TaskShort> tasks = new CsvReportReader(userDownloadCommand.getUsers(), projectToProjectIdProvider)
                .readTasks();

        TasksImporter tasksImporter = new TasksImporter(workspaceId, userDownloadCommand.getUserId(), projectIdToProjectNameMap, null);
        logger.info("Obtain all tasks in the given workspace.");
        Set<TaskShort> tasksAlreadyInWorkspace = tasksImporter.importTasks();
        logger.info(String.format("Obtained '%s' tasks in the given workspace.", tasksAlreadyInWorkspace.size()));

        ProjectIdByProjectNameFinder projectIdByProjectNameFinder = new ProjectIdByProjectNameFinder(projectIdToProjectNameMap);

        FilteredTasks filteredTasks = filterOutTasksAlreadyImported(tasks, tasksAlreadyInWorkspace, projectIdByProjectNameFinder);

        findAndCreateNecessarySections(filteredTasks.getTasksToCreateNecessarySections(), projectIdByProjectNameFinder);
        findAndCreateNecessaryAssigneeSections(filteredTasks.getTasksToRestore(), workspaceId);
        restoreTasks(filteredTasks, tasksAlreadyInWorkspace, workspaceId);
    }

    private FilteredTasks filterOutTasksAlreadyImported(List<TaskShort> tasks, Set<TaskShort> tasksAlreadyInWorkspace, ProjectIdByProjectNameFinder projectIdByProjectNameFinder) {
        Map<String, List<TaskShort>> tasksAlreadyInWorkspaceByNames = tasksAlreadyInWorkspace.stream()
                .collect(Collectors.groupingBy(TaskShort::getName));

        Map<FilterTaskStatus, List<TaskShort>> tasksByFilterTaskStatus = tasks.stream()
                .collect(Collectors.groupingBy(taskShort -> {
                    final List<TaskShort> tasksInWorkspace = tasksAlreadyInWorkspaceByNames.getOrDefault(taskShort.getName(), List.of());
                    Optional<TaskShort> taskFoundInWorkspace = tasksInWorkspace.stream()
                            .filter(taskInWorkspace -> taskInWorkspace.getNotes().contains("TaskId: " + taskShort.getGid()))
                            .findFirst();
                    if (taskFoundInWorkspace.isPresent()) {
                        Set<SectionDescriptorTO> expectedSections = CollectionUtils.emptyIfNull(taskShort.getMemberships())
                                .stream()
                                .map(taskMembership -> {
                                    String projectId = projectIdByProjectNameFinder.apply(taskMembership.getProject().getName());
                                    return new SectionDescriptorTO(taskMembership.getSection().getName(), projectId);
                                })
                                .collect(Collectors.toUnmodifiableSet());

                        Set<SectionDescriptorTO> sectionsAlreadyInWorkspace = CollectionUtils.emptyIfNull(taskFoundInWorkspace.get().getMemberships())
                                .stream()
                                .map(taskMembership -> new SectionDescriptorTO(taskMembership.getSection().getName(), taskMembership.getProject().getGid()))
                                .collect(Collectors.toUnmodifiableSet());

                        if (expectedSections.equals(sectionsAlreadyInWorkspace)) {
                            logger.info("Task '{}' (id: '{}') will be ignored. Already restored.", taskShort.getName(), taskShort.getGid());
                            return FilterTaskStatus.TASK_ALREADY_FULLY_RESTORED;
                        }

                        return FilterTaskStatus.TASK_SECTIONS_TO_UPDATE;
                    }

                    return FilterTaskStatus.TASK_TO_RESTORE;
                }));

        final List<TaskShort> tasksToRestore = tasksByFilterTaskStatus.getOrDefault(FilterTaskStatus.TASK_TO_RESTORE, List.of());
        final List<TaskShort> tasksToCreateNecessarySections = tasksByFilterTaskStatus.getOrDefault(FilterTaskStatus.TASK_SECTIONS_TO_UPDATE, List.of());
        final List<TaskShort> tasksAlreadyRestored = tasksByFilterTaskStatus.getOrDefault(FilterTaskStatus.TASK_ALREADY_FULLY_RESTORED, List.of());
        logger.info("Tasks to restore: '{}', tasks to create necessary sections: '{}', tasks already restored: '{}'", tasksToRestore.size(),
                tasksToCreateNecessarySections.size(), tasksAlreadyRestored.size());

        return new FilteredTasks(tasksToRestore, tasksToCreateNecessarySections);
    }

    private void restoreTasks(FilteredTasks filteredTasks, Set<TaskShort> tasksAlreadyInWorkspace, String workspaceId) throws InterruptedException {
        AttachmentService attachmentService = AttachmentServiceFactory.create();

        final ParentToNewTaskIdProvider parentToNewTaskIdProvider = new ParentToNewTaskIdProvider(tasksAlreadyInWorkspace);
        TaskWriteManager taskWriteManager = TaskWriteManagerFactory.create(workspaceId, attachmentService, new AttachmentPathProvider(),
                parentToNewTaskIdProvider);
        CreateTasksResult createdTasksResult = taskWriteManager.createTasks(filteredTasks.getTasksToRestore());

        final List<TaskShort> tasksToCreateNecessarySections = filteredTasks.getTasksToCreateNecessarySections();
        final Map<String, String> oldToNewTaskId = prepareOldToNewTaskIdMap(createdTasksResult, tasksToCreateNecessarySections, tasksAlreadyInWorkspace);

        SectionManager sectionManager = SectionManagerFactory.create();
        sectionManager.addTasksToSections(tasksToCreateNecessarySections, oldToNewTaskId);
    }

    private static Map<String, String> prepareOldToNewTaskIdMap(CreateTasksResult createdTasksResult, List<TaskShort> tasksToCreateNecessarySections,
                                                                Set<TaskShort> tasksAlreadyInWorkspace) {
        Map<String, List<TaskShort>> tasksAlreadyInWorkspaceByNames = tasksAlreadyInWorkspace.stream()
                .collect(Collectors.groupingBy(TaskShort::getName));

        Map<String, String> oltToNewTaskIdFromExisting = tasksToCreateNecessarySections.stream()
                .collect(Collectors.toUnmodifiableMap(TaskShort::getGid, taskShort ->
                        tasksAlreadyInWorkspaceByNames.getOrDefault(taskShort.getName(), List.of())
                                .stream()
                                .filter(taskInWorkspace -> taskInWorkspace.getNotes().contains("TaskId: " + taskShort.getGid()))
                                .map(TaskShort::getGid)
                                .findFirst()
                                .orElseThrow()));


        return Stream.of(createdTasksResult.getOldToNewTaskId(), oltToNewTaskIdFromExisting)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void findAndCreateNecessarySections(List<TaskShort> tasks, ProjectIdByProjectNameFinder projectIdByProjectNameFinder) throws IOException {
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
                    String projectId = projectIdByProjectNameFinder.apply(projectName);

                    final SectionDescriptorTO sectionDescriptorTO = new SectionDescriptorTO(sectionName, projectId);
                    List<CreatedSectionTO> createdSections = sectionService.getSection(sectionDescriptorTO);
                    sectionsByNameAndProject.putAll(createdSections.stream()
                            .collect(Collectors.toUnmodifiableMap(sectionTO -> new SectionNameProjectName(sectionTO.getProject().getName(), sectionTO.getName()),
                                    Function.identity())));

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

    private void findAndCreateNecessaryAssigneeSections(List<TaskShort> tasks, String workspaceId) throws IOException {
        Map<UserTaskAssigneeSection, List<TaskShort>> userTaskAssigneeSectionToTasks = tasks.stream()
                .filter(taskShort -> taskShort.getAssigneeSection() != null)
                .map(taskShort -> {
                    final UserTaskAssigneeSection assigneeSection = new UserTaskAssigneeSection(taskShort.getAssigneeSection().getGid(),
                            taskShort.getAssigneeSection().getName(),
                            taskShort.getAssignee().getGid());
                    return Map.entry(assigneeSection, taskShort);
                })
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableList())));

        SectionService sectionService = SectionServiceFactory.create();
        UserTaskListService userTaskListService = UserTaskListServiceFactory.create();

        Map<UserTaskAssigneeSection, CreatedSectionTO> userTaskAssigneeSectionToCreatedSectionTO = new HashMap<>();
        for (UserTaskAssigneeSection userTaskAssigneeSection : userTaskAssigneeSectionToTasks.keySet()) {
            CreatedSectionTO createdSectionTO;
            if (userTaskAssigneeSectionToCreatedSectionTO.containsKey(userTaskAssigneeSection)) {
                createdSectionTO = userTaskAssigneeSectionToCreatedSectionTO.get(userTaskAssigneeSection);
            } else {
                UserTaskListTO userTaskList = userTaskListService.getUserTaskList(userTaskAssigneeSection.getUserId(), workspaceId);

                final SectionDescriptorTO sectionDescriptorTO = new SectionDescriptorTO(userTaskAssigneeSection.getName(), userTaskList.getGid());
                List<CreatedSectionTO> createdSections = sectionService.getSection(sectionDescriptorTO);

                userTaskAssigneeSectionToCreatedSectionTO.putAll(createdSections.stream()
                        .collect(Collectors.toUnmodifiableMap(sectionTO -> new UserTaskAssigneeSection(sectionTO.getId(),
                                        sectionTO.getName(), userTaskAssigneeSection.getUserId()),
                                Function.identity())));

                createdSectionTO = userTaskAssigneeSectionToCreatedSectionTO.get(userTaskAssigneeSection);
                if (createdSectionTO == null) {
                    createdSectionTO = sectionService.createSection(sectionDescriptorTO);
                    userTaskAssigneeSectionToCreatedSectionTO.put(userTaskAssigneeSection, createdSectionTO);
                }
            }

            List<TaskShort> taskShorts = userTaskAssigneeSectionToTasks.get(userTaskAssigneeSection);
            for (TaskShort taskShort : taskShorts) {
                final TaskAssigneeSection assigneeSection = taskShort.getAssigneeSection();
                assigneeSection.setGid(createdSectionTO.getId());
            }
        }
    }

    @Data
    private static final class SectionNameProjectName {
        private final String projectName;
        private final String sectionName;
    }

    @Data
    private static final class UserTaskAssigneeSection {
        private final String gid;
        private final String name;
        private final String userId;
    }

    @Data
    private static final class FilteredTasks {
        private final List<TaskShort> tasksToRestore;
        private final List<TaskShort> tasksToCreateNecessarySections;
    }

    private static final class ProjectToProjectIdProvider implements Function<Project, String> {
        private final Map<String, String> projectIdToProjectNameMap;
        private final ProjectWriteService projectWriteService;

        public ProjectToProjectIdProvider(Map<String, String> projectIdToProjectNameMap, String workspaceId) {
            this.projectIdToProjectNameMap = projectIdToProjectNameMap;
            this.projectWriteService = ProjectWriteServiceFactory.create(workspaceId);
        }

        @Override
        public String apply(Project projectToCheck) {
            Optional<String> projectIdOpt = projectIdToProjectNameMap.entrySet()
                    .stream()
                    .filter(projectIdToProjectName -> projectToCheck.getName().equals(projectIdToProjectName.getValue()))
                    .map(Map.Entry::getKey)
                    .findFirst();

            String projectId;
            if (projectIdOpt.isEmpty()) {
                try {
                    logger.info("Create project with name '{}'", projectToCheck.getName());

                    final Project project = projectWriteService.createProject(projectToCheck);
                    projectId = project.getGid();
                    projectIdToProjectNameMap.put(projectId, project.getName());
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
                String parentNameTrimmed = parent.getName().trim();
                parentNewId = tasksAlreadyInWorkspace.stream()
                        .filter(taskShort -> taskShort.getName().equals(parent.getName())
                                || taskShort.getName().equals(parentNameTrimmed))
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

    @RequiredArgsConstructor
    private static final class ProjectIdByProjectNameFinder implements Function<String, String> {
        private final Map<String, String> projectIdToProjectNameMap;

        @Override
        public String apply(String projectName) {
            return projectIdToProjectNameMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(projectName))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow();
        }
    }

    private enum FilterTaskStatus {
        TASK_TO_RESTORE,
        TASK_SECTIONS_TO_UPDATE,
        TASK_ALREADY_FULLY_RESTORED
    }
}
