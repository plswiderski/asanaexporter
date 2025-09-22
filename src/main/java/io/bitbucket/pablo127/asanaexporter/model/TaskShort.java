package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TaskShort {
    private String gid;
    private String name;
    private String notes;

    @JsonProperty("due_on")
    private String dueOn;

    @JsonProperty("completed_at")
    private String completedAt;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("modified_at")
    private String modifiedAt;

    private List<TaskShortProject> projects;

    private List<TaskMembership> memberships;

    private TaskShortAssignee assignee;

    private Parent parent;

    private Recurrence recurrence;
}
