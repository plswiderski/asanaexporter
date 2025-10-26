package io.bitbucket.pablo127.asanaexporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static final File RESULT_FILE = new File("asanaTasks.csv");
    static String personalAccessToken;

    public static void main(String[] args) {
        setDefaultUncaughtExceptionHandler();
        personalAccessToken = args[0];
        String workspaceName = args.length < 2 ? "Personal Projects" : args[1];
        Mode mode = args.length < 3 ? Mode.BACKUP : Mode.valueOf(args[2]);

        try {
            Processor processor;
            if (mode == Mode.BACKUP) {
                processor = new AsanaBackuper(workspaceName);
            } else if (mode == Mode.RESTORE) {
                processor = new AsanaRestorer(workspaceName);
            } else {
                throw new IllegalStateException("Unrecognized mode " + mode);
            }

            processor.process();
        } catch (Exception e) {
            logger.error("Error occurred while running AsanaExporter.", e);
        } finally {
            System.exit(0);
        }
    }

    private static void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, e) -> logger.error("Uncaught exception occurred in thread " + thread.getName(), e));
    }
}
