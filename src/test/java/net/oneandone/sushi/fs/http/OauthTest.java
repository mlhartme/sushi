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

import net.oneandone.sushi.fs.World;

public class OauthTest {

    public static void main(String[] args) throws Exception {
        World world;
        HttpNode folder;
        Oauth oauth;

        world = World.create();
        oauth = Oauth.load(world.getHome().join(".smuggler.properties"));
        HttpFilesystem.wireLog("sushiwire.log");
        folder = (HttpNode) world.node("https://api.smugmug.com/api/v2/folder/user/mlhartme");
        folder.getRoot().setOauth(oauth);
        folder.getRoot().addExtraHeader("Accept", "application/json");
        System.out.println("get: " + folder.readString());
    }
}
