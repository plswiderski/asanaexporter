package io.bitbucket.pablo127.asanaexporter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Parent {
    private String gid;

    private String name;
}
