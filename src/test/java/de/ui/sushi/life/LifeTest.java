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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
 
public class LifeTest {
    @Test
    public void normal() throws IOException {
        Life life;
        Node file;
        
        life = new Life();
        assertNull(life.lookup(new Id("foo", "bar", "bar")));
        life.jars().add(new Jar(new Id("a", "b", "c")));
        file = new IO().getTemp().createTempFile();
        life.save(file);
        life = Life.load(file);
        assertNotNull(life.lookup(new Id("a", "b", "c")));
        assertNull(life.lookup(new Id("a", "b", "d")));
    }

    @Test
    public void lookupWithoutVersionl() throws IOException {
        Jar one;
        Jar two;
        Life life;
        List<Jar> lst;

        one = new Jar("a:b:1");
        two = new Jar("a:b:2");
        life = new Life();
        life.jars().add(one);
        life.jars().add(two);
        life.jars().add(new Jar("a:c:2"));
        lst = life.lookupWithoutVersion(Id.fromString("a:b:2"));
        assertEquals(2, lst.size());
        assertTrue(lst.contains(one));
        assertTrue(lst.contains(two));
    }
}
