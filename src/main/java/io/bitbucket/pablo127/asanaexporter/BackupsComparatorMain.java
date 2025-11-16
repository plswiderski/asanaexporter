package io.bitbucket.pablo127.asanaexporter;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.Project;
import io.bitbucket.pablo127.asanaexporter.model.Recurrence;
import io.bitbucket.pablo127.asanaexporter.model.RecurrenceData;
import io.bitbucket.pablo127.asanaexporter.model.TaskAssigneeSection;
import io.bitbucket.pablo127.asanaexporter.model.TaskAttachment;
import io.bitbucket.pablo127.asanaexporter.model.TaskMembership;
import io.bitbucket.pablo127.asanaexporter.model.TaskShortSection;
import io.bitbucket.pablo127.asanaexporter.util.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Util main that compares 2 backups. It can be used to verify that restore was done properly. Compare basic `asanaTasks.csv`
 * backup file with `asanaTasks.csv` backup file exported after restore on workspace used in the restore process.
 */
public class BackupsComparatorMain {

    private static final Logger logger = LoggerFactory.getLogger(BackupsComparatorMain.class);

    private final static Set<String> IGNORED_COLUMNS = Set.of("id", "createdAt", "completedAt", "modifiedAt");

    private final static Map<String, BiFunction<String, Map<String, String>, String>> MAPPINGS = Map.of(
            "projects", new BaseFunctionMapping(new ProjectsMapping()),
            "notes", new NotesMapping(),
            "sections", new BaseFunctionMapping(new SectionsMapping()),
            "assigneeSection", new BaseFunctionMapping(new AssigneeSectionMapping()),
            "parentTask", new BaseFunctionMapping(new ParentTaskMapping()),
            "recurrence", new BaseFunctionMapping(new RecurrenceMapping()),
            "attachments", new BaseFunctionMapping(new AttachmentsMapping()),
            "name", (text, u) -> text.trim());

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            logger.error("Pass paths to csv files that you want to compare. First pass path to the base one - file that restore was based on.");
            return;
        }

        File baseCsvFile = new File(args[0]);
        File currentCsvFile = new File(args[1]);
        logger.info("Start comparing backups from paths base: '{}', current: '{}'", baseCsvFile, currentCsvFile);
        if (!baseCsvFile.exists() || !currentCsvFile.exists()) {
            logger.error("Paths passed for csv files are incorrect. Files do not exist.");
            return;
        }

        List<Map<String, String>> current = loadRows(currentCsvFile.toPath());
        List<Map<String, String>> basic = loadRows(baseCsvFile.toPath());

        if (current.size() != basic.size()) {
            logger.error(String.format("Number of rows is not equal. Current: '%d', basic: '%d'", current.size(), basic.size()));
        }

        final Function<String, String> taskIdExtractor = notes -> {
            final int taskIdIndex = notes.indexOf("TaskId:");
            if (taskIdIndex == -1)
                return notes;

            String startWithId = notes.substring(taskIdIndex + 8);
            return startWithId.substring(0, startWithId.indexOf("\n"));
        };

        current = current.stream()
                .sorted((o1, o2) -> {
                    String name1 = o1.get("name").trim();
                    String name2 = o2.get("name").trim();

                    int compare = name1.compareTo(name2);
                    if (compare == 0) {
                        String notes1 = o1.get("notes");
                        String notes2 = o2.get("notes");

                        final String taskId1 = taskIdExtractor.apply(notes1);
                        final String taskId2 = taskIdExtractor.apply(notes2);
                        return taskId1.compareTo(taskId2);
                    }

                    return compare;
                })
                .collect(Collectors.toUnmodifiableList());

        basic = basic.stream()
                .sorted((o1, o2) -> {
                    String name1 = o1.get("name").trim();
                    String name2 = o2.get("name").trim();

                    int compare = name1.compareTo(name2);
                    if (compare == 0) {
                        return o1.get("id").compareTo(o2.get("id"));
                    }

                    return compare;
                })
                .collect(Collectors.toUnmodifiableList());

        for (int i = 0; i < Math.min(current.size(), basic.size()); i++) {
            final Map<String, String> currentOriginal = current.get(i);
            Map<String, String> rowCurrent = processMap(currentOriginal);

            final Map<String, String> basicOriginal = basic.get(i);
            Map<String, String> rowBasic = processMap(basicOriginal);

            if (!rowCurrent.equals(rowBasic)) {
                logger.error("Row #" + i + " not equal. Current: " + System.lineSeparator() + currentOriginal + System.lineSeparator()
                        + "basic: " + System.lineSeparator() + basicOriginal);
            }
        }

        logger.info("BackupsComparator finished.");
    }

    private static Map<String, String> processMap(Map<String, String> map) {
        return map.entrySet()
                .stream()
                .filter(e -> !IGNORED_COLUMNS.contains(e.getKey()))
                .map(entry -> Optional.ofNullable(MAPPINGS.get(entry.getKey()))
                        .map(mappingFunction -> mappingFunction.apply(entry.getValue(), map))
                        .map(newValue -> Map.entry(entry.getKey(), newValue))
                        .orElse(entry))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<Map<String, String>> loadRows(Path path) throws IOException {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        try (CSVReader csvReader = createCsvReader(path)) {
            boolean header = true;
            for (String[] columns : csvReader) {
                if (header) {
                    headers.addAll(Arrays.stream(columns)
                            .collect(Collectors.toUnmodifiableList()));
                    header = false;
                    continue;
                }

                final List<String> row = Arrays.stream(columns)
                        .collect(Collectors.toUnmodifiableList());
                rows.add(row);
            }
        }

        return rows.stream()
                .map(strings -> {
                    AtomicInteger index = new AtomicInteger(0);
                    return strings.stream()
                            .collect(Collectors.toUnmodifiableMap(o -> headers.get(index.getAndIncrement()), o -> o));
                })
                .collect(Collectors.toUnmodifiableList());
    }

    private static CSVReader createCsvReader(Path path) throws IOException {
        CSVParser csvParser = new CSVParserBuilder()
                .withSeparator(';')
                .build();

        Reader reader = new InputStreamReader(Files.newInputStream(path));
        return new CSVReaderBuilder(reader)
                .withCSVParser(csvParser)
                .build();
    }

    @RequiredArgsConstructor
    private static final class BaseFunctionMapping implements BiFunction<String, Map<String, String>, String> {

        private final Function<String, String> mappingFunction;

        @Override
        public String apply(String text, Map<String, String> allProperties) {
            return mappingFunction.apply(text);
        }
    }


    private static final class ProjectsMapping implements Function<String, String> {

        @Override
        public String apply(String text) {
            if (StringUtils.isEmpty(text)) {
                return text;
            }

            try {
                CollectionType listProjectJavaType = JsonMapper.INSTANCE.getTypeFactory().constructCollectionType(List.class, Project.class);
                List<Project> projects = JsonMapper.INSTANCE.readValue(text, listProjectJavaType);

                List<Project> projectsToSave = projects.stream()
                        .map(project -> {
                            Project projectToSave = new Project();
                            projectToSave.setName(project.getName());

                            return projectToSave;
                        })
                        .sorted(Comparator.comparing(Project::getName))
                        .collect(Collectors.toUnmodifiableList());

                return JsonMapper.INSTANCE.writeValueAsString(projectsToSave);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class NotesMapping implements BiFunction<String, Map<String, String>, String> {

        @Override
        public String apply(String text, Map<String, String> allProperties) {
            if (StringUtils.isEmpty(text)) {
                return text;
            }

            String onlyMessage = text;
            int startOfPropertiesInfo = text.indexOf("\n---\nTask restored from backup. Original properties:");
            if (startOfPropertiesInfo != -1) {
                onlyMessage = text.substring(0, startOfPropertiesInfo);
            }

            final String attachments = allProperties.get("attachments");
            if (StringUtils.isNotEmpty(attachments)) {
                try {
                    CollectionType listTaskAttachmentJavaType = JsonMapper.INSTANCE.getTypeFactory()
                            .constructCollectionType(List.class, TaskAttachment.class);
                    List<TaskAttachment> taskAttachments = JsonMapper.INSTANCE.readValue(attachments, listTaskAttachmentJavaType);

                    Set<String> attachmentIds = taskAttachments.stream()
                            .map(TaskAttachment::getGid)
                            .collect(Collectors.toUnmodifiableSet());

                    for (String attachmentId : attachmentIds) {
                        onlyMessage = onlyMessage.replace(attachmentId, "{attachmentId}");
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            return onlyMessage;
        }
    }

    private static final class SectionsMapping implements Function<String, String> {

        @Override
        public String apply(String text) {
            if (StringUtils.isEmpty(text)) {
                return text;
            }

            try {
                CollectionType listTaskMembershipJavaType = JsonMapper.INSTANCE.getTypeFactory()
                        .constructCollectionType(List.class, TaskMembership.class);
                List<TaskMembership> taskMemberships = JsonMapper.INSTANCE.readValue(text, listTaskMembershipJavaType);

                List<TaskMembership> taskMembershipsToSave = taskMemberships.stream()
                        .map(taskMembership -> {
                            TaskShortSection taskSection = taskMembership.getSection();
                            TaskShortSection taskSectionToSave = new TaskShortSection(null, taskSection.getName());

                            Project taskProject = taskMembership.getProject();
                            Project taskProjectToSave = new Project();
                            taskProjectToSave.setName(taskProject.getName());

                            return new TaskMembership(taskSectionToSave, taskProjectToSave);
                        })
                        .sorted((o1, o2) -> {
                            int compare = o1.getSection().getName().compareTo(o2.getSection().getName());
                            if (compare == 0) {
                                return o1.getProject().getName().compareTo(o2.getProject().getName());
                            }

                            return compare;
                        })
                        .collect(Collectors.toUnmodifiableList());

                return JsonMapper.INSTANCE.writeValueAsString(taskMembershipsToSave);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class AssigneeSectionMapping implements Function<String, String> {

        @Override
        public String apply(String text) {
            if (StringUtils.isEmpty(text)) {
                return text;
            }

            try {
                TaskAssigneeSection taskAssigneeSection = JsonMapper.INSTANCE.readValue(text, TaskAssigneeSection.class);
                taskAssigneeSection.setGid(null);

                return JsonMapper.INSTANCE.writeValueAsString(taskAssigneeSection);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class ParentTaskMapping implements Function<String, String> {

        @Override
        public String apply(String text) {
            if (StringUtils.isEmpty(text)) {
                return text;
            }

            try {
                Parent parent = JsonMapper.INSTANCE.readValue(text, Parent.class);
                parent.setGid(null);
                parent.setName(parent.getName().trim());

                return JsonMapper.INSTANCE.writeValueAsString(parent);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class RecurrenceMapping implements Function<String, String> {

        @Override
        public String apply(String text) {
            if (StringUtils.isEmpty(text)) {
                return text;
            }

            try {
                Recurrence recurrence = JsonMapper.INSTANCE.readValue(text, Recurrence.class);
                if (recurrence.getData() == null) {
                    recurrence.setData(new RecurrenceData());
                }

                recurrence.getData().setOriginalDueDateTimestamp(null);

                return JsonMapper.INSTANCE.writeValueAsString(recurrence);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class AttachmentsMapping implements Function<String, String> {

        @Override
        public String apply(String text) {
            if (StringUtils.isEmpty(text)) {
                return text;
            }

            try {
                CollectionType listTaskAttachmentJavaType = JsonMapper.INSTANCE.getTypeFactory()
                        .constructCollectionType(List.class, TaskAttachment.class);
                List<TaskAttachment> taskAttachments = JsonMapper.INSTANCE.readValue(text, listTaskAttachmentJavaType);

                taskAttachments.forEach(taskAttachment -> taskAttachment.setGid(null));

                return JsonMapper.INSTANCE.writeValueAsString(taskAttachments);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
