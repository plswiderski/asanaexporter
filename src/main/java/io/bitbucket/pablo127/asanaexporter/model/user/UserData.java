package io.bitbucket.pablo127.asanaexporter.model.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.bitbucket.pablo127.asanaexporter.model.Workspace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserData {
    private String gid;
    private String email;
    private String name;

    @JsonProperty("resource_type")
    private String resourceType;

    private List<Workspace> workspaces;
}
