package io.bitbucket.pablo127.asanaexporter.model.user;

import io.bitbucket.pablo127.asanaexporter.model.Workspace;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserData {
    private String gid;
    private String email;
    private String name;
    private String resource_type;
    private Map<String, String> photo;
    private List<Workspace> workspaces;
}
