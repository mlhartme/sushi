package de.ui.sushi.fs.webdav;

import de.ui.sushi.util.Strings;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LineLogger {
    private final Logger logger;
    private final String prefix;
    private final StringBuilder line;

    public LineLogger(Logger logger, String prefix) {
        this.logger = logger;
        this.prefix = prefix;
        this.line = new StringBuilder(prefix);
    }

    public void log(byte ... bytes) {
    	log(bytes, 0, bytes.length);
    }
    
    public void log(byte[] bytes, int ofs, int length) {
    	log(new String(bytes, ofs, length));
    }

    public void log(String str) {
        int prev;
        int idx;

        prev = 0;
        while (true) {
            idx = str.indexOf('\n', prev);
            if (idx == -1) {
                line.append(str.substring(prev, str.length()));
                return;
            }
            idx++;
            line.append(str.substring(prev, idx));
            logger.log(Level.FINE, Strings.escape(line.toString()));
            line.setLength(prefix.length());
            prev = idx;
        }
    }
}
