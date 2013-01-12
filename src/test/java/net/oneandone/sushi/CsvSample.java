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
package net.oneandone.sushi;

import net.oneandone.sushi.csv.Csv;
import net.oneandone.sushi.csv.Format;
import net.oneandone.sushi.csv.View;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.Instance;
import net.oneandone.sushi.metadata.Type;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;

import java.util.Arrays;

public class CsvSample {
    private static final World WORLD = new World();
    private static final Type TYPE = new ReflectSchema(WORLD).type(All.class);
    
    /** Serialize object to xml and load the result back into an object */
    public static void main(String[] args) throws Exception {
        All all;
        View view;
        Csv csv;
        Instance<All> instance;
        
        all = new All();

        System.out.println("object:\n" + all);
        
        instance = TYPE.instance(all);
        csv = new Csv(new Format());
        view = View.fromXml(WORLD.memoryNode("<view>" +
                "  <scope>items</scope>" +
                "  <field><name>Id</name><path>id</path></field>" +
                "  <field><name>String</name><path>string</path></field>" +
                "</view>"));
        instance.exportCsv(view, csv, "7", "2");
        System.out.println("orig\n" + csv);
        csv.get(2).get(1).set(0, "two");
        instance.importCsv(view, csv);
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
