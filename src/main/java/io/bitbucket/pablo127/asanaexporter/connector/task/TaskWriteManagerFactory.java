package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.connector.attachment.AttachmentService;
import io.bitbucket.pablo127.asanaexporter.model.Parent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskWriteManagerFactory {

    public static TaskWriteManager create(String workspaceId, AttachmentService attachmentService,
                                          BiFunction<String, String, File> attachmentPathProvider,
                                          Function<Parent, String> parentToNewTaskIdProvider) {
        return new TaskWriteManagerImpl(workspaceId, attachmentService, attachmentPathProvider, parentToNewTaskIdProvider);
    }
}
