package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentFile;
import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentService;
import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentServiceFactory;
import io.bitbucket.pablo127.asanaexporter.model.TaskAttachment;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class AttachmentsSaver {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentsSaver.class);

    private static final File ATTACHMENTS_RESULT_FILE = new File("attachments");

    private final Set<TaskShort> tasks;
    private final AttachmentService attachmentService;

    public AttachmentsSaver(Set<TaskShort> tasks) {
        this.tasks = tasks;
        this.attachmentService = AttachmentServiceFactory.create();
    }

    void save() throws IOException {
        List<TaskShort> attachmentTasksToSave = tasks.stream()
                .filter(taskShort -> taskShort.getAttachments() != null && !taskShort.getAttachments().isEmpty())
                .collect(Collectors.toUnmodifiableList());

        if (attachmentTasksToSave.isEmpty()) {
            return;
        }

        logger.info(String.format("Start to save attachments in %s", ATTACHMENTS_RESULT_FILE.getAbsolutePath()));
        for (TaskShort taskShort : attachmentTasksToSave) {
            List<AttachmentFile> attachments = taskShort.getAttachments()
                    .stream()
                    .map(TaskAttachment::getGid)
                    .map(attachmentId -> {
                        try {
                            return attachmentService.getAttachment(attachmentId);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .map(attachment -> {
                        try {
                            final File directory = new File(ATTACHMENTS_RESULT_FILE, taskShort.getGid());
                            directory.mkdirs();

                            File file = new File(directory, attachment.getAttachmentDescriptor().getName());
                            logger.info("Save attachment to " + file.getAbsolutePath());

                            Files.write(file.toPath(), attachment.getContent(), StandardOpenOption.CREATE);

                            TaskAttachment taskAttachment = taskShort.getAttachments().stream()
                                    .filter(ta -> ta.getGid().equals(attachment.getAttachmentDescriptor().getGid()))
                                    .findFirst()
                                    .orElseThrow();

                            taskAttachment.setName(attachment.getAttachmentDescriptor().getName());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        return attachment;
                    })
                    .collect(Collectors.toUnmodifiableList());

            logger.info(String.format("All needed attachments for task %s are saved. Processed %d attachments", taskShort.getGid(), attachments.size()));
        }
    }
}
