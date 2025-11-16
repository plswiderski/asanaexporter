package io.bitbucket.pablo127.asanaexporter.connector.attachment;

import io.bitbucket.pablo127.asanaexporter.model.AttachmentDescriptor;

import java.io.IOException;

public interface AttachmentService {
    AttachmentFile getAttachment(String attachmentId) throws IOException;
    AttachmentDescriptor uploadAttachment(AttachmentFile attachmentFile, String parentGid) throws IOException;
}
