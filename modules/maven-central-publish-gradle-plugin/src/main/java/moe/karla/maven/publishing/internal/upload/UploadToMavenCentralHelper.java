package moe.karla.maven.publishing.internal.upload;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UploadToMavenCentralHelper {

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
