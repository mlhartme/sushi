/*
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

    public int hashCode() {
        return id;
    }
}
