/*
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
package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.fs.http.model.StatusLine;

import java.io.IOException;

public class StatusException extends IOException {
    public static StatusException forResponse(Response response) {
        return new StatusException(response.getStatusLine(), response.getBodyBytes());
    }

    private final StatusLine statusline;
    private final byte[] responseBytes;

    public StatusException(StatusLine statusline, byte[] responseBytes) {
        super(Integer.toString(statusline.code));
        this.statusline = statusline;
        this.responseBytes = responseBytes;
    }

    public StatusLine getStatusLine() {
        return statusline;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }
}