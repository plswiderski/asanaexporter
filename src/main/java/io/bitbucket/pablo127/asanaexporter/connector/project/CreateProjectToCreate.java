package io.bitbucket.pablo127.asanaexporter.connector.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
class CreateProjectToCreate {
    private String name;

    @JsonProperty("workspace")
    private String workspaceGid;
}
