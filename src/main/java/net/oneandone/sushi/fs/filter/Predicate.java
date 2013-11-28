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
package net.oneandone.sushi.fs.filter;

import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

public interface Predicate {
    Predicate FILE = new Predicate() {
        public boolean matches(Node node, boolean isLink) throws ExistsException {
            return node.isFile();
        }
    };

    Predicate DIRECTORY = new Predicate() {
        public boolean matches(Node node, boolean isLink) throws ExistsException {
            return node.isDirectory();
        }
    };

    Predicate LINK = new Predicate() {
		public boolean matches(Node node, boolean isLink) throws IOException {
			return isLink;
		}
    };

    Predicate NON_LINK = new Predicate() {
		public boolean matches(Node node, boolean isLink) throws IOException {
			return !isLink;
		}
    };

    boolean matches(Node node, boolean isLink) throws IOException;

    public class FALSE {
    }
}
