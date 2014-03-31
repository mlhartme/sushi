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
package net.oneandone.sushi.csv;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.Path;
import net.oneandone.sushi.metadata.model.ModelBase;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ViewTest extends ModelBase {
    private static final Format NORMAL = new Format(false);
    private static final Format MERGED = new Format(true);

    @Test
    public void create() throws CsvLineException {
        assertEquals(1, csv(NORMAL, "a;b;c").size());
        assertEquals(1, csv(NORMAL, "a,A;b;c").size());
        assertEquals(2, csv(NORMAL, "a;b;c", "A;b;c").size());
        assertEquals(1, csv(MERGED, "a;b;c", "A;b;c").size());
    }

    @Test
    public void setOneOne() throws Exception {
        View view;

        view = view();
        assertEquals(90, audi.getEngine().getPs());
        view.fromCsv(csv(NORMAL, "name;ps", "audi;5"), MODEL.instance(vendor));
        assertEquals(5, audi.getEngine().getPs());
    }

    @Test
    public void setOneOneWith2Values() throws Exception {
        View view;

        view = view();
        assertEquals(90, audi.getEngine().getPs());
        view.fromCsv(csv(NORMAL, "name;ps", "audi|bmw;5"), MODEL.instance(vendor));
        assertEquals(5, audi.getEngine().getPs());
        assertEquals(5, bmw.getEngine().getPs());
    }

    @Test
    public void setTwoTwo() throws Exception {
        View view;

        view = view();
        assertEquals(90, audi.getEngine().getPs());
        view.fromCsv(csv(NORMAL, "name;seats;ps", "audi;5;6", "bmw;1;2"), MODEL.instance(vendor));
        assertEquals(5, audi.getSeats());
        assertEquals(6, audi.getEngine().getPs());
        assertEquals(1, bmw.getSeats());
        assertEquals(2, bmw.getEngine().getPs());
    }

    @Test
    public void setEmpty() throws Exception {
        View view;

        view = view();
        view.fromCsv(csv(NORMAL, "name;seats;ps"), MODEL.instance(vendor));
    }

    @Test
    public void setOptional() throws Exception {
        View view;

        view = view();
        view.add(new Field("cd", new Path("radio/cd")));

        assertEquals(false, audi.getRadio().getCd());
        view.fromCsv(csv(NORMAL, "name;cd", "audi;true"), MODEL.instance(vendor));
        assertEquals(true, audi.getRadio().getCd());

        assertNull(bmw.getRadio());
        view.fromCsv(csv(NORMAL, "name;cd", "bmw;true"), MODEL.instance(vendor));
        assertNotNull(bmw.getRadio());
        assertTrue(bmw.getRadio().getCd());
    }

    @Test
    public void setSequence() throws Exception {
        View view;

        view = new View(new Path("car"));
        view.add(new Field("name", new Path("name")));
        view.add(new Field("comment", new Path("comment")));
        assertEquals(0, audi.commentList().size());
        comment(view, "a", "a");
        comment(view, "", "");
        comment(view, "a|b", "a", "b");
        comment(view, "b", "b");
    }
    private void comment(View view, String value, String ... expected) throws Exception {
        view.fromCsv(csv(NORMAL, "name;comment", "audi;" + value), MODEL.instance(vendor));
        assertEquals(Arrays.asList(expected), audi.commentList());
    }

    @Test
    public void merge() {
        Csv csv;
        Line one;

        one = Line.create("one", "1");

        csv = new Csv(MERGED);
        csv.add(one);
        csv.add(Line.create("two", "2"));
        assertEquals(2, csv.size());

        csv.add(Line.create("eins", "1"));
        assertEquals(2, csv.size());
        assertEquals(Arrays.asList("one", "eins"), one.get(0));
    }

    @Test
    public void get() {
        View view;
        Csv dest;

        view = view();
        dest = new Csv(NORMAL);
        view.toCsv(MODEL.instance(vendor), dest, "audi", "bmw");
        assertEquals("\"name\";\"ps\";\"seats\"\n\"audi\";90;4\n\"bmw\";200;2\n", dest.toString());
    }

    @Test
    public void keyNotFound() throws Exception {
        View view;

        view = view();
        try {
            view.fromCsv(csv(NORMAL, "name; ps", "audie; 5"), MODEL.instance(vendor));
            fail();
        } catch (ViewException e) {
            // ok
        }
    }

    @Test
    public void xml() {
        View view;
        Field field;

        view = view("<view><scope>a</scope></view>");
        assertEquals(0, view.size());

        view = view("<view>" +
                "  <scope>a</scope>" +
                "  <field><name>1</name><path>a</path></field>" +
                "  <field><name>2</name><path>b</path></field>" +
                "</view>");
        assertEquals(2, view.size());

        field = view.lookup("1");
        assertNotNull(field);
        assertEquals("a", field.getPath().getPath());

        field = view.lookup("2");
        assertNotNull(field);
        assertEquals("b", field.getPath().getPath());
    }

    private View view(String str) {
        try {
            return View.fromXml(new World().memoryNode(str));
        } catch (IOException e) {
            throw new RuntimeException("TODO", e);
        }
    }

    private View view() {
        View view;

        view = new View(new Path("car"));
        view.add(new Field("name", new Path("name")));
        view.add(new Field("ps", new Path("engine/ps")));
        view.add(new Field("seats", new Path("seats")));
        return view;
    }

    private Csv csv(Format format, String ... lines) throws CsvLineException {
        return new Csv(format).addAll(lines);
    }
}
