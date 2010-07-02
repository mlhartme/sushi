package de.ui.sushi.fs.webdav;

import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import org.junit.Test;

public class WebdavNodeNexusFullTest {
    @Test
    public void exists() throws Exception {
        IO io;
        Node repo;

        io = new IO();
        repo = io.validNode("http://mavenrepo.united.domain:8081/nexus/content/repositories/1und1-stable");
        repo.checkExists();
    }
}
