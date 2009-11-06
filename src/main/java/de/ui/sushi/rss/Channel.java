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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import de.ui.sushi.xml.Builder;
import de.ui.sushi.xml.Selector;
import de.ui.sushi.xml.XmlException;

public class Channel {
    public static Channel fromXml(Selector selector, Element channel) throws XmlException {
        Channel result;
        
        result = new Channel();
        result.title = selector.string(channel, "title");
        result.link = selector.string(channel, "link");
        result.description = selector.string(channel, "description");
        for (Element item : selector.elements(channel, "item")) {
            result.items.add(Item.fromXml(selector, item));
        }
        return result;
    }

    private String title;
    private String link;
    private String description;
    private final List<Item> items;
    
    public Channel() {
        this("", "", "");
    }
    
    public Channel(String title, String link, String description) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.items = new ArrayList<Item>();
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<Item> items() {
        return items;
    }
    
    public void add(Item item, int maxItems) {
        int remove;

        items.add(0, item);
        remove = items.size() - maxItems;
        while (remove-- > 0) {
            items.remove(maxItems);
        }
    }
    
    public void addXml(Element rss) {
        Element channel;
        
        channel = Builder.element(rss, "channel");
        Builder.textElement(channel, "title", title);
        Builder.textElement(channel, "link", link);
        Builder.textElement(channel, "description", description);
        for (Item item : items) {
            item.addXml(channel);
        }
    }
}
