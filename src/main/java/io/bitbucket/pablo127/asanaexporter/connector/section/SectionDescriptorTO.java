package io.bitbucket.pablo127.asanaexporter.connector.section;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public final class SectionDescriptorTO {
    private final String name;
    private final String projectId;
}
