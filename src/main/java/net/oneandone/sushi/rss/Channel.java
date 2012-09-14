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

import net.oneandone.sushi.xml.Builder;
import net.oneandone.sushi.xml.Selector;
import net.oneandone.sushi.xml.XmlException;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

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
