package io.bitbucket.pablo127.asanaexporter.connector.section;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
class CreateSectionRequest {
    private SectionData data;
}
