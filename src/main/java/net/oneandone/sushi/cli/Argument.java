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

/** Defines where to store one command line argument (or a list of command line arguments) */
public abstract class Argument {
    private final ArgumentDeclaration declaration;
    private final SimpleType type;

    protected Argument(ArgumentDeclaration declaration, SimpleType type) {
        this.declaration = declaration;
        this.type = type;
    }

    public ArgumentDeclaration declaration() {
        return declaration;
    }

    public SimpleType type() { return type; }
    public abstract boolean before();
    public abstract void set(Object obj, Object value);

}
