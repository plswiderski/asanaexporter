package io.bitbucket.pablo127.asanaexporter.connector.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.bitbucket.pablo127.asanaexporter.model.Recurrence;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortAssignee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
class TaskToCreateTO {
    private String name;
    private String notes;

    @JsonProperty("due_on")
    private String dueOn;

    private List<String> projects;

    private TaskShortAssignee assignee;

    private Recurrence recurrence;

    private String workspace;

    @JsonProperty("assignee_section")
    private String assigneeSection;

    private boolean completed;
}
