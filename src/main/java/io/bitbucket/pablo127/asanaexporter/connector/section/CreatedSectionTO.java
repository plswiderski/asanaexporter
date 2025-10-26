package io.bitbucket.pablo127.asanaexporter.connector.section;

import io.bitbucket.pablo127.asanaexporter.model.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public final class CreatedSectionTO {
    private final String id;
    private final String name;
    private final Project project;
}
