package tests.publishing

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path

class BaseIntegrationTest {
    @TempDir
    Path projectDir

    Path mainScript, settingsScript

    @BeforeEach
    void setup() {
        Files.createDirectories(projectDir)

        mainScript = projectDir.resolve("build.gradle")
        settingsScript = projectDir.resolve("settings.gradle")

        mainScript << """
plugins {
    id 'moe.karla.maven-publishing' apply false
}
"""
    }

    @AfterEach
    void cleanup() {
        projectDir.deleteDir()
    }

    GradleRunner runner() {
        return GradleRunner.create()
                // .forwardOutput()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
    }
}
