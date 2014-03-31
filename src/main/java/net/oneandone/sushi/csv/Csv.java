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
package net.oneandone.sushi.csv;

import net.oneandone.sushi.fs.LineFormat;
import net.oneandone.sushi.fs.LineReader;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A list of lines. http://de.wikipedia.org/wiki/CSV-Datei. */
public class Csv implements Iterable<Line> {
    public static Csv read(Format format, Node node) throws IOException {
        StringBuilder msg;
        Csv csv;
        String line;

        try (Reader orig = node.createReader();
             LineReader src = new LineReader(orig, new LineFormat(LineFormat.GENERIC_SEPARATOR, LineFormat.Trim.SEPARATOR))) {
            csv = new Csv(format);
            msg = new StringBuilder();
            while (true) {
                line = src.next();
                if (line == null) {
                    if (msg.length() > 0) {
                        throw new CsvExceptions(msg.toString());
                    }
                    return csv;
                }
                try {
                    csv.add(line);
                } catch (CsvLineException e) {
                    if (msg.length() > 0) {
                        msg.append('\n');
                    }
                    msg.append(src.toString()).append(":").append(src.getLine()).append(": ").append(e.getMessage());
                }
            }
        }

    }

    //--

    private final Format format;
    private final List<Line> lines;

    public Csv(Format format) {
        this.format = format;
        this.lines = new ArrayList<Line>();
    }

    public Format getFormat() {
        return format;
    }

    public int size() {
        return lines.size();
    }

    public Line get(int line) {
        return lines.get(line);
    }

    public Iterator<Line> iterator() {
        return lines.iterator();
    }

    public void add(Line current) {
        if (format.merged && current.size() > 0) {
            for (Line line : lines) {
                if (line.equalsAfter(1, current)) {
                    line.merge(current, 0);
                    return;
                }
            }
        }
        lines.add(current);
    }

    public Csv addAll(String ... lines) throws CsvLineException {
        for (String line : lines) {
            add(line);
        }
        return this;
    }

    public Csv add(String line) throws CsvLineException {
        add(format.read(line));
        return this;
    }


    public void write(Node file) throws IOException {
        try (Writer dest = file.createWriter()) {
            write(dest);
        }
    }

    public void write(Writer dest) throws IOException {
        for (Line line : lines) {
            format.write(line, dest);
        }
        dest.flush();
    }

    @Override
    public String toString() {
        StringWriter dest;

        dest = new StringWriter();
        try {
            write(dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dest.toString();
    }
}
