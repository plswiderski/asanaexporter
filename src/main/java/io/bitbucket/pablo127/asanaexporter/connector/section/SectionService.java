package io.bitbucket.pablo127.asanaexporter.connector.section;

import java.io.IOException;
import java.util.List;

public interface SectionService {
    CreatedSectionTO createSection(SectionDescriptorTO section) throws IOException;
    List<CreatedSectionTO> getSection(SectionDescriptorTO section) throws IOException;
    void addTaskToSection(String sectionId, String taskId) throws IOException;
}
