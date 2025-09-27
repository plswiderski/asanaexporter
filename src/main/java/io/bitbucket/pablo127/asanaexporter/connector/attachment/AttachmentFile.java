package io.bitbucket.pablo127.asanaexporter.connector.attachment;

import io.bitbucket.pablo127.asanaexporter.model.AttachmentDescriptor;
import lombok.Data;

@Data
public final class AttachmentFile {
    private final byte[] content;
    private final AttachmentDescriptor attachmentDescriptor;
}
