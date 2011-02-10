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

package com.oneandone.sushi.metadata;

import com.oneandone.sushi.fs.World;
import com.oneandone.sushi.fs.file.FileNode;
import com.oneandone.sushi.metadata.annotation.AnnotationSchema;
import com.oneandone.sushi.metadata.model.Engine;
import com.oneandone.sushi.metadata.reflect.ReflectSchema;
import com.oneandone.sushi.xml.Builder;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SchemaTest {
    private static final World WORLD = new World();

    @Test
    public void simple() throws SAXException, IOException {
        String schema;

        schema = new ReflectSchema().type(String.class).createSchema();
        assertEquals(Type.SCHEMA_HEAD + 
                "  <xs:element name='string' type='xs:string'/>\n" +
                "</xs:schema>",
                schema);
        validate(schema,  "<string>abc</string>");
        validate(schema,  "<string/>");
        try {
            validate(schema,  "<nosuchelement/>");
            fail();
        } catch (SAXException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nosuchelement"));
        }
    }

    @Test
    public void complex() throws SAXException, IOException {
        String schema;

        schema = new AnnotationSchema().type(Engine.class).createSchema();
        assertEquals(Type.SCHEMA_HEAD + 
                "  <xs:element name='engine' type='engine'/>\n" +
                "  <xs:complexType name='engine'>\n" +
                "    <xs:sequence minOccurs='0'>\n" +
                "      <xs:element name='turbo' type='xs:boolean'/>\n" +
                "      <xs:element name='ps' type='xs:int'/>\n" +
                "    </xs:sequence>\n" +
                "    <xs:attributeGroup ref='ids'/>\n" +
                "  </xs:complexType>\n"+
                "</xs:schema>",
                schema);
        validate(schema, "<engine><turbo>true</turbo><ps>2</ps></engine>");
    }

    private static void validate(String schema, String content) throws IOException, SAXException {
        FileNode schemaFile;
        Builder builder;

        schemaFile = WORLD.getTemp().createTempFile();
        schemaFile.writeString(schema);
        builder = new Builder(schemaFile);
        builder.parseString(content);
    }
}
