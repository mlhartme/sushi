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
package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.SimpleType;

/** TODO: merge with Item? */
public abstract class Argument {
    private final String name;
    private final SimpleType type;
    private final int min;
    private final int max;

    protected Argument(String name, SimpleType type, int min, int max) {
        this.name = name;
        this.type = type;
        this.min = min;
        this.max = max;
    }

    public int max() {
        return max;
    }

    public abstract boolean before();

    public String getName() {
        return name;
    }
    
    public SimpleType getType() {
        return type;
    }

    public void checkCardinality(int size) {
        if (size < min) {
            throw new ArgumentException(name + ": missing value(s). Expected " + min + ", got " + size);
        }
        if (size > max) {
            throw new ArgumentException(name + ": too many values. Expected " + max + ", got " + size);
        }
    }

    public boolean isOptional() {
        return min == 0;
    }

    public boolean isList() {
        return max > 1;
    }

    public abstract void set(Object obj, Object value);

}
