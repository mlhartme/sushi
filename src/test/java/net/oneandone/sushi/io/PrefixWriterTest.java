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

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class PrefixWriterTest {
    @Test
    public void test() throws IOException {
        check("");
        check("", "");
        check("-a", "a");
        check("-ab", "a", "b");
        check("-a\n", "a\n");
        check("-a\n-bc", "a\nbc");
        check("-1\n-2\n-3\n", "1\n", "2\n", "3\n");
    }

    private void check(String expected, String ... args) throws IOException {
        StringWriter dest;
        PrefixWriter pw;

        dest = new StringWriter();
        pw = new PrefixWriter(dest, "-", "\n");
        for (String arg : args) {
            pw.write(arg);
        }
        assertEquals(expected, dest.toString());
    }

    @Test
    public void test2() throws IOException {
        check2("");
        check2("", "");
        check2("-a", "a");
        check2("-a/b", "a", "/", "b");
        check2("-a/n", "a/", "n");
        check2("-a/n-bc", "a/nbc");
        check2("-1/n-2/n-3/n", "1/n", "2/n", "3/n");
    }

    private void check2(String expected, String ... args) throws IOException {
        StringWriter dest;
        PrefixWriter pw;

        dest = new StringWriter();
        pw = new PrefixWriter(dest, "-", "/n");
        for (String arg : args) {
            pw.write(arg);
        }
        assertEquals(expected, dest.toString());
    }
}
