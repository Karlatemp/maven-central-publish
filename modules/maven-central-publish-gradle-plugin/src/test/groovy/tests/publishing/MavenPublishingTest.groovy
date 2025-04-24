package tests.publishing

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static tests.publishing.SigningSetupTest.getConfig

class MavenPublishingTest {

    @Nested
    class Integration extends BaseIntegrationTest {

        @Test
        void testSingleProject() {
            settingsScript << """
rootProject.name = 'mptest'
"""
            mainScript << """
apply plugin: 'moe.karla.maven-publishing'
apply plugin: 'maven-publish'
apply plugin: 'java'

group = 'moe.karla.mptest'
version = '1.0.0'

repositories {
    mavenCentral()
}

mavenPublishing {
    publishingType = moe.karla.maven.publishing.MavenPublishingExtension.PublishingType.USER_MANAGED

    url = 'https://github.com/Karlatemp/MavenPublishingTest'
    developer('Karlatemp', 'kar@kasukusakura.com')
}

publishing {
    publications {
        main(MavenPublication) {
            from(project.components.java)
        }
    }
}
"""
            runner()
                    .withEnvironment(System.getenv() + [
                            'SIGNING_SETUP': getConfig(),
                    ])
                    .withArguments('publishAllPublicationsToMavenStageRepository', 'packMavenPublishingStage', '--stacktrace')
                    .forwardOutput()
                    .build()
        }

    }
}
