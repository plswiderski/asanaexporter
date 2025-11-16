package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @JsonProperty("num_subtasks")
    private int subtasksCount;

    private List<TaskShortProject> projects;

    private List<TaskMembership> memberships;

    private TaskShortAssignee assignee;

    private Parent parent;

    private Recurrence recurrence;

    private List<TaskAttachment> attachments;

    private Workspace workspace;

    @JsonProperty("assignee_section")
    private TaskAssigneeSection assigneeSection;
}
