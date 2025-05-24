package moe.karla.maven.publishing

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

class PublishingStubsSetupPlugin implements Plugin<Project> {
    private static final String STUB_JAVADOC_CONFIGURATION_NAME = 'mavenPublishingDummyStubJavadoc'
    private static final String STUB_JAVADOC_TASK_NAME = 'createMavenPublishingDummyStubJavadoc'

    private static TaskProvider<Jar> getStubJavadocTask(Project target, String taskName) {
        try {
            return target.tasks.named(taskName, Jar.class)
        } catch (UnknownDomainObjectException ignored) {
        }

        return target.tasks.register(taskName, Jar.class) { Jar task ->
            task.group = 'publishing'
            task.archiveClassifier.set('javadoc')
            task.archiveBaseName.set('maven-publishing-stub-' + taskName)
            task.archiveVersion.set('')
            task.destinationDirectory.set(target.layout.buildDirectory.dir("tmp/mavenPublishingStubJavadocJars"))
        }
    }

    private static NamedDomainObjectProvider<Configuration> createStubJavadocConfiguration(Project target) {
        def configurations = target.configurations
        try {
            return configurations.named(STUB_JAVADOC_CONFIGURATION_NAME)
        } catch (UnknownDomainObjectException ignored) {
        }

        def stubTask = getStubJavadocTask(target, STUB_JAVADOC_TASK_NAME)

        return configurations.register(STUB_JAVADOC_CONFIGURATION_NAME) { Configuration configuration ->
            configuration.outgoing {
                artifact(stubTask)
            }
            configuration.attributes {
                attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage.class, "java-runtime"))
                attribute(Category.CATEGORY_ATTRIBUTE, target.objects.named(Category.class, "documentation"))
                attribute(Bundling.BUNDLING_ATTRIBUTE, target.objects.named(Bundling.class, "external"))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, target.objects.named(DocsType.class, DocsType.JAVADOC))
            }
        }
    }

    @Override
    void apply(Project target) {
        target.pluginManager.withPlugin('java') {
            // setup sourcesJar & javadocs
            def javaExt = target.extensions.findByName('java') as JavaPluginExtension

            javaExt.withSourcesJar()
            if (!target.tasks.names.contains('javadocJar')) {
                def stubConfig = createStubJavadocConfiguration(target)
                target.components.named("java", AdhocComponentWithVariants.class).configure { AdhocComponentWithVariants javaComponent ->
                    javaComponent.addVariantsFromConfiguration(stubConfig.get()) {
                        it.mapToMavenScope("runtime")
                        it.mapToOptional()
                    }
                }
            }
        }


        target.pluginManager.withPlugin('org.jetbrains.kotlin.multiplatform') {
            target.pluginManager.withPlugin('maven-publish') {

                def publishing = target.extensions.findByName('publishing') as PublishingExtension
                publishing.publications.withType(MavenPublication.class).configureEach { publication ->
                    publication.artifact(getStubJavadocTask(target, 'stubJavadocJarForPublication' + publication.name.capitalize())) { art ->
                        art.classifier = 'javadoc'
                    }
                }
            }
        }
    }
}
