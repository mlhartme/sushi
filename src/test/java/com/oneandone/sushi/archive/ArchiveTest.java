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

package com.oneandone.sushi.archive;

import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.fs.Node;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArchiveTest {
    private static final IO IO_OBJ = new IO();

    private List<String> directories;
    private List<String> fileNames;
    private List<String> fileData;
    
    @Before 
    public void setUp() {
        directories = new ArrayList<String>();
        fileNames = new ArrayList<String>();
        fileData = new ArrayList<String>();
    }

    @Test 
    public void empty() throws IOException {
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
        
        zip = Archive.createZip(IO_OBJ);
        for (String dir : directories) {
            zip.data.join(dir).mkdir();
        }
        for (int i = 0; i < fileNames.size(); i++) {
            zip.data.join(fileNames.get(i)).writeString(fileData.get(i));
        }

        file = IO_OBJ.getTemp().createTempFile();
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
        
        archive = Archive.createJar(IO_OBJ);
        for (String dir : directories) {
            archive.data.join(dir).mkdir();
        }
        for (int i = 0; i < fileNames.size(); i++) {
            archive.data.join(fileNames.get(i)).writeString(fileData.get(i));
        }

        file = IO_OBJ.getTemp().createTempFile();
        archive.save(file);
        archive = Archive.loadJar(file);
        assertEquals(directories.size() + fileNames.size(), archive.data.find("**/*").size());
        for (String dir : directories) {
            assertTrue(archive.data.join(dir).isDirectory());
        }
        for (int i = 0; i < fileNames.size(); i++) {
            assertEquals(fileData.get(i), archive.data.join(fileNames.get(i)).readString());
        }
        reloaded = IO_OBJ.getTemp().createTempFile();
        archive.save(reloaded);
        // TODO do not try "diff" on jar files, because they contain a timestamp that occasionally 
        // yields differences
    }
}
