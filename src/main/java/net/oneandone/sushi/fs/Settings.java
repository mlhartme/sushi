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
package net.oneandone.sushi.fs;

import net.oneandone.sushi.io.OS;
import net.oneandone.sushi.util.Separator;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

/**
 * <p>Settings for nodes. Immutable. </p>
 */
public class Settings {
    public static final String UTF_8 = "UTF-8";
    public static final String ISO8859_1 = "ISO8859_1";

    public static final String DEFAULT_LINE_SEPARATOR = OS.CURRENT.lineSeparator.getSeparator();

    private static final byte[] BYTES = { 65 };

    public final String encoding;
    public final Separator lineSeparator;
    public final LineFormat lineFormat;

    /** Create a Buffer with UTF-8 encoding */
    public Settings() {
        this(UTF_8);
    }

    public Settings(String encoding) {
        this(encoding, DEFAULT_LINE_SEPARATOR);
    }

    public Settings(String encoding, String lineSeparator) {
        try {
            new String(BYTES, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(encoding, e);
        }
        this.encoding = encoding;
        this.lineSeparator = Separator.on(lineSeparator);
        this.lineFormat = new LineFormat(Pattern.compile(Pattern.quote(lineSeparator)));
    }

    public String string(byte[] bytes) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String string(ByteArrayOutputStream stream) {
        try {
            return stream.toString(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    public byte[] bytes(String str) {
        try {
            return str.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
