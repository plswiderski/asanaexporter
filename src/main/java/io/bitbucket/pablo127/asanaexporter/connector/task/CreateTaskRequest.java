package io.bitbucket.pablo127.asanaexporter.connector.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
class CreateTaskRequest {
    private TaskToCreateTO data;
}
