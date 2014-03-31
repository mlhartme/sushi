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
package net.oneandone.sushi.io;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiWriter extends Writer {
    public static MultiWriter createNullWriter() {
        return new MultiWriter();
    }

    public static MultiWriter createTeeWriter(Writer ... dests) {
        MultiWriter result;

        result = new MultiWriter();
        result.dests.addAll(Arrays.asList(dests));
        return result;
    }

    private final List<Writer> dests;

    public MultiWriter() {
        dests = new ArrayList<>();
    }

    public List<Writer> dests() {
        return dests;
    }

    //--
    
    @Override
    public void write(int c) throws IOException {
        for (Writer dest : dests) {
            dest.write(c);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (Writer dest : dests) {
            dest.write(cbuf, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        for (Writer dest : dests) {
            dest.flush();
        }
    }

    @Override
    public void close() throws IOException {
        // TODO close as many as possible
        for (Writer dest : dests) {
            dest.close();
        }
    }
}
