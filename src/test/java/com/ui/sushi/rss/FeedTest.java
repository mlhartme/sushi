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

package de.ui.sushi.rss;

import java.util.Date;

import junit.framework.TestCase;
import de.ui.sushi.xml.Xml;
import de.ui.sushi.xml.XmlException;

public class FeedTest extends TestCase {
    private static final Xml XML = new Xml();
    
    public void testEmpty() throws XmlException {
        Feed feed;
        
        feed = new Feed();
        feed = Feed.fromXml(XML.selector, feed.toXml(XML.builder));
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
        
        feed = Feed.fromXml(XML.selector, feed.toXml(XML.builder));
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
