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

import net.oneandone.sushi.metadata.Instance;
import net.oneandone.sushi.metadata.Path;
import net.oneandone.sushi.metadata.SimpleTypeException;
import net.oneandone.sushi.metadata.Variable;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.util.List;

@Type public class Field {
    @Value private String name;
    @Value private Path path;
    
    public Field() { // TODO
        this("", new Path(""));
    }

    public Field(String name, Path path) {
        this.name = name;
        this.path = path;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public List<String> get(Instance<?> context) {
        Variable<?> var;

        var = path.access(context, false);
        if (var == null) {
            return null;
        } else {
            return var.getStrings();
        }
    }
    
    public void set(Instance<?> context, List<String> values) throws SimpleTypeException {
        if (values == null) {
            // ignore
        } else {
            path.access(context, true).setStrings(values);
        }
    }

    @Override
    public String toString() {
        return name + ":" + path.toString();
    }
}
