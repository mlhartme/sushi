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

package com.oneandone.sushi;

import com.oneandone.sushi.csv.Csv;
import com.oneandone.sushi.csv.Format;
import com.oneandone.sushi.csv.View;
import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.metadata.Instance;
import com.oneandone.sushi.metadata.Type;
import com.oneandone.sushi.metadata.reflect.ReflectSchema;

import java.util.Arrays;

public class CsvSample {
    private static final IO IO_OBJ = new IO();
    private static final Type TYPE = new ReflectSchema(IO_OBJ).type(All.class);
    
    /** Serialize object to xml and load the result back into an object */
    public static void main(String[] args) throws Exception {
        All all;
        View view;
        Csv csv;
        Instance<All> data;
        
        all = new All();

        System.out.println("object:\n" + all);
        
        data = TYPE.instance(all);
        csv = new Csv(new Format());
        view = View.fromXml(IO_OBJ.memoryNode("<view>" +
                "  <scope>items</scope>" +
                "  <field><name>Id</name><path>id</path></field>" +
                "  <field><name>String</name><path>string</path></field>" +
                "</view>"));
        data.exportCsv(view, csv, "7", "2");
        System.out.println("orig\n" + csv);
        csv.get(2).get(1).set(0, "two");
        data.importCsv(view, csv);
        System.out.println("modified\n" + csv);
    }
    
    public static class All {
        // TOOD: doesn't work for Lists because the static type is used
        public final Item[] items = { new Item(2, "zwei"), new Item(7, "sieben") };
        
        @Override
        public String toString() {
            return Arrays.asList(items).toString();
        }
    }
    
    public static class Item {
        public final int id;
        public final String string;
        
        public Item() {
            this(0, "");
        }

        public Item(int id, String string) {
            this.id = id;
            this.string = string;
        }
        
        @Override
        public String toString() {
            return "id=" + id + "+string=" + string;
        }
    }
}
