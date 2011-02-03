/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oneandone.sushi.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.w3c.dom.Document;

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
    
    @Test
    public void getOverflow() throws XmlException {
        try {
            getInt("" + (((long) Integer.MAX_VALUE) + 1));
            fail();
        } catch (XmlException e) {
            // ok
        }
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
