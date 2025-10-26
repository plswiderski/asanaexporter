package io.bitbucket.pablo127.asanaexporter.connector.section;

import io.bitbucket.pablo127.asanaexporter.Requester;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

final class SectionServiceImpl implements SectionService {

    @Override
    public CreatedSectionTO createSection(SectionDescriptorTO section) throws IOException {
        Requester<CreateSectionResponse> requester = new Requester<>(CreateSectionResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/projects/" + section.getProjectId() + "/sections");

        final SectionData sectionData = SectionData.builder()
                .name(section.getName())
                .build();
        final CreateSectionRequest createSectionRequest = new CreateSectionRequest(sectionData);

        CreateSectionResponse createSectionResponse = requester.requestPost(() -> url, createSectionRequest);
        return convertSectionData(createSectionResponse.getData());
    }

    @Override
    public List<CreatedSectionTO> getSection(SectionDescriptorTO section) throws IOException {
        Requester<SectionsResponse> requester = new Requester<>(SectionsResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/projects/" + section.getProjectId() + "/sections" +
                "?opt_fields=name,project,project.name");

        SectionsResponse sectionsResponse = requester.requestGet(() -> url);
        return convertResponse(sectionsResponse);
    }

    @Override
    public void addTaskToSection(String sectionId, String taskId) throws IOException {
        Requester<AddTaskToSectionResponse> requester = new Requester<>(AddTaskToSectionResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/sections/" + sectionId + "/addTask");

        final AddTaskToSectionRequest request = new AddTaskToSectionRequest(new AddTaskToSectionData(taskId));
        requester.requestPost(() -> url, request);
    }

    private static List<CreatedSectionTO> convertResponse(SectionsResponse sectionsResponse) {
        return CollectionUtils.emptyIfNull(sectionsResponse.getData())
                .stream()
                .map(SectionServiceImpl::convertSectionData)
                .collect(Collectors.toUnmodifiableList());
    }

    private static CreatedSectionTO convertSectionData(SectionData createdSectionData) {
        return CreatedSectionTO.builder()
                .id(createdSectionData.getGid())
                .name(createdSectionData.getName())
                .project(createdSectionData.getProject())
                .build();
    }
}
