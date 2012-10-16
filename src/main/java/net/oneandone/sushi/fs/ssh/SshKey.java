/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.sushi.fs.ssh;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Private/publish key, with optional passphrase */
public class SshKey {
    public static Identity loadDefault(World world, JSch jsch) throws IOException, JSchException {
        return loadDefault(world, jsch, null);
    }

    /** @param passphrase null to try to load passphrase from ~/.ssh/passphrase file */
    public static Identity loadDefault(World world, JSch jsch, String passphrase) throws IOException, JSchException {
        Node dir;
        Node file;
        Node key;

        dir = world.getHome().join(".ssh");
        file = dir.join("passphrase");
        if (passphrase == null && file.exists()) {
            passphrase = file.readString().trim();
        }
        key = dir.join("id_dsa");
        if (!key.exists()) {
            key = dir.join("id_rsa");
            if (!key.exists()) {
                key = dir.join("identity");
            }
        }
        if (!key.isFile()) {
            throw new IOException("private key not found: " + key);
        }
        return load(jsch, key, passphrase);
    }

    public static Identity load(JSch jsch, Node privateKey, String passphrase) throws IOException, JSchException {
        Identity identity;
        Throwable te;
        Class<?> clz;
        Method m;
        byte[] bytes;

        bytes = privateKey.readBytes();
        try {
            clz = Class.forName("com.jcraft.jsch.IdentityFile");
            m = clz.getDeclaredMethod("newInstance", String.class, byte[].class, byte[].class, JSch.class);
            m.setAccessible(true);
            identity = (Identity) m.invoke(null, privateKey.toString(), Arrays.copyOf(bytes, bytes.length), null, jsch);
        } catch (InvocationTargetException e) {
            te = e.getTargetException();
            if (te instanceof JSchException) {
                throw (JSchException) te;
            } else {
                throw new IllegalStateException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException("TODO", e);
        }
        if (passphrase != null) {
            if (!identity.setPassphrase(passphrase.getBytes())) {
                throw new JSchException("invalid passphrase");
            }
        } else {
            if (!identity.setPassphrase(null)) {
                throw new JSchException("missing passphrase");
            }
        }
        return identity;
    }
}
