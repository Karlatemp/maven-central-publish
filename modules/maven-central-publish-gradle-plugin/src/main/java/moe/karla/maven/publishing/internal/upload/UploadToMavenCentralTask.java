package moe.karla.maven.publishing.internal.upload;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

public abstract class UploadToMavenCentralTask extends DefaultTask {
    @Input
    @Optional
    public abstract Property<String> getUserName();

    @Input
    @Optional
    public abstract Property<String> getPassword();

    @Input
    public abstract Property<String> getPublishingType();

    @Input
    public abstract Property<String> getPublishingName();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getUploadFile();

    @Internal
    public abstract ConfigurableFileCollection getWorkerClasspath();

    @Inject
    public abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public void upload() {
        WorkQueue workQueue = getWorkerExecutor().classLoaderIsolation(setup -> {
            setup.getClasspath().from(getWorkerClasspath());
        });

        workQueue.submit(UploadToMavenCentralAction.class, param -> {
            param.getUserName().set(getUserName());
            param.getPassword().set(getPassword());
            param.getPublishingType().set(getPublishingType());
            param.getPublishingName().set(getPublishingName());
            param.getUploadFile().set(getUploadFile());
        });
    }
}
