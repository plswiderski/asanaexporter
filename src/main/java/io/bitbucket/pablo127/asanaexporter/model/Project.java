package io.bitbucket.pablo127.asanaexporter.model;

import lombok.Data;

@Data
public class Project {
    private String id;
    private String gid;
    private String name;
    private String resource_type;
}