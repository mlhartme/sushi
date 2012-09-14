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

import net.oneandone.sushi.fs.Settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class Base64 {
    /** true create base64 */
    private final boolean encoder;
    private final byte[] srcBuffer;
    private final byte[] destBuffer;
    
    public Base64(boolean encoder) {
        this(encoder, 1024);
    }

    public Base64(boolean encoder, int bufferSizeBase) {
        int bufferSize;
        
        if (bufferSizeBase < 1) {
            throw new IllegalArgumentException("" + bufferSizeBase);
        }
        bufferSize = bufferSizeBase * TRIPPLET_BYTES;
        this.encoder = encoder;
        if (encoder) {
            this.srcBuffer = new byte[bufferSize];
            this.destBuffer = new byte[toInt(encodedLength(bufferSize))];
        } else {
            this.srcBuffer = new byte[toInt(encodedLength(bufferSize))];
            this.destBuffer = new byte[bufferSize];
        }
    }

    private static int toInt(long l) {
        int i;
        
        i = (int) l;
        if (i != l) {
            throw new IllegalArgumentException("" + l);
        }
        return i;
    }
    
    public String run(String str) {
        try {
            return run(str, Settings.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public String run(String str, String encoding) throws UnsupportedEncodingException {
        return new String(run(str.getBytes(encoding)), encoding);
    }
    
    public byte[] run(byte ... bytes) {
        return run(bytes, 0, bytes.length);
    }
    
    public byte[] run(byte[] bytes, int ofs, int len) {
        ByteArrayOutputStream dest;
        
        dest = new ByteArrayOutputStream();
        try {
            run(new ByteArrayInputStream(bytes, ofs, len), dest);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return dest.toByteArray();
    }
    
    public long run(InputStream src, OutputStream dest) throws IOException {
        return run(src, dest, Long.MAX_VALUE);
    }
    
    /** @return number of bytes actually written */
    public long run(InputStream src, OutputStream dest, long maxSrcLength) throws IOException {
        int srcCount;
        int destCount;
        long destLength; // toal length
        long remaining;
        
        destLength = 0;
        remaining = maxSrcLength;
        while (true) {
            srcCount = read(src, srcBuffer, (int) Math.min(srcBuffer.length, remaining));
            if (srcCount == 0) {
                break;
            }
            destCount = encoder? encode(srcBuffer, srcCount, destBuffer) : decode(srcBuffer, srcCount, destBuffer);
            dest.write(destBuffer, 0, destCount);
            destLength += destCount;
            remaining -= srcCount;
        }
        return destLength;
    }

    //--
    
    /**
     * Make sure the buffer is filled as much as possible, even if the input stream
     * delivers smaller chunks
     * @return number of bytes actually read
     */
    public static int read(InputStream src, byte[] dest, int len) throws IOException {
        int ofs;
        int count;
        
        for (ofs = 0; ofs < len; ofs += count) {
            count = src.read(dest, ofs, len - ofs);
            if (count <= 0) {
                break;
            }
        }
        return ofs;
    }
    
    //--

    public static long encodedLength(long decodedLength) {
        long bits = decodedLength * EIGHTBIT;
        long triplets = bits / TWENTYFOURBITGROUP;

        if (bits % TWENTYFOURBITGROUP != 0) {
            return (triplets + 1) * 4;
        } else {
            return triplets * 4;
        }
    }
    
    //-- 
    //  The following is copied from commons-codec-1.3. Modifications: 
    //  o removed chunking support
    //  o removed interfaces
    //  o reduced memory consumption

    /**
     * The base length.
     */
    private static final int BASELENGTH = 255;

    /**
     * Lookup length.
     */
    private static final int LOOKUPLENGTH = 64;

    /**
     * Used to calculate the number of bits in a byte.
     */
    private static final int EIGHTBIT = 8;

    /**
     * Used when encoding something which has fewer than 24 bits.
     */
    private static final int SIXTEENBIT = 16;

    /**
     * Used to determine how many bits data contains.
     */
    private static final int TWENTYFOURBITGROUP = 24;


    /**
     * Used to test the sign of a byte.
     */
    private static final int SIGN = -128;
    
    /**
     * Byte used to pad output.
     */
    private static final byte PAD = (byte) '=';
    
    private static final int TRIPPLET_BYTES = TWENTYFOURBITGROUP / EIGHTBIT;
    
    // Create arrays to hold the base64 characters and a 
    // lookup for base64 chars
    private static final byte[] base64Alphabet = new byte[BASELENGTH];
    private static final byte[] lookUpBase64Alphabet = new byte[LOOKUPLENGTH];

    // Populating the lookup and character arrays
    static {
        for (int i = 0; i < BASELENGTH; i++) {
            base64Alphabet[i] = (byte) -1;
        }
        for (int i = 'Z'; i >= 'A'; i--) {
            base64Alphabet[i] = (byte) (i - 'A');
        }
        for (int i = 'z'; i >= 'a'; i--) {
            base64Alphabet[i] = (byte) (i - 'a' + 26);
        }
        for (int i = '9'; i >= '0'; i--) {
            base64Alphabet[i] = (byte) (i - '0' + 52);
        }

        base64Alphabet['+'] = 62;
        base64Alphabet['/'] = 63;

        for (int i = 0; i <= 25; i++) {
            lookUpBase64Alphabet[i] = (byte) ('A' + i);
        }

        for (int i = 26, j = 0; i <= 51; i++, j++) {
            lookUpBase64Alphabet[i] = (byte) ('a' + j);
        }

        for (int i = 52, j = 0; i <= 61; i++, j++) {
            lookUpBase64Alphabet[i] = (byte) ('0' + j);
        }

        lookUpBase64Alphabet[62] = (byte) '+';
        lookUpBase64Alphabet[63] = (byte) '/';
    }

    private static boolean isBase64(byte octect) {
        return octect == PAD || base64Alphabet[octect] != -1;
    }

    /**
     * Encodes binary data using the base64 algorithm, no chunking.
     *
     * @param binaryData Array containing binary data to encode.
     * @param binaryDataLength  number of bytes in binaryData.
     * @param encodedData Array receiving encoded data.
     * @return encoded data length
     */
    public static int encode(byte[] binaryData, int binaryDataLength, byte[] encodedData) {
        int lengthDataBits = binaryDataLength * EIGHTBIT;
        int fewerThan24bits = lengthDataBits % TWENTYFOURBITGROUP;
        int numberTriplets = lengthDataBits / TWENTYFOURBITGROUP;
        int encodedDataLength;

        if (fewerThan24bits != 0) {
            //data not divisible by 24 bit
            encodedDataLength = (numberTriplets + 1) * 4;
        } else {
            // 16 or 8 bit
            encodedDataLength = numberTriplets * 4;
        }
        if (encodedDataLength > encodedData.length) {
            throw new IllegalArgumentException(encodedDataLength + ">" + encodedData.length);
        }

        byte k = 0, l = 0, b1 = 0, b2 = 0, b3 = 0;

        int encodedIndex = 0;
        int dataIndex = 0;
        int i;

        for (i = 0; i < numberTriplets; i++) {
            dataIndex = i * 3;
            b1 = binaryData[dataIndex];
            b2 = binaryData[dataIndex + 1];
            b3 = binaryData[dataIndex + 2];

            l = (byte) (b2 & 0x0f);
            k = (byte) (b1 & 0x03);

            byte val1 =
                ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
            byte val2 =
                ((b2 & SIGN) == 0) ? (byte) (b2 >> 4) : (byte) ((b2) >> 4 ^ 0xf0);
            byte val3 =
                ((b3 & SIGN) == 0) ? (byte) (b3 >> 6) : (byte) ((b3) >> 6 ^ 0xfc);

            encodedData[encodedIndex] = lookUpBase64Alphabet[val1];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[val2 | (k << 4)];
            encodedData[encodedIndex + 2] = lookUpBase64Alphabet[(l << 2) | val3];
            encodedData[encodedIndex + 3] = lookUpBase64Alphabet[b3 & 0x3f];
            encodedIndex += 4;
        }

        // form integral number of 6-bit groups
        dataIndex = i * 3;

        if (fewerThan24bits == EIGHTBIT) {
            b1 = binaryData[dataIndex];
            k = (byte) (b1 & 0x03);
            byte val1 =
                ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
            encodedData[encodedIndex] = lookUpBase64Alphabet[val1];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[k << 4];
            encodedData[encodedIndex + 2] = PAD;
            encodedData[encodedIndex + 3] = PAD;
        } else if (fewerThan24bits == SIXTEENBIT) {

            b1 = binaryData[dataIndex];
            b2 = binaryData[dataIndex + 1];
            l = (byte) (b2 & 0x0f);
            k = (byte) (b1 & 0x03);

            byte val1 = ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
            byte val2 = ((b2 & SIGN) == 0) ? (byte) (b2 >> 4) : (byte) ((b2) >> 4 ^ 0xf0);
            encodedData[encodedIndex] = lookUpBase64Alphabet[val1];
            encodedData[encodedIndex + 1] = lookUpBase64Alphabet[val2 | (k << 4)];
            encodedData[encodedIndex + 2] = lookUpBase64Alphabet[l << 2];
            encodedData[encodedIndex + 3] = PAD;
        }

        return encodedDataLength;
    }

    /**
     * Decodes Base64 data into octects
     *
     * @param encoded Byte array containing Base64 data
     * @param encodedLength number of bytes in base64Data
     * @param binary Array containing decoded data.
     * @return binary data length
     */
    public static int decode(byte[] encoded, int encodedLength, byte[] binary) {
        byte b1, b2, b3, b4, marker0, marker1;
        int binaryIndex;
        int encodedIndex;
        
        encodedLength = discardNonBase64(encoded, encodedLength);
        encodedIndex = 0;
        binaryIndex = 0;
        while (encodedIndex < encodedLength) {
            b1 = base64Alphabet[encoded[encodedIndex++]];
            b2 = base64Alphabet[encoded[encodedIndex++]];
            marker0 = encoded[encodedIndex++];
            marker1 = encoded[encodedIndex++];
            if (marker0 == PAD) {
                // two PAD e.g. 3c[Pad][Pad]
                if (marker1 != PAD) {
                    throw new IllegalArgumentException();
                }
                binary[binaryIndex] = (byte) (b1 << 2 | b2 >> 4);
                binaryIndex += 1;
                if (encodedIndex != encodedLength) {
                    throw new IllegalStateException();
                }
            } else if (marker1 == PAD) {
                // one PAD e.g. 3cQ[Pad]
                b3 = base64Alphabet[marker0];
            
                binary[binaryIndex] = (byte) (b1 << 2 | b2 >> 4);
                binary[binaryIndex + 1] =
                (byte) (((b2 & 0xf) << 4) | ((b3 >> 2) & 0xf));
                binaryIndex += 2;
                if (encodedIndex != encodedLength) {
                    throw new IllegalStateException();
                }
            } else {
                // no PAD e.g 3cQl
                b3 = base64Alphabet[marker0];
                b4 = base64Alphabet[marker1];
                
                binary[binaryIndex] = (byte) (b1 << 2 | b2 >> 4);
                binary[binaryIndex + 1] = (byte) (((b2 & 0xf) << 4) | ((b3 >> 2) & 0xf));
                binary[binaryIndex + 2] = (byte) (b3 << 6 | b4);
                binaryIndex += 3;
            } 
        }
        return binaryIndex;
    }
    
    /**
     * Discards any characters outside of the base64 alphabet, per
     * the requirements on page 25 of RFC 2045 - "Any characters
     * outside of the base64 alphabet are to be ignored in base64
     * encoded data."
     *
     * @param data The base-64 encoded data to groom
     * @return number of bytes in groomed data.
     */
    static int discardNonBase64(byte[] data, int length) {
        int dest = 0;
        for (int i = 0; i < length; i++) {
            if (isBase64(data[i])) {
                data[dest++] = data[i];
            }
        }
        return dest;
    }
}
