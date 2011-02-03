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

package de.ui.sushi.util;

import java.io.IOException;
import java.util.List;

/** indicates a processes terminating with a non-zero result */
public class ExitCode extends IOException {
    private static final long serialVersionUID = 2L;
    
    public final List<String> call;
    public final int code;
    public final String output;
    
    public ExitCode(List<String> call, int code) {
        this(call, code, "");
    }

    public ExitCode(List<String> call, int code, String output) {
        super(call.get(0) + " failed with exit code " + code + ", output: " + output.trim());
        
        this.call = call;
        this.code = code;
        this.output = output;
    }
}
