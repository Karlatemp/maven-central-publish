package moe.karla.maven.publishing


import org.gradle.api.Plugin
import org.gradle.api.Project

class MavenPublishingPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        def rootProject = target.rootProject
        if (target != rootProject) {
            target.logger.warn('maven-publish-publish requires be applied on root project.')
            rootProject.apply plugin: MavenPublishingPlugin.class
            return
        }

        rootProject.apply plugin: PublishingStubsSetupPlugin.class
        rootProject.allprojects {
            apply plugin: SigningSetupPlugin.class
        }


        def ext = rootProject.extensions.create('mavenPublishing', MavenPublishingExtension.class)
        rootProject.afterEvaluate {
        }
    }
}
