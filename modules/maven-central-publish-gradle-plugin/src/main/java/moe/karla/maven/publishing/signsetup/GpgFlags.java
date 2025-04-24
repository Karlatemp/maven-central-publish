package moe.karla.maven.publishing.signsetup;

import java.util.*;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public enum GpgFlags {
    DISABLE_TEST_TASK("notest"),
    DISABLE_SIGNING_EXT_SETUP("nouse"),
    DISABLE_PUBLICATION_SIGNING("nopub"),
    USE_GPG_CMD("gpgcmd"),
    ;

    final String flag;

    GpgFlags(String flag) {
        this.flag = flag;
    }

    private static final Map<String, Collection<GpgFlags>> ALIAS = new HashMap<>();

    static {
        for (GpgFlags flag : GpgFlags.values()) {
            ALIAS.put(flag.flag, Collections.singleton(flag));
        }
        ALIAS.put("true", Arrays.asList(USE_GPG_CMD));
        ALIAS.put("all", EnumSet.allOf(GpgFlags.class));
        ALIAS.put("", Arrays.asList(DISABLE_SIGNING_EXT_SETUP));
    }

    public static Set<GpgFlags> of(String opts) {
        EnumSet<GpgFlags> flags = EnumSet.noneOf(GpgFlags.class);
        for (String sub : opts.split(",")) {
            flags.addAll(ALIAS.getOrDefault(sub, Collections.emptySet()));
        }
        return flags;
    }
}
