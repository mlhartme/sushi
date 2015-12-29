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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class NodeReader extends InputStreamReader {
    public static NodeReader create(Node node) throws FileNotFoundException, NewInputStreamException {
        try {
            return new NodeReader(node, node.newInputStream(), node.getWorld().getSettings().encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    //--
    
    private final Node node;
    private final String encoding;
    
    public NodeReader(Node node, InputStream source, String encoding) throws UnsupportedEncodingException {
        super(source, encoding);
        
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
