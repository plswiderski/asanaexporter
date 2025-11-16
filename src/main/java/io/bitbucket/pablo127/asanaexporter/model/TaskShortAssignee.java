package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TaskShortAssignee {
    private String gid;

    @JsonProperty("resource_type")
    private String resourceType;
}
