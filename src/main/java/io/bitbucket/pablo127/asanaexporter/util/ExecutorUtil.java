package io.bitbucket.pablo127.asanaexporter.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutorUtil {

    public static ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(17,
                17,
                5000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>());
    }
}
