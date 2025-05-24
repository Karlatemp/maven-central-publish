package moe.karla.maven.publishing

import moe.karla.maven.publishing.internal.GitBaseValueSource
import moe.karla.maven.publishing.signsetup.SignSetupConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

class MavenPublishingPlugin implements Plugin<Project> {

    @Override
    void apply(Project target) {
        def rootProject = target.rootProject
        if (target != rootProject) {
            target.logger.warn('maven-publish-publish requires be applied on root project.')
            rootProject.apply plugin: MavenPublishingPlugin.class
            return
        }
        rootProject.pluginManager.apply(BasePlugin.class)

        rootProject.allprojects {
            apply plugin: SigningSetupPlugin.class
        }

        def cacheRepoLocation = rootProject.layout.buildDirectory.get()
                .dir('maven-publishing-stage')
                .asFile


        def ext = rootProject.extensions.create('mavenPublishing', MavenPublishingExtension.class)
        rootProject.afterEvaluate {
            if (!ext.manuallyPomSetup) {
                initializeMissingProperties(rootProject, ext)
            }


            rootProject.allprojects {
                if (ext.automaticSourcesAndJavadoc) {
                    apply plugin: PublishingStubsSetupPlugin.class
                }

                pluginManager.withPlugin('maven-publish') {
                    def currentProject = project
                    def publishing = currentProject.extensions.findByName('publishing') as PublishingExtension

                    if (!ext.manuallyPomSetup) {
                        initializeProjectPomContents(currentProject, ext)
                    }

                    publishing.repositories {
                        maven {
                            name = 'MavenStage'
                            url = cacheRepoLocation.toURI()
                        }
                    }
                }
            }
        }


        def cleanTask = rootProject.tasks.register('cleanMavenPublishingStage') {
            doLast { cacheRepoLocation.deleteDir() }
        }
        rootProject.tasks.clean.dependsOn(cleanTask)

        def packBundleTask = rootProject.tasks.register('packMavenPublishingStage', Zip.class) {
            destinationDirectory.set(temporaryDir)
            archiveFileName.set('bundle.zip')

            from(cacheRepoLocation)
        }

        def dependencies = [
                "org.apache.httpcomponents:httpclient:4.5.13",
                "org.apache.httpcomponents:httpmime:4.5.13",
        ]
        def externalTaskConfiguration = rootProject.configurations.create('mavenPublishingExternalModuleClasspath')
        dependencies.forEach { externalTaskConfiguration.dependencies.add(rootProject.dependencies.create(it)) }
        def jarMe = findJarMe()


        rootProject.tasks.register('publishToMavenCentral', JavaExec.class) { exec ->
            group = 'publishing'
            dependsOn(packBundleTask)
            inputs.files(packBundleTask.get().outputs.files)

            classpath = externalTaskConfiguration
            if (jarMe != null) {
                classpath = classpath + exec.project.files(jarMe)
            }
            mainClass.set('moe.karla.maven.publishing.advtask.UploadToMavenCentral')

            args(packBundleTask.get().outputs.files.singleFile.absolutePath)

            def envs = [
                    'MAVEN_PUBLISH_USER'           : exec.project.providers.environmentVariable('MAVEN_PUBLISH_USER')
                            .orElse(exec.project.providers.gradleProperty('maven.publish.user'))
                            .orElse(exec.project.providers.systemProperty('maven.publish.user'))
                            .orElse(''),
                    'MAVEN_PUBLISH_PASSWORD'       : exec.project.providers.environmentVariable('MAVEN_PUBLISH_PASSWORD')
                            .orElse(exec.project.providers.gradleProperty('maven.publish.password'))
                            .orElse(exec.project.providers.systemProperty('maven.publish.password'))
                            .orElse(''),

                    'MAVEN_PUBLISH_PUBLISHING_NAME': exec.project.provider { exec.project.name },
                    'MAVEN_PUBLISH_PUBLISHING_TYPE': exec.project.provider { ext.publishingType.name() },
            ]

            doFirst {
                envs.forEach { key, value ->
                    exec.environment(key, value.get())
                }
            }
        }
    }

