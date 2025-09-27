package io.bitbucket.pablo127.asanaexporter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AttachmentDescriptor {
    private String gid;
    private String name;
    private String asanaUrl;
    private Long size;
}
