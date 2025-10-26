package io.bitbucket.pablo127.asanaexporter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TaskMembership {
    private TaskShortSection section;
    private Project project;
}
