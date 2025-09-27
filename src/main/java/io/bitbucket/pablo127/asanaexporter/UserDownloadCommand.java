package io.bitbucket.pablo127.asanaexporter;

import io.bitbucket.pablo127.asanaexporter.model.Workspace;
import io.bitbucket.pablo127.asanaexporter.model.user.User;
import io.bitbucket.pablo127.asanaexporter.model.user.UserData;
import io.bitbucket.pablo127.asanaexporter.model.user.Users;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserDownloadCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(UserDownloadCommand.class);

    private final String givenWorkspaceName;

    @Getter
    private String workspaceId;

    @Getter
    private String userId;

    @Getter
    private List<UserData> users;

    private List<Workspace> workspaces;

    public UserDownloadCommand(String givenWorkspaceName) {
        this.givenWorkspaceName = givenWorkspaceName;
    }

    @Override
    public void run() {
        try {
            Requester<User> requester = new Requester<>(User.class);
            UserData userData = requester.requestGet(new UriBuilder().uri("https://app.asana.com/api/1.0/users/me"))
                    .getData();

            this.workspaces = userData.getWorkspaces();
            this.workspaceId = workspaces.stream()
                    .filter(workspace -> workspace.getName().equals(givenWorkspaceName))
                    .map(Workspace::getGid)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find workspace with name " + givenWorkspaceName));

            userId = userData.getGid();

            users = findUsers();
            addOwnerToUsers(users, userData);

            logger.info("Downloaded userData.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addOwnerToUsers(List<UserData> users, UserData ownerUserData) {
        UserData ownerUserDataToAdd = UserData.builder()
                .gid(ownerUserData.getGid())
                .name(ownerUserData.getName())
                .resourceType(ownerUserData.getResourceType())
                .build();

        if (!users.contains(ownerUserDataToAdd))
            users.add(ownerUserData);
    }

    private List<UserData> findUsers() throws IOException {
        List<UserData> allUsers = new ArrayList<>();
        for (Workspace workspace : workspaces) {
            if ("Personal Projects".equals(workspace.getName()))
                continue;

            Users users = new Requester<>(Users.class)
                    .requestGet(new UriBuilder().findWorkspacesUsers(workspace.getGid()));

            allUsers.addAll(users.getData());
        }
        return allUsers;
    }
}
