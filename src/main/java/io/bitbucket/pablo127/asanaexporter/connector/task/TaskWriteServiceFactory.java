package io.bitbucket.pablo127.asanaexporter.connector.task;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskWriteServiceFactory {

    public static TaskWriteService create() {
        return new TaskWriteServiceImpl();
    }
}
