package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.user.User;
import io.bitbucket.pablo127.asanaexporter.model.user.UserData;
import lombok.Getter;

import java.io.IOException;

public class UserDownloadCommand implements Runnable {

    @Getter
    private String workspaceId;

    @Getter
    private String userId;

    @Getter
    private String userName;

    @Override
    public void run() {
        try {
            Requester<User> requester = new Requester<>(User.class);
            UserData userData = requester.request(new UriBuilder().uri("https://app.asana.com/api/1.0/users/me"))
                    .getData();

            workspaceId = userData.getWorkspaces()
                    .get(0)
                    .getGid();
            userId = userData.getGid();
            userName = userData.getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
