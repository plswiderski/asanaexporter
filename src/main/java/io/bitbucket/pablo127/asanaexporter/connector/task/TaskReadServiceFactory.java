package io.bitbucket.pablo127.asanaexporter.connector.task;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskReadServiceFactory {

    public static TaskReadService create(String workspaceId) {
        return new TaskReadServiceImpl(workspaceId);
    }
}
