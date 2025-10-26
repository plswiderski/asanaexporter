package io.bitbucket.pablo127.asanaexporter.connector.section;

import io.bitbucket.pablo127.asanaexporter.model.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
class SectionData {
    private String gid;
    private String name;
    private Project project;
}
