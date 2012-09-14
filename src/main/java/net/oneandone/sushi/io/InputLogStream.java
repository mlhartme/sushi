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
package net.oneandone.sushi.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputLogStream extends FilterInputStream {
	private final OutputStream log;

	public InputLogStream(InputStream src, OutputStream log) {
		super(src);
		this.log = log;
    }

	@Override
    public int read() throws IOException {
        int c;
        
        c = in.read();
        if (c != -1) {
            log.write((char) c);
            if (c == '\n') {
            	log.flush();
            }
        }
        return c;
    }
	
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int result;
        
        result = in.read(b, off, len);
        if (result != -1) {
            log.write(b, off, result);
            log.flush();
        }
        return result;
    }
}
