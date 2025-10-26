package io.bitbucket.pablo127.asanaexporter.connector.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
class CreateProjectRequest {
    private CreateProjectToCreate data;
}
