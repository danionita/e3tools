/*
 * Copyright (C) 2015, 2016 Dan Ionita 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package e3fraud.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * A search and replace input stream.
 *
 * @author jcummings
 *
 */
public class SearchAndReplaceInputStream extends InputStream {

    private final InputStream is;
    private final char[] search;
    private final char[] replace;
    private int len, pos, idx;
    private char ch, buf[];

    public SearchAndReplaceInputStream(InputStream is, String search, String replace) {
        this.is = is;
        this.search = search.toCharArray();
        this.replace = replace.toCharArray();

        len = this.search.length;
        pos = 0;
        idx = -1;

        ch = this.search[0];
        buf = new char[Math.max(this.search.length, this.replace.length)];
    }

    @Override
    public int read() throws IOException {
        if (idx == -1) {
            idx = 0;

            int i = -1;
            while ((i = is.read()) != -1 && (buf[pos] = (char) i) == ch) {
                if (++pos == len) {
                    break;
                }

                ch = search[pos];
            }

            if (pos == len) {
                buf = new char[Math.max(this.search.length, this.replace.length)];
                System.arraycopy(replace, 0, buf, 0, replace.length);
            }

            pos = 0;
            ch = search[pos];
        }

        int toReturn = -1;
        if (idx > -1 && buf[idx] != 0) {
            toReturn = buf[idx];
            buf[idx] = 0;
            if (idx < buf.length - 1 && buf[idx + 1] != 0) {
                idx++;
            } else {
                idx = -1;
                buf = new char[Math.max(this.search.length, this.replace.length)];
            }
        }

        return toReturn;
    }
}
