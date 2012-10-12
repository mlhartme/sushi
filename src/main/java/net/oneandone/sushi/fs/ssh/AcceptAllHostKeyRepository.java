package net.oneandone.sushi.fs.ssh;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;

public class AcceptAllHostKeyRepository implements HostKeyRepository {
    @Override
    public int check(String host, byte[] key) {
        return HostKeyRepository.OK;
    }

    @Override
    public void add(HostKey hostkey, UserInfo ui) {
        throw new IllegalStateException();
    }

    @Override
    public void remove(String host, String type) {
        throw new IllegalStateException();
    }

    @Override
    public void remove(String host, String type, byte[] key) {
        throw new IllegalStateException();
    }

    @Override
    public String getKnownHostsRepositoryID() {
        throw new IllegalStateException();
    }

    @Override
    public HostKey[] getHostKey() {
        throw new IllegalStateException();
    }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        throw new IllegalStateException();
    }
}
