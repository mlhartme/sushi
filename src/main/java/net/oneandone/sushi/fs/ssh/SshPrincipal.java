package net.oneandone.sushi.fs.ssh;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;

public class SshPrincipal implements UserPrincipal, GroupPrincipal {
    public final int id;

    public SshPrincipal(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return Integer.toString(id);
    }

    public boolean equals(Object obj) {
        if (obj instanceof SshPrincipal) {
            return id == ((SshPrincipal) obj).id;
        }
        return false;
    }
}
