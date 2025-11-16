package io.bitbucket.pablo127.asanaexporter.connector.project;

import io.bitbucket.pablo127.asanaexporter.model.Project;

import java.io.IOException;

public interface ProjectWriteService {
    Project createProject(Project project) throws IOException;
}
