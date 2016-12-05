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

public class Header {
    public static final String CONNECTION = "Connection";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String HOST = "Host";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    public static Header parse(String line) throws ProtocolException {
        int colon;
        String name;

        colon = line.indexOf(':');
        if (colon == -1) {
            throw new ProtocolException("missing : in header: " + line);
        }
        name = Scanner.substringTrimmed(line, 0, colon);
        if (name.isEmpty()) {
            throw new ProtocolException("empty name in header: " + line);
        }
        return new Header(name, Scanner.substringTrimmed(line, colon + 1, line.length()));
    }

    //--

    public final String name;
    public final String value;

    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
