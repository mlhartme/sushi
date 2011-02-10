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

package net.sf.beezle.sushi.fs;

import net.sf.beezle.sushi.fs.memory.MemoryNode;
import org.junit.Test;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class NodeXmlTest {
    private static final World WORLD = new World();

    @Test
    public void xslt() throws IOException, TransformerException {
        Transformer t;
        MemoryNode src;
        MemoryNode dest;
        
        t = WORLD.memoryNode(
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
                "  <xsl:output method='xml' indent='yes'/>" +
                "  <xsl:template match='/' ><out/></xsl:template>" +
                "</xsl:stylesheet>").readXsl();
        src = WORLD.memoryNode("<foo><bar/></foo>");
        dest = WORLD.memoryNode("");
        src.xslt(t, dest);
        assertEquals(WORLD.getSettings().join("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<out/>", ""),
                dest.readString());
    }
}
