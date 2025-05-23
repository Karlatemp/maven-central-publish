package moe.karla.maven.publishing

import com.google.gson.Gson
import moe.karla.maven.publishing.signsetup.GpgFlags
import moe.karla.maven.publishing.signsetup.SignSetupConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension

import java.nio.file.Files
import java.nio.file.Paths

class SigningSetupPlugin implements Plugin<Project> {
    private static final Gson GSON = new Gson()

    private static SignSetupConfiguration loadConfiguration(Project project) {
        String signingManually = project.findProperty("signing.manually")
        if (signingManually == null) {
            return loadConfiguration0(project)
        }
        SignSetupConfiguration conf = loadConfiguration0(project)
        if (conf == null) {
            conf = new SignSetupConfiguration()
        }
        conf.flags = GpgFlags.of(signingManually)
        return conf
    }

    private static SignSetupConfiguration loadConfiguration0(Project project) {
        String envFile = System.getenv('SIGNING_SETUP_FILE')
        if (envFile != null) {
            try (def reader = Files.newBufferedReader(Paths.get(envFile))) {
                return GSON.fromJson(reader, SignSetupConfiguration.class)
            } catch (e) {
                throw new RuntimeException("Exception when loading setup file from SIGNING_SETUP_FILE", e)
            }
        }

        String envValue = System.getenv('SIGNING_SETUP')
        if (envValue != null) {
            try (def reader = new StringReader(envValue)) {
                return GSON.fromJson(reader, SignSetupConfiguration.class)
            } catch (e) {
                throw new RuntimeException("Exception when loading setup file from SIGNING_SETUP", e)
            }
        }

        String propFile = project.findProperty("signing.setup.file")
        if (propFile != null) {
            try (def reader = Files.newBufferedReader(Paths.get(propFile))) {
                return GSON.fromJson(reader, SignSetupConfiguration.class)
            } catch (e) {
                throw new RuntimeException("Exception when loading setup file from signing.setup.file", e)
            }
        }

        String propValue = project.findProperty("signing.setup")
        if (propValue != null) {
            try (def reader = new StringReader(propValue)) {
                return GSON.fromJson(reader, SignSetupConfiguration.class)
            } catch (e) {
                throw new RuntimeException("Exception when loading setup file from signing.setup", e)
            }
        }

        return null
    }

    @Override
    void apply(Project target) {
        def configuration = loadConfiguration(target)

        target.pluginManager.withPlugin('signing') {
            setup(target, configuration)
        }

        if (configuration == null) {
            target.logger.warn("Signing Setup Configuration not found. Please refer to https://github.com/Karlatemp/maven-central-publish#gpg-setup")
            target.logger.warn("Signatures are not automatically set unless you manually apply the signing plugin.")
            return
        }

        target.apply(plugin: 'signing')
    }

    static void setup(Project target, SignSetupConfiguration configuration) {
        def signingExt = target.extensions.getByName('signing') as SigningExtension
        Set<GpgFlags> flags = Collections.emptySet()
        if (configuration != null) {
            flags = configuration.flags
        }

        if (configuration != null && !flags.contains(GpgFlags.DISABLE_SIGNING_EXT_SETUP)) {
            if (flags.contains(GpgFlags.USE_GPG_CMD)) {
                signingExt.useGpgCmd()
            } else {
                signingExt.useInMemoryPgpKeys(configuration.keyId, configuration.key, configuration.keyPassword)
            }
        }

        /// Publications
        if (!flags.contains(GpgFlags.DISABLE_PUBLICATION_SIGNING)) {
            target.pluginManager.withPlugin('maven-publish') {
                def publishing = target.extensions.findByName('publishing') as PublishingExtension
                publishing.publications.configureEach { publication ->
                    signingExt.sign(publication)
                }
            }
        }

        /// Test task
        if (!flags.contains(GpgFlags.DISABLE_TEST_TASK)) {
            target.tasks.register('testSigning') {
                doLast {
                    temporaryDir.mkdirs()
                    def file = new File(temporaryDir, 'temp.txt')
                    try (def writer = new FileWriter(file)) {
                        writer.write(UUID.randomUUID().toString())
                    }
                    signingExt.sign(file).execute()
                    println("Signing test passed")
                }
            }
        }
    }
}
