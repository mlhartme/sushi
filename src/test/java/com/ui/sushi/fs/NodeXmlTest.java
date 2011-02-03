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

package de.ui.sushi.fs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import de.ui.sushi.fs.memory.MemoryNode;

public class NodeXmlTest {
    private static final IO IO_OBJ = new IO();

    @Test
    public void xslt() throws IOException, TransformerException {
        Transformer t;
        MemoryNode src;
        MemoryNode dest;
        
        t = IO_OBJ.stringNode(
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
                "  <xsl:output method='xml' indent='yes'/>" +
                "  <xsl:template match='/' ><out/></xsl:template>" +
                "</xsl:stylesheet>").readXsl();
        src = IO_OBJ.stringNode("<foo><bar/></foo>");
        dest = IO_OBJ.stringNode("");
        src.xslt(t, dest);
        assertEquals(IO_OBJ.getSettings().join("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<out/>", ""), 
                dest.readString());
    }
}
