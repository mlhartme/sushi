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
package net.oneandone.sushi.metadata.model;

import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

@Type
public class Engine {
    @Value boolean turbo;
    @Value int ps;
    
    public Engine() {
        this(false, 0);
    }

    public Engine(boolean turbo, int ps) {
        this.turbo = turbo;
        this.ps = ps;
    }

    public boolean getTurbo() {
        return turbo;
    }
    
    public void setTurbo(boolean turbo) {
        this.turbo = turbo;
    }

    public int getPs() {
        return ps;
    }
    
    public void setPs(int ps) {
        this.ps = ps;
    }
}
