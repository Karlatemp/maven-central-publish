package moe.karla.maven.publishing.signsetup;

import java.util.Collections;
import java.util.Set;

public class SignSetupConfiguration {
    public String key;
    public String keyId;
    public String keyPassword;


    public transient Set<GpgFlags> flags = Collections.emptySet();
}
