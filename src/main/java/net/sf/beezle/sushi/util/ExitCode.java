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

package net.sf.beezle.sushi.util;

/** Indicates that a program terminated with a non-zero result. TODO: rename to ProgramExitCode? */
public class ExitCode extends ProgramException {
    public final int code;
    public final String output;

    public ExitCode(Program program, int code) {
        this(program, code, "");
    }

    public ExitCode(Program program, int code, String output) {
        super(program, program.getBuilder().command().get(0) + " failed with exit code " + code + ", output: " + output.trim());
        this.code = code;
        this.output = output;
    }
}
