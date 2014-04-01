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

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashMap;
import java.util.Objects;

/**
 * Basic <tt>.netrc</tt> parser.
 *
 * Note that currently neither the <tt>account</tt> nor <tt>macdef</tt> parameters are supported.
 *
 * @see <a href="http://hg.python.org/cpython/file/3a1db0d2747e/Lib/netrc.py">Python implementation</a>
 * @see <a href="http://linux.die.net/man/5/netrc">netrc man page</a>
 *
 * @author Mirko Friedenhagen
 */
public class NetRcParser {

    private final static CharSequence ADDITIONAL_CHARS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    final HashMap<String, NetRcAuthenticator> authenticators = new HashMap<>();

    /**
     * Returns a {@link NetRcAuthenticator} for the given host.
     *
     * @param hostname to look for.
     * @return authentication information for hostname.
     */
    public NetRcAuthenticator getAuthenticators(final String hostname) {
        return authenticators.get(hostname);
    }

    public void parse(Reader in) throws IOException {
        final NetRcStreamTokenizer tokenizer = new NetRcStreamTokenizer(in);

        String entryName;
        String login;
        String password;
        String toplevel;
        while (true) {
            int tt = tokenizer.nextToken();
            final String sval = tokenizer.sval;
            toplevel = sval;
            if (tt == StreamTokenizer.TT_EOF) {
                break;
            } else if (sval.equals("machine")) {
                tokenizer.nextToken();
                entryName = tokenizer.sval;
            } else if (sval.equals("default")) {
                entryName = "default";
            } else if (sval.equals("macdef")) {
                throw new NetRcIllegalArgumentException(("macdef not supported"), tokenizer.lineno(), sval);
            } else {
                throw new NetRcIllegalArgumentException("bad toplevel", tokenizer.lineno(), toplevel);
            }
            login = "";
            password = null;
            while (true) {
                tokenizer.nextToken();
                final String sval1 = tokenizer.sval;
                if (sval1 == null || sval1.startsWith("#") || sval1.equals("machine") || sval1.equals("") || sval1.equals("default")) {
                    if (password != null) {
                        authenticators.put(entryName, new NetRcAuthenticator(login, password));
                        tokenizer.pushBack();
                        break;
                    } else {
                        throw new NetRcIllegalArgumentException("malformed token at toplevel " + toplevel, tokenizer.lineno(), sval1);
                    }
                } else if (sval1.equals("login") || sval1.equals("user")) {
                    tokenizer.nextToken();
                    login = tokenizer.sval;
                } else if (sval1.equals("account")) {
                    throw new NetRcIllegalArgumentException("account not supported", tokenizer.lineno(), sval1);
                } else if (sval1.equals("password")) {
                    tokenizer.nextToken();
                    password = tokenizer.sval;
                } else {
                    throw new NetRcIllegalArgumentException("bad follower token", tokenizer.lineno(), sval1);
                }
            }
        }

    }

    /**
     * Holder class for authentication information.
     */
    public static class NetRcAuthenticator {

        private final String user;
        private final String pass;

        /**
         * @param user username
         * @param pass password
         */
        public NetRcAuthenticator(String user, String pass) {
            this.user = user;
            this.pass = pass;
        }

        /**
         * @return the user
         */
        public String getUser() {
            return user;
        }

        /**
         * @return the pass
         */
        public String getPass() {
            return pass;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.user);
            hash = 17 * hash + Objects.hashCode(this.pass);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NetRcAuthenticator other = (NetRcAuthenticator) obj;
            if (!Objects.equals(this.user, other.user)) {
                return false;
            }
            if (!Objects.equals(this.pass, other.pass)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "NetRcAuthenticator{" + "user=" + user + ", pass=" + pass + '}';
        }

    }

    public static class NetRcIllegalArgumentException extends IllegalArgumentException {
        public NetRcIllegalArgumentException(String message, int lineNo, String token) {
            super(message + " at line " + lineNo + ": " + token);
        }
    }

    private static class NetRcStreamTokenizer extends StreamTokenizer {

        public NetRcStreamTokenizer(Reader r) {
            super(r);
        }
        {
            resetSyntax();
            wordChars('a', 'z');
            wordChars('A', 'Z');
            wordChars('0', '9');
            wordChars(128 + 32, 255);
            whitespaceChars(0, ' ');
            for (int i = 0; i < ADDITIONAL_CHARS.length(); i++) {
                wordChars(ADDITIONAL_CHARS.charAt(i), ADDITIONAL_CHARS.charAt(i));
            }
            commentChar('#');
        }
    }

}
