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

import net.oneandone.sushi.fs.memory.MemoryNode;
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
        assertEquals(WORLD.getSettings().lineSeparator.join("<?xml version=\"1.0\" encoding=\"UTF-8\"?><out/>", ""),
                dest.readString());
    }
}
