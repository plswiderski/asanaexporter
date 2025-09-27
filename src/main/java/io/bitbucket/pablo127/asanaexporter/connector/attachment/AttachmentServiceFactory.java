package io.bitbucket.pablo127.asanaexporter.connector.attachment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachmentServiceFactory {

    public static AttachmentService create() {
        return new AttachmentServiceImpl();
    }
}
