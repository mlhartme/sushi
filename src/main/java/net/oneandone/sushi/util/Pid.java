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
package net.oneandone.sushi.util;

import java.lang.management.ManagementFactory;

public class Pid {
    public static int pid() {
        String str;
        int idx;

        // see http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
        str = ManagementFactory.getRuntimeMXBean().getName();
        idx = str.indexOf('@');
        if (idx == -1) {
            throw new IllegalStateException("cannot guess pid from " + str);
        }
        return Integer.parseInt(str.substring(0, idx));
    }

}
