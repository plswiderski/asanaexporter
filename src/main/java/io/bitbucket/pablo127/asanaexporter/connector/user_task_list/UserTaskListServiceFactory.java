package io.bitbucket.pablo127.asanaexporter.connector.user_task_list;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserTaskListServiceFactory {

    public static UserTaskListService create() {
        return new UserTaskListServiceImpl();
    }
}
