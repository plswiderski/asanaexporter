package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskShortAssignee {
    private String gid;

    @JsonProperty("resource_type")
    private String resourceType;
}
