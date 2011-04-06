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

package net.sf.beezle.sushi.rss;

import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.xml.Builder;
import net.sf.beezle.sushi.xml.Selector;
import net.sf.beezle.sushi.xml.XmlException;
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
