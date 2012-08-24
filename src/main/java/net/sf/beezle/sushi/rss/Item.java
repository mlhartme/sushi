/**
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

import net.sf.beezle.sushi.xml.Builder;
import net.sf.beezle.sushi.xml.Selector;
import net.sf.beezle.sushi.xml.XmlException;
import org.w3c.dom.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Item {
    public static Item fromXml(Selector selector, Element item) throws XmlException {
        Item result;
        String str;
        
        result = new Item();
        result.title = selector.string(item, "title");
        result.link = selector.string(item, "link");
        result.description = selector.stringOpt(item, "description");
        result.author = selector.string(item, "author");
        result.guid = selector.string(item, "guid");
        str = selector.string(item, "pubDate");
        try {
            synchronized (FORMAT) {
                result.pubDate = FORMAT.parse(str);
            }
        } catch (ParseException e) {
            throw new XmlException("invalid pubDate", e);
        }
        return result;
    }

    //--

    // rft822 time format:
    public static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    private String title;
    private String link;
    /** may be null */
    private String description;
    private String author;
    private String guid;
    private Date pubDate;

    public Item() {
        this("", "", null, "", "", new Date());
    }
    
    public Item(String title, String link, String description, String author, String guid, Date pubDate) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.author = author;
        this.guid = guid;
        this.pubDate = pubDate;
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
    
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getGuid() {
        return guid;
    }
    public void setGuid(String guid) {
        this.guid = guid;
    }
    public Date getPubDate() {
        return pubDate;
    }
    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public void addXml(Element channel) {
        Element item;
        
        item = Builder.element(channel, "item");
        Builder.textElement(item, "title", title);
        Builder.textElement(item, "link", link);
        if (description != null) {
            Builder.textElement(item, "description", description);
        }
        Builder.textElement(item, "author", author);
        Builder.textElement(item, "guid", guid);
        synchronized (FORMAT) {
            Builder.textElement(item, "pubDate", FORMAT.format(pubDate));
        }
    }
}
