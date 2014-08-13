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
package net.oneandone.sushi.fs.ssh;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;

import java.io.IOException;
import java.util.Vector;

/** Some documentation: Mac OS: http://www.dribin.org/dave/blog/archives/2007/11/28/ssh_agent_leopard/ */

public class SshAgent {
    public static void configure(JSch jsch) throws IOException {
        USocketFactory factory;
        IdentityRepository repository;
        Vector identities;

        try {
            factory = new JNAUSocketFactory();
            repository = new RemoteIdentityRepository(new SSHAgentConnector(factory));
            identities = repository.getIdentities();
        } catch (AgentProxyException e) {
            throw new IOException("cannot connect to ssh-agent", e);
        }
        for (Object obj : identities) {
            try {
                jsch.addIdentity((Identity) obj, null);
            } catch (JSchException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
