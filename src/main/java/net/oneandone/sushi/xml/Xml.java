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
package net.oneandone.sushi.xml;

/**
 * <p>Xml processing stuff. Not thread-save - every thread should have it's own instance.
 * Creates members lazy because they are resource comsuming. </p>
 */
public class Xml {
    private Builder builder;
    private Selector selector;
    private Serializer serializer;
    
    public Xml() {
        this.builder = null;
        this.selector = null;
        this.serializer = null;
    }

    public Builder getBuilder() {
        if (builder == null) {
            builder = new Builder();
        }
        return builder;
    }

    public Selector getSelector() {
        if (selector == null) {
            selector = new Selector();
        }
        return selector;
    }

    public Serializer getSerializer() {
        if (serializer == null) {
            serializer = new Serializer();
        }
        return serializer;
    }
}
