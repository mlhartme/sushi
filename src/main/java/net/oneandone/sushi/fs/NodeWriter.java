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
package net.oneandone.sushi.fs;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class NodeWriter extends OutputStreamWriter {
    public static NodeWriter create(Node node, boolean append) throws NewOutputStreamException {
        try {
            return new NodeWriter(node, node.newOutputStream(append), node.getWorld().getSettings().encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    //--
    
    private final Node node;
    private final String encoding;
    
    public NodeWriter(Node node, OutputStream dest, String encoding) throws UnsupportedEncodingException {
        super(dest, encoding);
        
        this.node = node;
        this.encoding = encoding;
    }

    public Node getNode() {
        return node;
    }
    
    @Override
    public String getEncoding() {
        return encoding;
    }
}
