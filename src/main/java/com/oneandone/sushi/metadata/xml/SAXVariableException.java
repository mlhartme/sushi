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

package com.oneandone.sushi.metadata.xml;

import org.xml.sax.Locator;

import com.oneandone.sushi.metadata.Variable;

public class SAXVariableException extends SAXLoaderException {
    public final Variable<?> variable;
    
    public SAXVariableException(Variable<?> variable, Locator locator, Throwable e) {
        super(variable.item.getName() + ": " + e.getMessage(), locator);
        
        this.variable = variable;
    }
}
