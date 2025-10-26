package io.bitbucket.pablo127.asanaexporter.connector.task;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SearchTasksRequest {
    private final String text;
}
