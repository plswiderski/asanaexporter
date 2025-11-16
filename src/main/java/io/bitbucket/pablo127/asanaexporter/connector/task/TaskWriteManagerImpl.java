package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentFile;
import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentService;
import io.bitbucket.pablo127.asanaexporter.model.AttachmentDescriptor;
import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.TaskAttachment;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortProject;
import io.bitbucket.pablo127.asanaexporter.util.ExecutorUtil;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

final class TaskWriteManagerImpl implements TaskWriteManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskWriteManagerImpl.class);
    private static final String ATTACHMENT_NOTES_URL_FORMAT = "https://app.asana.com/app/asana/-/get_asset?asset_id=%s";

    private final String workspaceId;
    private final TaskWriteService taskWriteService;
    private final AttachmentService attachmentService;
    private final BiFunction<String, String, File> attachmentPathProvider;
    private final Function<Parent, String> parentToNewTaskIdProvider;

    public TaskWriteManagerImpl(String workspaceId, AttachmentService attachmentService,
                                BiFunction<String, String, File> attachmentPathProvider,
                                Function<Parent, String> parentToNewTaskIdProvider) {
        this.workspaceId = workspaceId;
        this.taskWriteService = TaskWriteServiceFactory.create();
        this.attachmentService = attachmentService;
        this.attachmentPathProvider = attachmentPathProvider;
        this.parentToNewTaskIdProvider = parentToNewTaskIdProvider;
    }

    @Override
    public CreateTasksResult createTasks(List<TaskShort> tasks) {
        logger.info("Start creating tasks");
        List<TaskShort> taskShorts = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        Map<String, String> oldToNewTaskId = new ConcurrentHashMap<>();

        List<TaskShort> tasksToProcess = new TasksToProcessSorter().sort(tasks);

        ExecutorService executor = ExecutorUtil.createExecutorService();
        AtomicInteger counter = new AtomicInteger(0);
        try (ProgressBar progressBar = new ProgressBar("Creating tasks", tasksToProcess.size())) {
            boolean shutdownInvoked = false;
            for (TaskShort task : tasksToProcess) {
                final Runnable createTaskRunnable = () -> {
                    try {
                        final TaskShort createdTask = createTask(task, oldToNewTaskId);

                        oldToNewTaskId.put(task.getGid(), createdTask.getGid());
                        taskShorts.add(createdTask);
                        progressBar.stepTo(counter.incrementAndGet());
                    } catch (Exception e) {
                        exceptions.add(new Exception("Could not create task with name: " + task.getName(), e));
                    }
                };

                if (task.getParent() == null) {
                    executor.submit(createTaskRunnable);
                } else {
                    if (!shutdownInvoked) {
                        shutdownInvoked = true;
                        executor.shutdown();
                        executor.awaitTermination(10, TimeUnit.HOURS);
                    }

                    createTaskRunnable.run();
                }
            }

            if (!shutdownInvoked) {
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            logger.error("Unexpected error occurred while creating tasks.", e);
        } finally {
            for (Exception exception : exceptions) {
                logger.error(exception.getMessage(), exception);
            }
            final String additionalMessage = counter.get() < tasksToProcess.size()
                    ? "After fixing problem rerun import - tasks already imported" +
                            " will not be created again."
                    : "Restore succeeded.";
            logger.info(String.format("Correctly restored %d tasks. " + additionalMessage, counter.get()));
        }

        return new CreateTasksResult(taskShorts, oldToNewTaskId);
    }

    private TaskShort createTask(TaskShort task, Map<String, String> oldToNewTaskId) throws IOException {
        final String notes = task.getNotes() + "\n" + prepareNotesImportInformation(task);
        TaskToCreateTO taskToCreate = convertToTaskToCreateTO(task, notes);

        TaskShort createdTask;
        if (task.getParent() == null) {
            createdTask = taskWriteService.createTask(taskToCreate);
        } else {
            String parentId = oldToNewTaskId.get(task.getParent().getGid());
            if (parentId == null) {
                parentId = parentToNewTaskIdProvider.apply(task.getParent());
                oldToNewTaskId.put(task.getParent().getGid(), parentId);
            }
            createdTask = taskWriteService.createSubtask(parentId, taskToCreate);
        }

        List<AttachmentDescriptor> attachmentDescriptors = uploadAttachments(task.getGid(), task.getAttachments(), createdTask.getGid());
        String newNotes = obtainTaskNotesWithAttachments(task, createdTask, attachmentDescriptors);
        if (!newNotes.equals(notes)) {
            return taskWriteService.updateTask(createdTask.getGid(), convertToTaskToCreateTO(task, newNotes));
        }

        return createdTask;
    }

    private TaskToCreateTO convertToTaskToCreateTO(TaskShort task, String notes) {
        return TaskToCreateTO.builder()
                .name(task.getName())
                .notes(notes)
                .dueOn(task.getDueOn())
                .assignee(task.getAssignee())
                .projects(task.getProjects().stream()
                        .map(TaskShortProject::getGid)
                        .collect(Collectors.toUnmodifiableList()))
                .recurrence(task.getRecurrence())
                .workspace(workspaceId)
                .assigneeSection(task.getAssigneeSection() != null ? task.getAssigneeSection().getGid() : null)
                .completed(StringUtils.isNotEmpty(task.getCompletedAt()))
                .build();
    }

    private static String obtainTaskNotesWithAttachments(TaskShort originalTask, TaskShort createdTask, List<AttachmentDescriptor> attachmentDescriptors) {
        Map<String, AttachmentDescriptor> nameToAttachment = attachmentDescriptors.stream()
                .collect(Collectors.toUnmodifiableMap(AttachmentDescriptor::getName, Function.identity()));

        String notes = createdTask.getNotes();
        for (TaskAttachment originalTaskAttachment : originalTask.getAttachments()) {
            AttachmentDescriptor attachmentDescriptor = nameToAttachment.get(originalTaskAttachment.getName());

            String originalGid = originalTaskAttachment.getGid();

            notes = notes.replace(String.format(ATTACHMENT_NOTES_URL_FORMAT, originalGid),
                    String.format(ATTACHMENT_NOTES_URL_FORMAT, attachmentDescriptor.getGid()));
        }

        return notes;
    }

    private String prepareNotesImportInformation(TaskShort task) {
        List<Map.Entry<String, String>> properties = List.of(
                Map.entry("TaskId", task.getGid()),
                Map.entry("Created at", task.getCreatedAt()),
                Map.entry("Modified at", task.getModifiedAt()),
                Map.entry("Completed at", task.getCompletedAt()));
        final String propertiesString = properties.stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        return "---\n" + "Task restored from backup. Original properties:\n" + propertiesString;
    }

    private List<AttachmentDescriptor> uploadAttachments(String taskGid, List<TaskAttachment> attachments, String newTaskGid) throws IOException {
        List<AttachmentDescriptor> attachmentDescriptors = new ArrayList<>();
        for (TaskAttachment taskAttachment : CollectionUtils.emptyIfNull(attachments)) {
            File filePath = attachmentPathProvider.apply(taskGid, taskAttachment.getName());
            if (!filePath.exists()) {
                throw new IOException(String.format("Task (with id: %s) attachment %s does not exist", taskGid, taskAttachment.getName()));
            }
            byte[] content = Files.readAllBytes(filePath.toPath());

            final AttachmentDescriptor attachmentDescriptor = AttachmentDescriptor.builder()
                    .name(taskAttachment.getName())
                    .build();
            final AttachmentFile attachmentFile = new AttachmentFile(content, attachmentDescriptor);
            AttachmentDescriptor uploadedAttachment = attachmentService.uploadAttachment(attachmentFile, newTaskGid);

            attachmentDescriptors.add(uploadedAttachment);
        }

        return attachmentDescriptors;
    }
}
