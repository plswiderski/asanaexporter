package io.bitbucket.pablo127.asanaexporter.model;

import lombok.Data;

import java.util.List;

@Data
public class ProjectRequest {
    private List<Project> data;
}
