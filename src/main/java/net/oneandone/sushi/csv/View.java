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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.metadata.Instance;
import net.oneandone.sushi.metadata.Path;
import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;
import net.oneandone.sushi.metadata.annotation.AnnotationSchema;
import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;
import net.oneandone.sushi.metadata.xml.LoaderException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Defines how to turn instances into csv an vice versa */ 
@Type public class View {
    public static View fromXml(Node src) throws IOException, LoaderException {
        Schema metadata;
        
        metadata = new AnnotationSchema();
        metadata.add(new SimpleType(metadata, Path.class, "string") {
            @Override
            public Object newInstance() {
                return "";
            }
            
            @Override
            public Object stringToValue(String str) {
                return new Path(str);
            }

            @Override
            public String valueToString(Object value) {
                return ((Path) value).getPath();
            }
        });
        return metadata.type(View.class).<View>loadXml(src).get();
    }

    @Value private Path scope;
    @Sequence(Field.class) private final List<Field> fields;
    
    public View() { // TODO
        this(new Path(""));
    }

    public View(Path scope) {
        this.scope = scope;
        this.fields = new ArrayList<Field>();
    }

    public Path getScope() {
        return scope;
    }
    
    public void setScope(Path scope) {
        this.scope = scope;
    }
    
    public List<Field> fields() {
        return fields;
    }
    
    public void add(Field field) {
        if (lookup(field.getName()) != null) {
            throw new ViewException("duplicate field: " + field.getName());
        }
        fields.add(field);
    }
    
    public int size() {
        return fields.size();
    }
    
    public Field lookup(String name) {
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }
    
    //--
    
    public void toCsv(Instance<?> src, Csv dest, String ... selected) {
        toCsv(src, dest, Arrays.asList(selected));
    }
    
    public void toCsv(Instance<?> src, Csv dest, List<String> selected) {
        List<String> keys;
        String key;
        int idx;
        Instance<?>[] found;
        
        found = new Instance<?>[selected.size()];
        for (Instance<?> value : scope.select(src)) {
            keys = fields.get(0).get(value);
            if (keys.size() == 1) {
                key = keys.get(0);                
                idx = selected.indexOf(key);
                if (idx != -1) {
                    if (found[idx] != null) {
                        throw new IllegalArgumentException("duplicate key: " + key);
                    }
                    found[idx] = value;
                }
            }
        }
        dest.add(header());
        for (int i = 0; i < found.length; i++) {
            if (found[i] == null) {
                throw new IllegalArgumentException("key not found: " + selected.get(i));
            }
            toCsv(found[i], dest);
        }
    }

    public void toCsv(Instance<?> value, Csv dest) {
        Line line;
        List<String> strings;
        
        line = new Line();
        for (Field field : fields) {
            strings = field.get(value);
            if (strings == null) {
                line.addNull();
            } else {
                line.add().addAll(strings);
            }
        }
        dest.add(line);
    }

    public void fromCsv(Csv src, Instance<?> dest) throws SimpleTypeException {
        List<Field> header;
    
        header = null;
        for (Line line : src) {
            if (header == null) {
                // don't care wether about line.isComment
                header = parseHeader(line);
            } else {
                setLine(header, line, dest);
            }
        }
    }
    
    public void setLine(List<Field> header, Line line, Instance<?> dest) throws SimpleTypeException {
        int size;
        Instance<?> selected;
        
        size = line.size();
        if (size != header.size()) {
            throw new ViewException("column mismatch: expected " + header.size() + ", got " + size);
        }
        for (String id : line.get(0)) {
            selected = find(scope.select(dest), header.get(0).getPath(), id);
            for (int i = 1; i < size; i++) {
                header.get(i).set(selected, line.get(i));
            }
        }
    }
    
    private Instance<?> find(List<Instance<?>> all, Path path, String value) {
        for (Instance<?> i : all) {
            if (value.equals(path.selectOne(i).valueToString())) {
                return i;
            }
        }
        throw new ViewException("no such value: " + value);
    }
    
    //--
    
    private Line header() {
        Line line;
        
        line = new Line();
        for (Field field : fields) {
            line.addOne(field.getName());
        }
        return line;
    }

    private List<Field> parseHeader(Line line) {
        List<Field> result;
        Field field;
        String name;
        
        result = new ArrayList<Field>();
        for (int i = 0, max = line.size(); i < max; i++) {
            name = line.getOne(i);
            field = lookup(name);
            if (field == null) {
                throw new ViewException("unknown field in header: '" + name + "'");
            }
            result.add(field);
        }
        return result;
    }
}
