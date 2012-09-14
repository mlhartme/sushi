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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputPumpStream extends Thread {
    private final InputStream in;
    private final OutputStream out;
    private IOException exception;
    private volatile boolean finishing;

    public InputPumpStream(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.exception = null;
        this.finishing = false;
    }

    public void run() {
        try {
            while (true) {
                try {
                    while (!finishing && in.available() == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new Interrupted(e);
                        }
                    }
                    if (finishing) {
                        return;
                    }
                } catch (IOException e) {
                    if (in instanceof BufferedInputStream && "Stream closed".equals(e.getMessage())) {
                        // I'd expected BufferdInputStream.available to return 0 for a closed stream,
                        // but they prefer to throw this exception
                        return;
                    }
                }
                out.write(in.read());
                out.flush();
            }
        } catch (IOException e) {
            exception = e;
        }
    }


    public void finish(Launcher launcher) throws Failure {
        finishing = true;
        try {
            join();
        } catch (InterruptedException e) {
            throw new Interrupted(e);
        }
        if (exception != null) {
            throw new Failure(launcher, exception);
        }
    }
}
