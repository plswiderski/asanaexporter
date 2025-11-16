package io.bitbucket.pablo127.asanaexporter.connector.attachment;

import io.bitbucket.pablo127.asanaexporter.Requester;
import io.bitbucket.pablo127.asanaexporter.model.AttachmentDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

final class AttachmentServiceImpl implements AttachmentService {

    @Override
    public AttachmentFile getAttachment(String attachmentId) throws IOException {
        Requester<AttachmentResponse> requester = new Requester<>(AttachmentResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/attachments/" + attachmentId);

        AttachmentResponse response = requester.requestGet(() -> url);

        final AttachmentData attachmentData = response.getData();
        URL downloadUrl = new URL(attachmentData.getDownloadUrl());

        Requester<Requester.BytesArrayResult> bytesRequester = Requester.ofBytesRequester();
        Requester.BytesArrayResult bytesArrayResult = bytesRequester.requestGet(() -> downloadUrl);

        final AttachmentDescriptor attachmentDescriptor = createAttachmentDescriptor(attachmentData);
        return new AttachmentFile(bytesArrayResult.getBytes(), attachmentDescriptor);
    }

    @Override
    public AttachmentDescriptor uploadAttachment(AttachmentFile attachmentFile, String parentGid) throws IOException {
        Requester<AttachmentResponse> requester = new Requester<>(AttachmentResponse.class);
        URL url = new URL("https://app.asana.com/api/1.0/attachments?opt_fields=download_url,permanent_url,name");

        final Map<String, String> params = Map.of("parent", parentGid);
        Requester.BytesArrayResult body = new Requester.BytesArrayResult(attachmentFile.getContent(), attachmentFile.getAttachmentDescriptor().getName(), params);
        AttachmentResponse response = requester.requestPost(() -> url, body);
        return createAttachmentDescriptor(response.getData());
    }

    private static AttachmentDescriptor createAttachmentDescriptor(AttachmentData attachmentData) {
        return AttachmentDescriptor.builder()
                .gid(attachmentData.getGid())
                .name(attachmentData.getName())
                .asanaUrl(attachmentData.getPermanentUrl())
                .size(attachmentData.getSize())
                .build();
    }
}
