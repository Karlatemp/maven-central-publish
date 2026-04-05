package moe.karla.maven.publishing.internal.upload;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

public abstract class UploadToMavenCentralAction implements WorkAction<UploadToMavenCentralAction.Param> {

    public interface Param extends WorkParameters {
        @Input
        @Optional
        Property<String> getUserName();

        @Input
        @Optional
        Property<String> getPassword();

        @Input
        Property<String> getPublishingType();

        @Input
        Property<String> getPublishingName();

        @Input
        RegularFileProperty getUploadFile();
    }


    @Override
    public void execute() {

        Param param = getParameters();

        try (CloseableHttpClient httpclient = HttpClientBuilder.create().build()) {

            HttpPost httpPost = new HttpPost(
                    "https://central.sonatype.com/api/v1/publisher/upload?publishingType=" + param.getPublishingType().get()
                            + "&name=" + URLEncoder.encode(param.getPublishingName().get(), "UTF-8")
            );
            httpPost.addHeader("Authorization", UploadToMavenCentralHelper.getAuthorizationToken(
                    param.getUserName().getOrNull(),
                    param.getPassword().getOrNull()
            ));

            httpPost.setEntity(
                    MultipartEntityBuilder.create()
                            .addBinaryBody("bundle", param.getUploadFile().get().getAsFile())
                            .build()
            );

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() / 100 != 2) {
                    System.err.println("HTTP Post Exited with " + response.getStatusLine().getStatusCode());
                    if (response.getEntity() != null) {
                        InputStream content = response.getEntity().getContent();
                        if (content != null) {
                            byte[] buffer = new byte[1024];
                            while (true) {
                                int len = content.read(buffer);
                                if (len == -1) break;

                                System.err.write(buffer, 0, len);
                            }
                        }
                    }
                    throw new IllegalStateException("HTTP Post Exited with " + response.getStatusLine().getStatusCode());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
