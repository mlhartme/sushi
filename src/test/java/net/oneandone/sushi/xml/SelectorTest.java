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
package net.oneandone.sushi.xml;

import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelectorTest {
    private static final Builder BUILDER = new Builder();
    private static final Selector SELECTOR = new Selector();
    
    @Test
    public void getIntNormal() throws XmlException {
        assertEquals(42, getInt("42"));
    }

    @Test
    public void getIntNagative() throws XmlException {
        assertEquals(-1, getInt("-1"));
    }
    @Test
    public void getIntMax() throws XmlException {
        assertEquals(Integer.MAX_VALUE, getInt("" + Integer.MAX_VALUE));
    }
    
    @Test(expected=XmlException.class)
    public void getOverflow() throws XmlException {
        getInt("" + (((long) Integer.MAX_VALUE) + 1));
    }

    @Test
    public void getLong() throws XmlException {
        Document doc;
        long v;
        
        v = Integer.MAX_VALUE;
        v++;
        assertTrue(v > Integer.MAX_VALUE);
        doc = BUILDER.literal("<root>" + v + "</root>");        
        assertEquals(v, SELECTOR.longer(doc.getDocumentElement(), "."));
    }

    private int getInt(String str) throws XmlException {
        Document doc;
        
        doc = BUILDER.literal("<root>" + str + "</root>");        
        return SELECTOR.integer(doc.getDocumentElement(), ".");
    }
    
    //--
    
    @Test 
    public void string() throws XmlException {
        Document doc;
        
        doc = BUILDER.literal(
                "<published time='1173806109'>" +
                "  <date format='RFC2822'>Tue, 13 Mar 2007 18:15:09 +0100</date>" +
                "  <date format='RFC3339'>2007-03-13T18:15:09+01:00</date>" +
                "</published>");
        assertEquals("Tue, 13 Mar 2007 18:15:09 +0100", SELECTOR.string(doc, "published/date[@format='RFC2822']"));
    }

    @Test
    public void simple() {
        simple(false, "");
        simple(false, "/");
        simple(true, "a");
        simple(true, "1");
        simple(true, "x42");
        simple(true, "a/");
        simple(true, "a/b");
        simple(false, "a//b");
    }

    private static void simple(boolean expected, String path) {
        assertEquals(expected, Selector.isSimple(path));
    }
    
}
