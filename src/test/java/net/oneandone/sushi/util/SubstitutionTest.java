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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SubstitutionTest {
    private final Map<String, String> props;

    public SubstitutionTest() {
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
