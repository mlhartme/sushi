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

package com.oneandone.sushi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import com.oneandone.sushi.util.Substitution;
import com.oneandone.sushi.util.SubstitutionException;

public class SubstitutionTest {
    private Map<String, String> props;

    public SubstitutionTest() throws IOException {
        props = new HashMap<String, String>();
        props.put("1", "one");
        props.put("2", "two");
	}

	@Test
	public void underline() throws SubstitutionException {
	    Substitution underline;
	    
		underline = new Substitution("_", "_", '/');
		assertEquals("", underline.apply("", props));
        assertEquals("1", underline.apply("1", props));
        assertEquals("one", underline.apply("_1_", props));
        assertEquals(" one xyz", underline.apply(" _1_ xyz", props));
        assertEquals("onetwo", underline.apply("_1__2_", props));
        assertEquals("_", underline.apply("/_", props));
        assertEquals("__", underline.apply("/_/_", props));
        assertEquals("abc_def", underline.apply("abc/_def", props));

        try {
            underline.apply("_3_", props);
            fail();
        } catch (SubstitutionException e) {
            // ok
        }
        try {
            underline.apply("_", props);
            fail();
        } catch (SubstitutionException e) {
            // ok
        }
	}

    @Test
    public void ant() throws SubstitutionException {
        Substitution ant;
        
        ant = Substitution.ant();
        assertEquals("", ant.apply("", props));
        assertEquals("1", ant.apply("1", props));
        assertEquals("one", ant.apply("${1}", props));
        assertEquals(" one xyz", ant.apply(" ${1} xyz", props));
        assertEquals("onetwo", ant.apply("${1}${2}", props));
        assertEquals("${", ant.apply("\\${", props));
        assertEquals("${${", ant.apply("\\${\\${", props));
        assertEquals("123${456", ant.apply("123\\${456", props));
        try {
            ant.apply("${3}", props);
            fail();
        } catch (SubstitutionException e) {
            // ok
        }
        try {
            ant.apply("${", props);
            fail();
        } catch (SubstitutionException e) {
            // ok
        }
	}
}
