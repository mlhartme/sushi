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

import junit.framework.TestCase;
import net.oneandone.sushi.xml.Xml;
import net.oneandone.sushi.xml.XmlException;

import java.util.Date;

public class FeedTest extends TestCase {
    private static final Xml XML = new Xml();
    
    public void testEmpty() throws XmlException {
        Feed feed;
        
        feed = new Feed();
        feed = Feed.fromXml(XML.getSelector(), feed.toXml(XML.getBuilder()));
        assertEquals(0, feed.channels().size());
    }

    public void testNormal() throws XmlException {
        Feed feed;
        Channel channel;
        Item item;
        Date date = new Date();
        
        feed = new Feed();
        channel = new Channel();
        channel.items().add(new Item());
        channel.items().add(new Item());
        feed.channels().add(channel);
        
        channel = new Channel("t", "l", "d");
        channel.items().add(new Item("t", "l", "d", "a", "g", date));
        feed.channels().add(channel);
        
        feed = Feed.fromXml(XML.getSelector(), feed.toXml(XML.getBuilder()));
        assertEquals(2, feed.channels().size());
        assertEquals(2, feed.channels().get(0).items().size());

        channel = feed.channels().get(1);
        assertEquals("t", channel.getTitle());
        assertEquals("l", channel.getLink());
        assertEquals("d", channel.getDescription());
        assertEquals(1, channel.items().size());
        
        item = channel.items().get(0);
        assertEquals("t", item.getTitle());
        assertEquals("l", item.getLink());
        assertEquals("d", item.getDescription());
        assertEquals("a", item.getAuthor());
        assertEquals("g", item.getGuid());
        // dates do not equal because milli seconds get lost:
        assertEquals(date.toString(), item.getPubDate().toString());
    }
}
