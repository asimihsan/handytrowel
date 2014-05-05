/** ========================================================================
  * handytrowel: src/main/java/extraction/LinkExtractor.java
  * Get 'a' link href's from links that are children of the article.
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

package com.asimihsan.handytrowel.extraction;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.HTMLFetcher;

public final class LinkExtractor {
    public static final LinkExtractor INSTANCE = new LinkExtractor();

    /**
     * Black list of regular expression strings for matching links never to return.
     */
    private static final List<String> blackListRegularExpressions = Lists.newArrayList(
                "https://www.facebook.com/sharer/sharer.php",
                "http://www.facebook.com/share.php",
                "https://twitter.com/intent/tweet",
                "http://pinterest.com/pin/create/bookmarklet",
                "http://www.reddit.com/submit",
                "https://plus.google.com/share",
                "http://www.reddit.com/submit",
                "http://del.icio.us/post",
                "http://tapiture.com/bookmarklet/image",
                "http://www.stumbleupon.com/submit",
                "http://www.linkedin.com/shareArticle",
                "http://slashdot.org/bookmark.pl",
                "http://technorati.com/faves",
                "http://posterous.com/share",
                "http://www.tumblr.com/share",
                "http://www.newsvine.com/_tools/seed",
                "http://ping.fm/ref",
                "http://www.friendfeed.com/share"
            );
    /**
     * Regular expression of black-listed links to never return.
     */
    private static final Pattern blackList = Pattern.compile(
                Joiner.on("|").join(blackListRegularExpressions));

    /**
     * Returns the singleton instance of {@link ImageExtractor}.
     *
     * @return the singleton instance of {@link ImageExtractor}.
     */
    public static LinkExtractor getInstance() {
        return INSTANCE;
    }

    private LinkExtractor() {
    }

    /**
     * Processes the given {@link TextDocument} and the original HTML text (as a
     * String).
     *
     * @param doc
     *            The processed {@link TextDocument}.
     * @param origHTML
     *            The original HTML document.
     * @return A List of enclosed {@link Image}s
     * @throws BoilerpipeProcessingException
     */
    public List<String> process(final TextDocument doc,
                                final String origHTML) throws BoilerpipeProcessingException {
        return process(doc, new InputSource(
                           new StringReader(origHTML)));
    }

    /**
     * Processes the given {@link TextDocument} and the original HTML text (as an
     * {@link InputSource}).
     *
     * @param doc
     *            The processed {@link TextDocument}.
     *            The original HTML document.
     * @return A List of enclosed links
     * @throws BoilerpipeProcessingException
     */
    public List<String> process(final TextDocument doc,
                                final InputSource is) throws BoilerpipeProcessingException {
        final Implementation implementation = new Implementation();
        implementation.process(doc, is);

        return implementation.linksHighlight;
    }

    /**
     * Fetches the given {@link URL} using {@link HTMLFetcher} and processes the
     * retrieved HTML using the specified {@link BoilerpipeExtractor}.
     *
     *            The processed {@link TextDocument}.
     *            The original HTML document.
     * @return A List of enclosed links
     * @throws BoilerpipeProcessingException
     */
    public List<String> process(final URL url, final BoilerpipeExtractor extractor)
    throws IOException, BoilerpipeProcessingException, SAXException {
        final HTMLDocument htmlDoc = HTMLFetcher.fetch(url);

        final TextDocument doc = new BoilerpipeSAXInput(htmlDoc.toInputSource())
        .getTextDocument();
        extractor.process(doc);

        final InputSource is = htmlDoc.toInputSource();

        return process(doc, is);
    }


    private final class Implementation extends AbstractSAXParser implements
        ContentHandler {
        List<String> linksHighlight = new ArrayList<>();
        private List<String> linksBuffer = new ArrayList<>();

        private int inIgnorableElement = 0;
        private int characterElementIdx = 0;
        private final BitSet contentBitSet = new BitSet();

        private boolean inHighlight = false;

        Implementation() {
            super(new HTMLConfiguration());
            setContentHandler(this);
        }

        void process(final TextDocument doc, final InputSource is)
        throws BoilerpipeProcessingException {
            for (TextBlock block : doc.getTextBlocks()) {
                if (block.isContent()) {
                    final BitSet bs = block.getContainedTextElements();
                    if (bs != null) {
                        contentBitSet.or(bs);
                    }
                }
            }

            try {
                parse(is);
            } catch (SAXException e) {
                throw new BoilerpipeProcessingException(e);
            } catch (IOException e) {
                throw new BoilerpipeProcessingException(e);
            }
        }

        public void endDocument() throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException {
        }

        public void processingInstruction(String target, String data)
        throws SAXException {
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void startElement(String uri, String localName, String qName,
                                 Attributes atts) throws SAXException {
            TagAction ta = TAG_ACTIONS.get(localName);
            if (ta != null) {
                ta.beforeStart(this, localName);
            }

            try {
                if (inIgnorableElement == 0) {
                    if(inHighlight && "A".equalsIgnoreCase(localName)) {
                        String href = atts.getValue("href");
                        if((href != null) &&
                                (href.length() > 0) &&
                                !(blackList.matcher(href).lookingAt())) {
                            linksBuffer.add(href);
                        }
                    }
                }
            } finally {
                if (ta != null) {
                    ta.afterStart(this, localName);
                }
            }
        }

        public void endElement(String uri, String localName, String qName)
        throws SAXException {
            TagAction ta = TAG_ACTIONS.get(localName);
            if (ta != null) {
                ta.beforeEnd(this, localName);
            }

            try {
                if (inIgnorableElement == 0) {
                    //
                }
            } finally {
                if (ta != null) {
                    ta.afterEnd(this, localName);
                }
            }
        }

        public void characters(char[] ch, int start, int length)
        throws SAXException {
            characterElementIdx++;
            if (inIgnorableElement == 0) {

                boolean highlight = contentBitSet.get(characterElementIdx);
                if(!highlight) {
                    if(length == 0) {
                        return;
                    }
                    boolean justWhitespace = true;
                    for(int i=start; i<start+length; i++) {
                        if(!Character.isWhitespace(ch[i])) {
                            justWhitespace = false;
                            break;
                        }
                    }
                    if(justWhitespace) {
                        return;
                    }
                }

                inHighlight = highlight;
                if(inHighlight) {
                    linksHighlight.addAll(linksBuffer);
                    linksBuffer.clear();
                }
            }
        }

        public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
        }

    }


    private static final TagAction TA_IGNORABLE_ELEMENT = new TagAction() {
        void beforeStart(final Implementation instance, final String localName) {
            instance.inIgnorableElement++;
        }

        void afterEnd(final Implementation instance, final String localName) {
            instance.inIgnorableElement--;
        }
    };

    private static Map<String, TagAction> TAG_ACTIONS = new HashMap<String, TagAction>();
    static {
        TAG_ACTIONS.put("STYLE", TA_IGNORABLE_ELEMENT);
        TAG_ACTIONS.put("SCRIPT", TA_IGNORABLE_ELEMENT);
        TAG_ACTIONS.put("OPTION", TA_IGNORABLE_ELEMENT);
        TAG_ACTIONS.put("NOSCRIPT", TA_IGNORABLE_ELEMENT);
        TAG_ACTIONS.put("EMBED", TA_IGNORABLE_ELEMENT);
        TAG_ACTIONS.put("APPLET", TA_IGNORABLE_ELEMENT);
        TAG_ACTIONS.put("LINK", TA_IGNORABLE_ELEMENT);

        TAG_ACTIONS.put("HEAD", TA_IGNORABLE_ELEMENT);
    }

    private abstract static class TagAction {
        void beforeStart(final Implementation instance, final String localName) {
        }

        void afterStart(final Implementation instance, final String localName) {
        }

        void beforeEnd(final Implementation instance, final String localName) {
        }

        void afterEnd(final Implementation instance, final String localName) {
        }
    }
}
