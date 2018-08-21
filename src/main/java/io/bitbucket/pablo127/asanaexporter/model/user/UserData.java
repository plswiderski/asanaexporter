package io.bitbucket.pablo127.asanaexporter.model.user;

import io.bitbucket.pablo127.asanaexporter.model.Workspace;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserData {
	private String[] data;
    private String id;
    private String email;
    private String name;
    private Map<String, String> photo;
    private List<Workspace> workspaces;
}
