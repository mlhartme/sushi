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
package net.oneandone.sushi.launcher;

/** Indicates that a process terminated with a non-zero result. */
public class ExitCode extends Failure {
    public final int code;
    public final String output;

    public ExitCode(Launcher launcher, int code) {
        this(launcher, code, "");
    }

    public ExitCode(Launcher launcher, int code, String output) {
        super(launcher, launcher.getBuilder().command().get(0) + " failed with exit code " + code + ", output: " + output.trim());
        this.code = code;
        this.output = output;
    }
}
