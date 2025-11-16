package io.bitbucket.pablo127.asanaexporter.connector.section;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SectionManagerFactory {

    public static SectionManager create() {
        final SectionService sectionService = SectionServiceFactory.create();
        return new SectionManagerImpl(sectionService);
    }
}
