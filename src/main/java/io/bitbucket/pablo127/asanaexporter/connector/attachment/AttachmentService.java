package io.bitbucket.pablo127.asanaexporter.connector.attachment;

import java.io.IOException;

public interface AttachmentService {
    AttachmentFile getAttachment(String attachmentId) throws IOException;
}
