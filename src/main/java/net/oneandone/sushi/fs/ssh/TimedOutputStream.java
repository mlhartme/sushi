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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TimedOutputStream extends FilterOutputStream {
    private final long started;
    public long duration;

    public TimedOutputStream(OutputStream out) {
        super(out);
        this.started = System.currentTimeMillis();
        this.duration = 0;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (duration == 0) {
            duration = System.currentTimeMillis() - started;
        } else {
            // already closed
        }
    }
}
