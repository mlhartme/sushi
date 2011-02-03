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

package de.ui.sushi.metadata.model;

import java.util.ArrayList;
import java.util.List;

import de.ui.sushi.metadata.annotation.Type;
import de.ui.sushi.metadata.annotation.Option;
import de.ui.sushi.metadata.annotation.Sequence;
import de.ui.sushi.metadata.annotation.Value;

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
