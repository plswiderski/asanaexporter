package io.bitbucket.pablo127.asanaexporter.connector.section;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SectionServiceFactory {

    public static SectionService create() {
        return new SectionServiceImpl();
    }
}
