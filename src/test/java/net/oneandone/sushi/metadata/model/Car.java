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

import net.oneandone.sushi.metadata.annotation.Option;
import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@Type
public class Car {
    @Value private String name;
    @Value private Kind kind;
    @Value private int seats;
    @Value private Engine engine;
    @Option private Radio radio;
    @Sequence(String.class) private final List<String> commentList;
    
    public Car() {
        this("", Kind.NORMAL, 0, new Engine(), null);
    }

    public Car(String name, Kind kind, int seats, Engine engine, Radio radio) {
        this.name = name;
        this.kind = kind;
        this.seats = seats;
        this.engine = engine;
        this.radio = radio;
        this.commentList = new ArrayList<String>();
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Kind getKind() {
        return kind;
    }
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }
    
    /** @return never null */
    public Engine getEngine() {
        return engine;
    }
    
    public void setEngine(Engine engine) {
        this.engine = engine;
    }
    
    public Radio getRadio() {
        return radio;
    }
    
    public void setRadio(Radio radio) {
        this.radio = radio;
    }
    
    public List<String> commentList() {
        return commentList;
    }
}
