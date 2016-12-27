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

import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.Properties;

public class Oauth {
    public static Oauth load(FileNode src) throws IOException {
        Properties p;

        p = src.readProperties();
        return new Oauth(get(p, "consumer.key"), get(p, "consumer.secret"), get(p, "token.id"), get(p, "token.secret"));
    }

    public static String get(Properties p, String key) {
        String value;

        value = p.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("property not found: " + key);
        }
        return value;
    }

    public final String consumerKey;
    public final String consumerSecret;
    public final String tokenId;
    public final String tokenSecret;

    public Oauth(String consumerKey, String consumerSecret, String tokenId, String tokenSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.tokenId = tokenId;
        this.tokenSecret = tokenSecret;
    }
}
