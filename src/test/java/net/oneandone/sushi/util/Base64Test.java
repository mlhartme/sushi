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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Base64Test {
    @Test
    public void encode() {
        assertEquals("AQ==", new String(run(true, new byte[] { 1 })));
    }

    @Test
    public void decode() {
        byte[] result;
        
        result = run(false, "AQ==".getBytes());
        assertEquals(1, result.length);
        assertEquals((byte) 1, result[0]);
    }

    @Test
    public void lengthZeros() {
        int i;
        byte[] data;
        
        for (i = 0; i < 255; i++) {
            data = new byte[i]; // initialized to zeros
            check(data);
        }
    }

    @Test
    public void lengthNumbers() {
        byte[] data;
        
        for (int i = 0; i < 255; i++) {
            data = new byte[i]; // initialized to zeros
            for (int j = 0; j < i; j++) {
                data[j] = (byte) j;
            }
            check(data);
        }
    }

    @Test
    public void one() {
        int b;
        
        for (b = Byte.MIN_VALUE; b <= Byte.MAX_VALUE; b++) {
            check(new byte[] { (byte) b });
        }
    }
    
    @Test
    public void complex() {
        check("sch??ne schei??e".getBytes());
    }
    
    @Test
    public void string() {
        Base64 encoder;
        Base64 decoder;
        StringBuilder builder;
        String normal;
        String encoded;

        builder = new StringBuilder();
        encoder = new Base64(true);
        decoder = new Base64(false);
        for (char c = 0; c < 257; c++) {
            normal = builder.toString();
            encoded = encoder.run(normal);
            assertEquals(normal, decoder.run(encoded));
            builder.append(c);
        }
    }

    private void check(byte[] orig) {
        byte[] encoded;
        byte[] cmp;
        
        encoded = run(true, orig);
        assertEq(org.apache.commons.codec.binary.Base64.encodeBase64(orig), encoded);
        assertEquals((int) Base64.encodedLength(orig.length), encoded.length);

        cmp = run(false, encoded);
        assertEq(org.apache.commons.codec.binary.Base64.decodeBase64(encoded), cmp);
        assertEquals((int) Base64.encodedLength(orig.length), encoded.length);
        
        assertEquals(orig.length, cmp.length);
        assertEquals(new String(orig), new String(cmp));
    }

    private void assertEq(byte[] expected, byte[] found) {
        assertEquals(expected.length, found.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("idx " + i, expected[i], found[i]);
        }
    }

    private static byte[] run(boolean encoder, byte[] src) {
        Base64 base64;
        ByteArrayOutputStream stream;
        
        base64 = new Base64(encoder);
        stream = new ByteArrayOutputStream();
        try {
            base64.run(new ByteArrayInputStream(src), stream);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return stream.toByteArray();
        
    }
}
