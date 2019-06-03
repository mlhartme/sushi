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
package net.oneandone.sushi.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiOutputStream extends OutputStream {
    public static MultiOutputStream createNullStream() {
        return new MultiOutputStream();
    }

    public static MultiOutputStream createTeeStream(OutputStream... dests) {
        MultiOutputStream result;

        result = new MultiOutputStream();
        result.dests.addAll(Arrays.asList(dests));
        return result;
    }

    private final List<OutputStream> dests;

    public MultiOutputStream() {
        dests = new ArrayList<>();
    }

    public List<OutputStream> dests() {
        return dests;
    }

    //--

    @Override
    public void write(int c) throws IOException {
        for (OutputStream dest : dests) {
            dest.write(c);
        }
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream dest : dests) {
            dest.flush();
        }
    }

    @Override
    public void close() throws IOException {
        // TODO close as many as possible
        for (OutputStream dest : dests) {
            dest.close();
        }
    }
}
