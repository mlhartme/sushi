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
package net.oneandone.sushi.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 *
 * @author Mirko Friedenhagen
 */
public class NetRcTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    NetRc sut;

    @Before
    public void setUp() {
        sut = new NetRc();
    }

    @Test
    public void testParse() throws IOException {
        InputStreamReader in = newInputStreamReader(
                "machine svn.dev.java.net login user password pass\n"+
                "machine svn2.dev.java.net user user2 password päss2\n" +
                "#machine svn3.dev.java.net user user3 password pass3\n" +
                "default login userd password passd");
        sut.parse(in);
        Assert.assertEquals(new NetRc.Authenticator("user", "pass"), sut.getAuthenticator("svn.dev.java.net"));
        Assert.assertEquals(new NetRc.Authenticator("user2", "päss2"), sut.getAuthenticator("svn2.dev.java.net"));
        Assert.assertNull(sut.getAuthenticator("svn3.dev.java.net"));
        final NetRc.Authenticator defaultAuthenticator = sut.getAuthenticator("default");
        Assert.assertEquals(new NetRc.Authenticator("userd", "passd"), defaultAuthenticator);
        Assert.assertEquals("userd", defaultAuthenticator.getUser());
        Assert.assertEquals("passd", defaultAuthenticator.getPass());
        Assert.assertEquals("NetRc.Authenticator{user=userd, pass=passd}", String.valueOf(defaultAuthenticator));
    }

    @Test
    public void testParseMultiLine() throws IOException  {
        InputStreamReader in = newInputStreamReader(
                "machine\n\tsvn.dev.java.net\n\tlogin\n\tuser\n\tpassword pass\n\n\n"+
                "machine svn2.dev.java.net\n\tuser user2\n\tpassword päss2\n");
        sut.parse(in);
        Assert.assertEquals(new NetRc.Authenticator("user", "pass"), sut.getAuthenticator("svn.dev.java.net"));
        Assert.assertEquals(new NetRc.Authenticator("user2", "päss2"), sut.getAuthenticator("svn2.dev.java.net"));
    }

    @Test
    public void testParseMacDefNotSupported() throws IOException  {
        InputStreamReader in = newInputStreamReader("macdef foo");
        expectedException.expect(NetRc.NetRcIllegalArgumentException.class);
        expectedException.expectMessage("macdef not supported at line 1: macdef");
        sut.parse(in);
    }

    @Test
    public void testParseAccountNotSupported() throws IOException  {
        InputStreamReader in = newInputStreamReader("machine svn.dev.java.net login user account account\n");
        expectedException.expect(NetRc.NetRcIllegalArgumentException.class);
        expectedException.expectMessage("account not supported at line 1: account");
        sut.parse(in);
    }

    @Test
    public void testParseBadTopLevel() throws IOException  {
        InputStreamReader in = newInputStreamReader("fizzbuzz svn.dev.java.net login user pass account\n");
        expectedException.expect(NetRc.NetRcIllegalArgumentException.class);
        expectedException.expectMessage("bad toplevel at line 1: fizzbuzz");
        sut.parse(in);
    }

    @Test
    public void testParseBadEntryWithBadFollowerToken() throws IOException  {
        InputStreamReader in = newInputStreamReader("machine svn.dev.java.net login user puss account\n");
        expectedException.expect(NetRc.NetRcIllegalArgumentException.class);
        expectedException.expectMessage("bad follower token at line 1: puss");
        sut.parse(in);
    }

    @Test
    public void testParseBadEntryWithoutPassword() throws IOException  {
        InputStreamReader in = newInputStreamReader("machine svn.dev.java.net login user\n");
        expectedException.expect(NetRc.NetRcIllegalArgumentException.class);
        expectedException.expectMessage("malformed token at toplevel machine at line 2: null");
        sut.parse(in);
    }

    private InputStreamReader newInputStreamReader(final String netrc) {
        final ByteArrayInputStream inBytes = new ByteArrayInputStream(
                netrc.getBytes(Charset.forName("utf-8")));
        return new InputStreamReader(inBytes);
    }

}