    static File findJarMe() {
        def pd = SignSetupConfiguration.class.protectionDomain
        if (pd == null) return null
        def cs = pd.codeSource
        if (cs == null) return null
        def loc = cs.location
        if (loc == null) return null
        if (loc.protocol == "file") {
            return Paths.get(loc.toURI()).toFile()
        }
        return null
    }

    private static String cmdGit(Project proj, String... args) {
        return proj.providers.of(GitBaseValueSource.class) {
            it.parameters.getArgs().addAll(args)
        }.get()
    }

    private static void initializeMissingProperties(Project rootProject, MavenPublishingExtension ext) {
        if (ext.url == null || ext.url.isEmpty()) {
            def remote = cmdGit(rootProject, 'remote', 'get-url', 'origin').trim()

            while (true) {
                def githubMatcher = Pattern.compile("(?:git@github.com:|https://github.com/)(.+?)(?:\\.git)?").matcher(remote)
                if (githubMatcher.matches()) {
                    ext.url = "https://github.com/" + githubMatcher.group(1)
                    break
                }

                ext.url = remote
                break
            }
        }
        if (ext.scmUrl == null || ext.scmUrl.isEmpty()) {
            ext.scmUrl = ext.url
        }
        if (ext.scmConnection == null || ext.scmConnection.isEmpty()) {
            ext.scmConnection = "scm:git:" + ext.url
        }
        if (ext.scmDeveloperConnection == null || ext.scmDeveloperConnection.isEmpty()) {
            ext.scmDeveloperConnection = "scm:git:" + ext.url
        }
        if (ext.developers == null || ext.developers.isEmpty()) {
            def lastCommitAuthor = cmdGit(rootProject, 'show', '--format=%an<%ae>', 'HEAD').trim()
            def matcher = Pattern.compile("(.+)<(.+)>").matcher(lastCommitAuthor)

            if (matcher.matches()) {
                ext.developer(matcher.group(1), matcher.group(2))
            } else {
                throw new RuntimeException("Failed to resolve developer information from last commit with " + lastCommitAuthor)
            }
        }
        if (ext.licenses == null || ext.licenses.isEmpty()) {
            def licenseFile = rootProject.file('LICENSE')
            String name = 'UNLICENSE'
            if (licenseFile.exists()) {
                def firstLine = Files.readAllLines(licenseFile.toPath())
                        .stream()
                        .map { it.trim() }
                        .filter { !it.isEmpty() }
                        .findFirst()
                if (firstLine.present) {
                    name = firstLine.get()
                }
            }

            ext.license(name, ext.url)
        }
    }

    private static void initializeProjectPomContents(Project currentProject, MavenPublishingExtension ext) {
        if (currentProject.description == null || currentProject.description.isEmpty()) {
            currentProject.description = currentProject.name
        }

        def publishing = currentProject.extensions.findByName('publishing') as PublishingExtension
        publishing.publications.withType(MavenPublication.class).configureEach {
            pom {
                name.set(artifactId)
                description.set(currentProject.description)

                url.set(ext.url)
                licenses {
                    ext.licenses.forEach { lInfo ->
                        license {
                            name.set(lInfo.name)
                            url.set(lInfo.url)
                        }
                    }
                }
                developers {
                    ext.developers.forEach { dInfo ->
                        developer {
                            name.set(dInfo.name)
                            email.set(dInfo.email)
                            if (dInfo.organization != null) organization.set(dInfo.organization)
                            if (dInfo.organizationUrl != null) organization.set(dInfo.organizationUrl)
                        }
                    }
                }
                scm {
                    url.set(ext.scmUrl)
                    connection.set(ext.scmConnection)
                    developerConnection.set(ext.scmDeveloperConnection)
                }
            }
        }
    }
}
