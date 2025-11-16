package io.bitbucket.pablo127.asanaexporter.connector.section;

import lombok.Data;

import java.util.List;

@Data
class SectionsResponse {
    private List<SectionData> data;
}
