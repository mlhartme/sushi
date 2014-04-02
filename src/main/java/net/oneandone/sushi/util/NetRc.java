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
public class NetRc {

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

    private static class NetRcParser {

        private final HashMap<String, NetRcAuthenticator> authenticators;
        private final NetRcStreamTokenizer tokenizer;
        String entryName;
        String login;
        String password;
        String toplevel;

        private NetRcParser(Reader in, HashMap<String, NetRcAuthenticator> authenticators) {
            this.tokenizer = new NetRcStreamTokenizer(in);
            this.authenticators = authenticators;
        }

        private void parse() throws IOException {
            while (true) {
                resetFields();
                if (!parseMachineOrDefault()) break;
                parseAfterMachineOrDefault();
            }
        }

        private void resetFields() {
            login = null;
            password = null;
        }

        private boolean parseMachineOrDefault() throws NetRcIllegalArgumentException, IOException {
            int tt = nextToken();
            toplevel = sval();
            final String outerToken = sval();
            if (tt == StreamTokenizer.TT_EOF) {
                return false;
            } else if (outerToken.equals("machine")) {
                nextToken();
                entryName = sval();
            } else if (outerToken.equals("default")) {
                entryName = "default";
            } else if (outerToken.equals("macdef")) {
                throw new NetRcIllegalArgumentException(("macdef not supported"), lineno(), outerToken);
            } else {
                throw new NetRcIllegalArgumentException("bad toplevel", lineno(), toplevel);
            }
            return true;
        }

        private void parseAfterMachineOrDefault() throws NetRcIllegalArgumentException, IOException {
            while (true) {
                nextToken();
                final String sval = sval();
                if (sval == null || sval.startsWith("#") || sval.equals("machine") || sval.equals("") || sval.equals("default")) {
                    if (password != null) {
                        authenticators.put(entryName, new NetRcAuthenticator(login, password));
                        pushBack();
                        break;
                    } else {
                        throw new NetRcIllegalArgumentException("malformed token at toplevel " + toplevel, lineno(), sval);
                    }
                } else if (sval.equals("login") || sval.equals("user")) {
                    nextToken();
                    login = sval();
                } else if (sval.equals("account")) {
                    throw new NetRcIllegalArgumentException("account not supported", lineno(), sval);
                } else if (sval.equals("password")) {
                    nextToken();
                    password = sval();
                } else {
                    throw new NetRcIllegalArgumentException("bad follower token", lineno(), sval);
                }
            }
        }

        private String sval() {
            return tokenizer.sval;
        }

        private void pushBack() {
            tokenizer.pushBack();
        }

        private int lineno() {
            return tokenizer.lineno();
        }

        private int nextToken() throws IOException {
            return tokenizer.nextToken();
        }
    }

    public void parse(Reader in) throws IOException {
        new NetRcParser(in, authenticators).parse();
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
