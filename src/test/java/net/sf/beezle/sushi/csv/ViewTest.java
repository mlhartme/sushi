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

package net.sf.beezle.sushi.csv;

import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.metadata.Path;
import net.sf.beezle.sushi.metadata.model.ModelBase;
import net.sf.beezle.sushi.metadata.xml.LoaderException;
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
        } catch (LoaderException e) {
            throw new RuntimeException("TODO", e);
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
