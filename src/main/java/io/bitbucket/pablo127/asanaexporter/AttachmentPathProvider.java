package io.bitbucket.pablo127.asanaexporter;

import java.io.File;
import java.util.function.BiFunction;

public final class AttachmentPathProvider implements BiFunction<String, String, File> {
    private static final File ATTACHMENTS_RESULT_FILE = new File("attachments");

    @Override
    public File apply(String taskGid, String fileName) {
        final File taskDir = new File(ATTACHMENTS_RESULT_FILE, taskGid);
        taskDir.mkdirs();
        return new File(taskDir, fileName);
    }

    public File getBaseDir() {
        return ATTACHMENTS_RESULT_FILE;
    }
}
