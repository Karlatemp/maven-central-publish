package tests.publishing

import moe.karla.maven.publishing.PublishingStubsSetupPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import java.nio.file.Files

class StubSetupTask {
    @Test
    void testPluginApply() {
        Project project = ProjectBuilder.builder().build()
        project.getPluginManager().apply(PublishingStubsSetupPlugin.class)

        Project nestedProject = ProjectBuilder.builder().withParent(project).build()

        Assertions.assertNotNull(project.tasks.findByName('createMavenPublishingDummyStubJavadoc'))
        Assertions.assertNull(nestedProject.tasks.findByName('createMavenPublishingDummyStubJavadoc'))
    }

    @Nested
    class Integration extends BaseIntegrationTest {
        @Test
        void testPluginApply() {
            mainScript << """
apply plugin: moe.karla.maven.publishing.PublishingStubsSetupPlugin
"""
            settingsScript << """
include(':nested')
"""
            Files.createDirectories(projectDir.resolve('nested')).resolve('build.gradle') << """
apply plugin: moe.karla.maven.publishing.PublishingStubsSetupPlugin
"""


            runner()
                    .withArguments('build', 'createMavenPublishingDummyStubJavadoc')
                    .build()
        }

        @Test
        void testPublishing() {
            settingsScript << """
rootProject.name = 'mptest'
"""
            mainScript << """
apply plugin: moe.karla.maven.publishing.PublishingStubsSetupPlugin
apply plugin: 'maven-publish'
apply plugin: 'java'

group = 'moe.karla.mptest'
version = '1.0.0'

publishing {
    publishing {
        repositories {
            maven {
                name = 'TestRepo'
                url = '${projectDir.resolve('repo').toUri()}'
            }
        }
    }
    publications {
        main(MavenPublication) {
            from(project.components.java)
        }
    }
}
"""
            runner()
                    .withArguments('publishAllPublicationsToTestRepoRepository')
                    .build()

            //Files.walk(projectDir.resolve('repo')).forEach { println it }
            Assertions.assertTrue(projectDir.resolve('repo/moe/karla/mptest/mptest/1.0.0/mptest-1.0.0-javadoc.jar').toFile().exists())
        }
    }
}
