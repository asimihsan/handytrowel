/** ========================================================================
  * handytrowel: src/main/java/nlp/WordReader.java
  * Read line-delimited words from a file with hash comments.
  * ========================================================================
  * Copyright (c) 2014, Asim Ihsan, All rights reserved.
  * <http://www.asimihsan.com>
  * https://github.com/asimihsan/handytrowel/blob/master/LICENSE
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published
  * by the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  * ========================================================================
  */

package com.asimihsan.handytrowel.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Read line-delimited words from a file with hash comments.
 *
 * @author Asim Ihsan
 */
public class WordReader {

    private final String resourcePath;

    private WordReader(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public static WordReader WordReaderWithResourcePath(String resourcePath) {
        WordReader reader = new WordReader(resourcePath);
        return reader;
    }

    public List<String> getWords() throws IOException {
        List<String> words = new LinkedList<>();
        try (
            InputStream is = getClass().getResourceAsStream(this.resourcePath);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
        ) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                if (line.trim().length() == 0)
                    continue;
                words.add(line.trim());
            }
        }
        return words;
    }

}
