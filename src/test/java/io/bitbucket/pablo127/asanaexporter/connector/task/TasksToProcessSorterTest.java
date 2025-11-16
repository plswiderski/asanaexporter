package io.bitbucket.pablo127.asanaexporter.connector.task;

import io.bitbucket.pablo127.asanaexporter.model.Parent;
import io.bitbucket.pablo127.asanaexporter.model.TaskShort;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TasksToProcessSorterTest {
    private static final Pattern PARENT_PATTERN = Pattern.compile("[(][0-9]+[)]");

    @ParameterizedTest
    @MethodSource("provideCompareArguments")
    void sort(String givenTasksRepresentation, String expectedTasksRepresentation) {
        List<TaskShort> tasks = generateTasks(givenTasksRepresentation);
        List<TaskShort> expectedTasks = generateTasks(expectedTasksRepresentation);

        List<TaskShort> sortedTasks = new TasksToProcessSorter().sort(tasks);

        assertEquals(expectedTasks, sortedTasks, () -> "Actual tasks order: " + sortedTasks.stream()
                .map(taskShort -> taskShort.getGid() +
                        (taskShort.getParent() != null ? "(" + taskShort.getParent().getGid() + ")" : ""))
                .collect(Collectors.joining(", ")));

    }

    private static Stream<Arguments> provideCompareArguments() {
        final int sizeOfElements = 30000;
        String givenMany = IntStream.range(0, sizeOfElements)
                .mapToObj(operand -> operand != sizeOfElements - 1 ? operand + "(" + (operand + 1) + ")" : operand + "")
                .collect(Collectors.joining(", "));

        String expectedMany = IntStream.range(0, sizeOfElements)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .map(operand -> operand != sizeOfElements - 1 ? operand + "(" + (operand + 1) + ")" : operand + "")
                .collect(Collectors.joining(", "));

        return Stream.of(
                Arguments.of("1, 2, 3, 4, 5", "1, 2, 3, 4, 5"),
                Arguments.of("1(5), 2, 3, 4, 5", "2, 3, 4, 5, 1(5)"),
                Arguments.of("1(5), 2(5), 3(5), 4(5), 5", "5, 1(5), 2(5), 3(5), 4(5)"),
                Arguments.of("1, 2(5), 3(4), 4(5), 5", "1, 5, 2(5), 4(5), 3(4)"),
                Arguments.of("2(5), 3(4), 4(5), 5, 1", "5, 1, 2(5), 4(5), 3(4)"),
                Arguments.of("2(4), 3(5), 4(3), 5(1), 1", "1, 5(1), 3(5), 4(3), 2(4)"),
                Arguments.of("2(4), 3(5), 5(1), 1, 56", "1, 56, 2(4), 5(1), 3(5)"),
                Arguments.of("2(4), 3(5), 5(1), 1(99), 56(57)", "2(4), 1(99), 5(1), 56(57), 3(5)"),
                Arguments.of(givenMany, expectedMany));
    }

    /*
    ParentId is given in the bracket.
     */
    private List<TaskShort> generateTasks(String input) {
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .map(s -> {
                    TaskShort taskShort = new TaskShort();

                    String taskId = s;
                    final Matcher matcher = PARENT_PATTERN.matcher(s);
                    if (matcher.find()) {
                        String bracket = matcher.group(0);
                        String parentId = bracket.substring(1, bracket.length() - 1);
                        taskShort.setParent(new Parent(parentId, parentId));

                        taskId = taskId.substring(0, taskId.indexOf(bracket));
                    }

                    taskShort.setGid(taskId);
                    return taskShort;
                })
                .collect(Collectors.toUnmodifiableList());
    }
}