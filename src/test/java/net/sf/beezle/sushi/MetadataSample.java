/**
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
package net.sf.beezle.sushi;

import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.metadata.Instance;
import net.sf.beezle.sushi.metadata.Type;
import net.sf.beezle.sushi.metadata.reflect.ReflectSchema;

public class MetadataSample {
    private static final World WORLD = new World();
    /** Serialize object to xml and load the result back into an object */
    public static void main(String[] args) throws Exception {
        Instance<Obj> data;
        Obj obj;
        
        data = TYPE.loadXml(WORLD.memoryNode("<obj><number>2</number><string>str</string></obj>"));
        obj = data.get();
        System.out.println("object:\n" + obj);
        obj.number = 3;
        System.out.println("xml:\n" + data.toXml());
    }
    
    private static final Type TYPE = new ReflectSchema(WORLD).type(Obj.class);
    
    public static class Obj {
        public int number;
        public String string;
        
        public Obj() {
            this(0, "");
        }

        public Obj(int number, String string) {
            this.number = number;
            this.string = string;
        }
        
        @Override
        public String toString() {
            return "number=" + number + ",string=" + string;
        }
    }
}
