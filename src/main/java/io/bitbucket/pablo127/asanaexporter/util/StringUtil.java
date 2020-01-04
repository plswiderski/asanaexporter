package io.bitbucket.pablo127.asanaexporter.util;

import java.util.function.Function;

public final class StringUtil {

    private StringUtil() {
    }

    public static String emptyIfNullOrTransform(String input, Function<String, String> function) {
        return input == null ? "" : function.apply(input);
    }
}
