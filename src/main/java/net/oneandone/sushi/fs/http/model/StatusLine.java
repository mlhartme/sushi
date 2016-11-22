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
package net.oneandone.sushi.fs.http.model;

public class StatusLine {
    public static final String HTTP_1_1 = "HTTP/1.1";

    public static StatusLine parse(String str) throws ProtocolException {
        Scanner scanner;
        String protocol;
        int between;
        int space;
        int code;

        scanner = new Scanner();
        protocol = scanner.parseProtocol(str);
        between = scanner.skipWhitespace(str);
        space = str.indexOf(' ', between);
        if (space < 0) {
            space = str.length();
        }
        try {
            code = Integer.parseInt(Scanner.substringTrimmed(str, between, space));
        } catch (NumberFormatException e) {
            throw new ProtocolException("status line contains invalid status code: " + str);
        }
        return new StatusLine(protocol, code);
    }

    //--

    public final String protocol;

    public final int statuscode;

    public StatusLine(String protocol, int statuscode) {
        this.protocol = protocol;
        this.statuscode = statuscode;
    }

    public String toString() {
        return protocol + " " + statuscode;
    }
}
