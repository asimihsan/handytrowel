/** ========================================================================
  * handytrowel: src/main/java/cli/Main.java
  * Command-line interface that executes handytrowel.
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

package com.asimihsan.handytrowel.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;

import com.asimihsan.handytrowel.extraction.LinkExtractor;
import com.asimihsan.handytrowel.network.HTMLFetcher;
import com.asimihsan.handytrowel.network.HTMLFetcher.HTMLFetcherBuilder;
import com.asimihsan.handytrowel.nlp.TextAnalyzer;
import com.asimihsan.handytrowel.nlp.TextAnalyzer.TextAnalyzerBuilder;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Main {

    // Positional arguments
    @Argument private List<String> arguments = new ArrayList<>();

    public static void main(String[] args) throws SAXException {
        new Main().doMain(args);
    }

    public void doMain(String[] args) throws SAXException {
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        try {
            parser.parseArgument(args);
            if (arguments.isEmpty())
                throw new CmdLineException(parser, "No arguments were given");
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("handytrowel [URL]");
            parser.printUsage(System.err);
            System.err.println();
            System.exit(1);
        }
        String url = arguments.get(0);

        HTMLFetcher htmlFetcher = new HTMLFetcherBuilder()
        .timeoutMillis(30 * 10000)
        .build();
        String pageSource = null;
        try {
            pageSource = htmlFetcher.getPageSource(url);
        } catch (TimeoutException e) {
            System.err.println(e.getStackTrace());
            System.exit(1);
        }

        String extractedBody = null;
        List<String> links = null;
        try {
            final HTMLDocument htmlDoc = new HTMLDocument(pageSource);
            final TextDocument doc = new BoilerpipeSAXInput(htmlDoc.toInputSource()).getTextDocument();
            ArticleExtractor.INSTANCE.process(doc);
            final InputSource is = htmlDoc.toInputSource();
            links = LinkExtractor.INSTANCE.process(doc, is);
            /*
             * working article sentences extractor
             * !!AI do I have to call this again, or can I piggy back on LinkExtractor?
             */
            extractedBody = ArticleExtractor.INSTANCE.getText(pageSource);

        } catch (BoilerpipeProcessingException e) {
            System.err.println(e.getStackTrace());
            System.exit(1);
        }

        TextAnalyzer analyzer = new TextAnalyzerBuilder()
        .body(extractedBody)
        .build()
        .analyze();
        List<String> tokens = analyzer.getTokens();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        Map<String, Object> articleData = new HashMap<>();
        articleData.put("extractedBody", extractedBody);
        articleData.put("links", links);
        articleData.put("tokens", tokens);
        try {
            mapper.writeValue(System.out, articleData);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (JsonMappingException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
