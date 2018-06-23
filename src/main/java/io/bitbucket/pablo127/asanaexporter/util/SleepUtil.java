package io.bitbucket.pablo127.asanaexporter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SleepUtil {

    private static final Logger logger = LoggerFactory.getLogger(SleepUtil.class);

    private SleepUtil() {
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error("Sleep was interrupted.", e);
        }
    }
}
