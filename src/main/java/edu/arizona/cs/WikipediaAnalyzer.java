package edu.arizona.cs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

public class WikipediaAnalyzer extends Analyzer {
	@SuppressWarnings("resource")
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
		
        WikipediaTokenizer src = new WikipediaTokenizer();
        TokenStream result = new LowerCaseFilter(src);  // Apply LowerCaseFilter to convert all characters to lowercase
        result = new StopFilter(result, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);  // Apply StopFilter using the standard English stop words
        return new TokenStreamComponents(src, result);
    }
}
