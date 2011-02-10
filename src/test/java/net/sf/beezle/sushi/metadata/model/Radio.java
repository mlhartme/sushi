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

package net.sf.beezle.sushi.metadata.model;

import net.sf.beezle.sushi.metadata.annotation.Option;
import net.sf.beezle.sushi.metadata.annotation.Type;
import net.sf.beezle.sushi.metadata.annotation.Value;

@Type
public class Radio {
    @Value private boolean cd;
    @Value private int speaker;
    @Option private String pin;
    
    public Radio() {
    }
    
    public boolean getCd() {
        return cd;
    }
    
    public void setCd(boolean cd) {
        this.cd = cd;
    }
    
    public int getSpeaker() {
        return speaker;
    }
    
    public void setSpeaker(int speaker) {
        this.speaker = speaker;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
