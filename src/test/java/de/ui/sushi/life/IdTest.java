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

package de.ui.sushi.life;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;

public class IdTest {
    @Test
    public void fromString() {
        check("1:2:3");
        check("de.schlund.tariff:tariff:1-SNAPSHOT");
    }
    
    private void check(String id) {
        assertEquals(id, Id.fromString(id).toString());
    }

    @Test
    public void formRepoNode() throws Exception {
        IO io;
        Node junit;
        
        io = new IO();
        junit = io.locateClasspathItem(Test.class);
        junit.checkFile();
        assertEquals(Id.fromString("junit:junit:4.5"), Id.fromNode(junit));
    }

    @Test
    public void formBillyboyNode() throws Exception {
        IO io;
        Node jar;
        
        io = new IO();
        jar = io.getWorking().join("a+bc+def.jar");
        jar.writeBytes();
        try {
            assertEquals(Id.fromString("a:bc:def"), Id.fromNode(jar));
        } finally {
            jar.delete();
        }
    }
}
