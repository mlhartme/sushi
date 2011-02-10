/*
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

package net.sf.beezle.sushi.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiOutputStream extends OutputStream {
    public static MultiOutputStream createNullStream() {
        return new MultiOutputStream();
    }

    public static MultiOutputStream createTeeStream(OutputStream ... dests) {
        MultiOutputStream result;
        
        result = new MultiOutputStream();
        result.dests.addAll(Arrays.asList(dests));
        return result;
    }
    
    private final List<OutputStream> dests;
    
    public MultiOutputStream() {
        dests = new ArrayList<OutputStream>();
    }

    public List<OutputStream> dests() {
        return dests;
    }

    //--
    
    @Override
    public void write(int c) throws IOException {
        for (OutputStream dest : dests) {
            dest.write(c);
        }
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream dest : dests) {
            dest.flush();
        }
    }

    @Override
    public void close() throws IOException {
        for (OutputStream dest : dests) {
            dest.close();
        }
    }
}
