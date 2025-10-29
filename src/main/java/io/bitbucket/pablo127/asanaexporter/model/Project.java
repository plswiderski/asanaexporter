package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Project {
    private String gid;
    private String name;
    private String notes;

    private Boolean archived;
    private String color;
    private String icon;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("modified_at")
    private String modifiedAt;

    @JsonProperty("due_on")
    private String dueOn;
}