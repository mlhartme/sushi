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
package net.oneandone.sushi.fs.http.model;

import net.oneandone.sushi.fs.http.io.AsciiInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HeaderList implements Iterable<Header> {
    public static HeaderList of(String... keyValues) {
        HeaderList result;

        if (keyValues.length % 2 != 0) {
            throw new IllegalStateException();
        }
        result = new HeaderList();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.add(keyValues[i], keyValues[i + 1]);
        }
        return result;
    }

    public static HeaderList parse(AsciiInputStream src) throws IOException {
        HeaderList result;
        String line;

        result = new HeaderList();
        while (true) {
            line = src.readLine();
            if (line == null || line.isEmpty()) {
                return result;
            }
            if ((line.charAt(0) == ' ') || (line.charAt(0) == '\t')) {
                throw new ProtocolException("header continuation is not supported: " + line);
            }
            result.add(Header.parse(line));
        }
    }

    private final List<Header> headers;

    public HeaderList() {
        this.headers = new ArrayList<>();
    }

    public void addAll(HeaderList lst) {
        for (Header header : lst) {
            add(header);
        }
    }

    public void add(Header header) {
        headers.add(header);
    }

    public void add(String name, String value) {
        add(new Header(name, value));
    }

    public String getFirstValue(String name) {
        Header header;

        header = getFirst(name);
        return header == null ? null : header.value;
    }
    
    public Header getFirst(String name) {
        for (Header header : this) {
            if (header.name.equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    public Iterator<Header> iterator() {
        return headers.iterator();
    }

}
