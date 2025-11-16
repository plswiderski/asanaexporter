package io.bitbucket.pablo127.asanaexporter.connector.section;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
class AddTaskToSectionData {
    @JsonProperty("task")
    private String taskId;
}
