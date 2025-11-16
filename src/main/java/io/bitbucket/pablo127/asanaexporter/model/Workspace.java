package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Workspace {
    private String gid;
    private String name;

    @JsonProperty("resource_type")
    private String resourceType;
}
