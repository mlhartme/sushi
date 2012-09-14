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
package net.oneandone.sushi.rss;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.xml.Builder;
import net.oneandone.sushi.xml.Selector;
import net.oneandone.sushi.xml.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Feed {
    private final List<Channel> channels;
    
    public static Feed read(Node src) throws XmlException, IOException, SAXException {
        return fromXml(src.getWorld().getXml().getSelector(), src.readXml());
    }

    public static Feed fromXml(Selector selector, Document doc) throws XmlException {
        Feed feed;
        
        feed = new Feed();
        for (Element element : selector.elements(doc.getDocumentElement(), "channel")) {
            feed.channels.add(Channel.fromXml(selector, element));
        }
        return feed;
    }

    public Feed() {
        channels = new ArrayList<Channel>();
    }

    public List<Channel> channels() {
        return channels;
    }

    //-

    public void write(Node dest) throws IOException {
        dest.writeXml(toXml(dest.getWorld().getXml().getBuilder()));
    }        

    public Document toXml(Builder builder) {
        Document doc;
        Element rss;
        
        doc = builder.createDocument("rss");
        rss = doc.getDocumentElement();
        rss.setAttribute("version", "2.0");
        for (Channel channel : channels) {
            channel.addXml(rss);
        }
        return doc;
    }
}
