package io.bitbucket.pablo127.asanaexporter.connector.project;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProjectWriteServiceFactory {

    public static ProjectWriteService create(String workspaceId) {
        return new ProjectWriteServiceImpl(workspaceId);
    }
}
