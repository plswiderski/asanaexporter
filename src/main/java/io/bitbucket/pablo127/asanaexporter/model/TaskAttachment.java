package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskAttachment {
    private String gid;
    private String name;

    @JsonProperty("resource_subtype")
    private String resourceSubtype;
}
