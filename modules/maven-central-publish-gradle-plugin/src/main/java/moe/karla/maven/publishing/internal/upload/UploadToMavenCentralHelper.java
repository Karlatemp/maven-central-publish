package moe.karla.maven.publishing.internal.upload;

import org.gradle.api.artifacts.repositories.PasswordCredentials;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UploadToMavenCentralHelper {
    public static void setupPasswordCredentials(PasswordCredentials credentials, String userName, String password) {
        if (password == null || password.isEmpty()) {
            return;
        }

        int idx = password.indexOf(':');
        if (idx != -1) {
            credentials.setUsername(password.substring(0, idx));
            credentials.setPassword(password.substring(idx + 1));
            return;
        }

        credentials.setUsername(userName);
        credentials.setPassword(password);
    }


    private static byte[] getPasswd(String userName, String password) {
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Password is not specify.");
        }

        if (password.contains(":")) {
            return password.getBytes(StandardCharsets.UTF_8);
        }

        if (userName == null || userName.isEmpty()) {
            throw new RuntimeException("Unable to parse authentication credentials");
        }

        return (userName + ":" + password).getBytes(StandardCharsets.UTF_8);
    }

    public static String getAuthorizationToken(String userName, String password) {
        return "Bearer " + Base64.getEncoder().encodeToString(
                getPasswd(userName, password)
        );
    }
}
