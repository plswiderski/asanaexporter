package io.bitbucket.pablo127.asanaexporter.connector.attachment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
class AttachmentData {
    private String gid;
    private String name;

    @JsonProperty("download_url")
    private String downloadUrl;

    @JsonProperty("permanent_url")
    private String permanentUrl;

    private Long size;
}
