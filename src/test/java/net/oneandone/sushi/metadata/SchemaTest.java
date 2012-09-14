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
package net.oneandone.sushi.metadata;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.metadata.annotation.AnnotationSchema;
import net.oneandone.sushi.metadata.model.Engine;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;
import net.oneandone.sushi.xml.Builder;
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
