/** ========================================================================
  * handytrowel: src/main/java/nlp/TextAnalyzer.java
  * Normalize/pre-process text, output post-processed text and n-grams.
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import com.google.common.base.Joiner;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.Pair;

/**
 * Take a body of text, perform common pre-processing and normalization
 * tasks, and return the post-processed body and n-gram statistics.
 *
 * @author Asim Ihsan
 */
public class TextAnalyzer {

    /**
     * Body of raw text that you wish to analyze.
     *
     * Required parameter via builder.
     */
    private final String body;

    public static class TextAnalyzerBuilder {
        private String body;

        public TextAnalyzerBuilder body(String body) {
            this.body = body;
            return this;
        }
        public TextAnalyzer build() {
            return new TextAnalyzer(this);
        }
    }

    private TextAnalyzer(TextAnalyzerBuilder builder) {
        this.body = builder.body;
    }

    /**
     * List of tokens that are created by a call to analyze() and then
     * retrieved by a call to getTokens()
     */
    private final List<String> tokens = new LinkedList<>();

    /**
     * Regular expression object that matches for punctuation. Note that
     * this also matches full stops, so we lose sentence information.
     *
     * Sometimes Stanford CoreNLP's tokenizer spits out "'s" and 'n't" on
     * its own, so we ignore single letters before/after punctuation too.
     *
     * Note that Stanford CoreNLP helpfully points out brackets with
     * -lrb- and -rrb-. Let's chuck those too.
     */
    private final Pattern punctuation = Pattern.compile("(?:[a-z]?[\\p{Punct}]+[a-z]?|-[lr].b-)");

    /**
     * A compiled number regular expression so we can replace all using it
     * with $NUMBER.
     */
    private final Pattern number = Pattern.compile("[0-9]+");

    public List<String> getTokens() {
        return tokens;
    }

    public TextAnalyzer analyze() {
        // Stanford CoreNLP, avoid lemmatization as it's very slow to use Porter2 stemming
        // instead. (Porter -> Snowball (Porter2) -> Lancaster is order of stemming
        // aggressiveness.
        //
        // other ideas
        // - remove top 10k most common english words
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, stopword");
        props.setProperty("customAnnotatorClass.stopword", "com.asimihsan.handytrowel.nlp.StopwordAnnotator");
        List<String> stopWords = null;
        try {
            stopWords = WordReader.wordReaderWithResourcePath("/nlp/top1000words.txt").getWords();
        } catch (IOException e) {
            e.printStackTrace();
            return this;
        }
        String customStopWordList = Joiner.on(",").join(stopWords);
        props.setProperty(StopwordAnnotator.STOPWORDS_LIST, customStopWordList);
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(body);
        pipeline.annotate(document);
        List<CoreLabel> inputTokens = document.get(CoreAnnotations.TokensAnnotation.class);
        SnowballStemmer stemmer = new englishStemmer();
        for (CoreLabel token : inputTokens) {
            Pair<Boolean, Boolean> stopword = token.get(StopwordAnnotator.class);
            if (stopword.first())
                continue;
            String word = token.word().toLowerCase();

            //!!AI TODO this sucks, should make another annotator and make it optional etc.
            //also we're matching full stops! so we lose sentence information.
            if (punctuation.matcher(word).matches())
                continue;

            //!AI TODO again this would be its own annotator and optional
            word = number.matcher(word).replaceAll("NUMBER");

            stemmer.setCurrent(word);
            stemmer.stem();
            word = stemmer.getCurrent();
            tokens.add(word);
        }
        return this;
    }

    // Stanford NLP tokenizer, trained on Penn Tree Bank (PTB)
    // to use lemmatization need very large models in classpath
    // http://search.maven.org/remotecontent?filepath=edu/stanford/nlp/stanford-corenlp/3.3.1/stanford-corenlp-3.3.1-models.jar
    //
    // lemmatization requires massive models and a lot of space, not worth it.
    /*
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma, stopword");
    props.setProperty("customAnnotatorClass.stopword", "com.asimihsan.handytrowel.StopwordAnnotator");
    String customStopWordList = "start,starts,period,periods,a,an,and,are,as,at,be,but,by,for,he,had,if,in,into,is,it,no,not,of,on,or,such,that,the,their,then,there,these,they,this,to,was,will,with";
    props.setProperty(StopwordAnnotator.STOPWORDS_LIST, customStopWordList);
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    Annotation document = new Annotation(pageContents);
    pipeline.annotate(document);
    List<CoreLabel> inputTokens = document.get(CoreAnnotations.TokensAnnotation.class);

    for (CoreLabel token : inputTokens) {
        Pair<Boolean, Boolean> stopword = token.get(StopwordAnnotator.class);
        if (!stopword.first()) {
            String word = token.get(LemmaAnnotation.class).toLowerCase();
            outputTokens.add(word);
        }
    }
    */

}
