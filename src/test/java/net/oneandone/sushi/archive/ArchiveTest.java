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
package net.oneandone.sushi.archive;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArchiveTest {
    private static final World WORLD = new World();

    private List<String> directories;
    private List<String> fileNames;
    private List<String> fileData;
    
    @Before 
    public void setUp() {
        directories = new ArrayList<>();
        fileNames = new ArrayList<>();
        fileData = new ArrayList<>();
    }

    @Test 
    public void one() throws IOException {
        dir("one");
        checkZip();
        checkJar();
    }

    @Test
    public void normal() throws IOException {
        dir("dir");
        file("file", "data");
        file("empty", "");
        checkZip();
        checkJar();
    }
    
    @Test(expected=ArchiveException.class) 
    public void explicitManifest() throws IOException {
        dir(Archive.META_INF);
        file(Archive.MANIFEST, "foo");
        checkZip();
        checkJar();
    }

    private void dir(String name) {
        directories.add(name);
    }
    
    private void file(String name, String data) {
        fileNames.add(name);
        fileData.add(data);
    }

    private void checkZip() throws IOException {
        Node file;
        Archive zip;
        
        zip = Archive.createZip(WORLD);
        for (String dir : directories) {
            zip.data.join(dir).mkdir();
        }
        for (int i = 0; i < fileNames.size(); i++) {
            zip.data.join(fileNames.get(i)).writeString(fileData.get(i));
        }

        file = WORLD.getTemp().createTempFile();
        zip.save(file);
        zip = Archive.loadZip(file);
        assertEquals(directories.size() + fileNames.size(), zip.data.find("**/*").size());
        for (String dir : directories) {
            assertTrue(zip.data.join(dir).isDirectory());
        }
        for (int i = 0; i < fileNames.size(); i++) {
            assertEquals(fileData.get(i), zip.data.join(fileNames.get(i)).readString());
        }
        // TODO do not try "diff" on zip files, because they contain a timestamp that occasionally 
        // yields differences
    }
    
    private void checkJar() throws IOException {
        Node file;
        Archive archive;
        Node reloaded;
        
        archive = Archive.createJar(WORLD);
        for (String dir : directories) {
            archive.data.join(dir).mkdir();
        }
        for (int i = 0; i < fileNames.size(); i++) {
            archive.data.join(fileNames.get(i)).writeString(fileData.get(i));
        }

        file = WORLD.getTemp().createTempFile();
        archive.save(file);
        archive = Archive.loadJar(file);
        assertEquals(directories.size() + fileNames.size(), archive.data.find("**/*").size());
        for (String dir : directories) {
            assertTrue(archive.data.join(dir).isDirectory());
        }
        for (int i = 0; i < fileNames.size(); i++) {
            assertEquals(fileData.get(i), archive.data.join(fileNames.get(i)).readString());
        }
        reloaded = WORLD.getTemp().createTempFile();
        archive.save(reloaded);
        // TODO do not try "diff" on jar files, because they contain a timestamp that occasionally 
        // yields differences
    }
}
