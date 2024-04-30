package edu.arizona.cs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.opennlp.OpenNLPLemmatizerFilter;
import org.apache.lucene.analysis.opennlp.tools.NLPLemmatizerOp;


public class LemmaAnalyzer extends Analyzer{
    @SuppressWarnings("resource")
    @Override
    protected TokenStreamComponents createComponents(String fieldName)  {

        StandardTokenizer src = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(src);
        result = new StopFilter(result,  EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        try {
            InputStream dict = new FileInputStream("en-lemmatizer.dict");
            NLPLemmatizerOp lemmaOp = new NLPLemmatizerOp(dict, null);
            result = new OpenNLPLemmatizerFilter(result, lemmaOp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return new TokenStreamComponents(src, result);
    }
}
