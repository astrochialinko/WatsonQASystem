package edu.arizona.cs;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.lucene.index.IndexWriterConfig;

public class MainWatson {
    static boolean buildIndex = false;
	static boolean runQuery = true;

	// pre-index processing flags
	static boolean index_lemmatization = false;   // lemmatization and stemming are mutually exclusive
	static boolean index_stemming = true;

	// query flags
	static boolean query_lemmatization = false;   // lemmatization and stemming are mutually exclusive
	static boolean query_stemming = true;

	static String wikiDir = "wiki-folder"; // input wiki pages
	static String queryFile = "questions.txt"; // input questions as query
	
	// output files under the folder WastonQASystem
    static String indexFile = "index-file"; // saving the built index

	// 3 different index files for query
	static String indexFileStd = "index-file-std";
	static String indexFileLemma = "index-file-lemma";
	static String indexFileStem = "index-file-stem";
	static String query_indexFile= "";

    public static void main(String[] args ) throws FileNotFoundException, IOException {
		if(buildIndex) {
			BuildIndex myBuildIndex = new BuildIndex(index_lemmatization, index_stemming);
			myBuildIndex.fileIndex(wikiDir, indexFile);
		}
		if(runQuery) {
			if (query_stemming && !query_lemmatization) {
				query_indexFile = indexFileStem;
            	System.out.println("Querying with stemming");
        	}
        	else if (!query_stemming && query_lemmatization) {
        	    query_indexFile = indexFileLemma;
        	    System.out.println("Querying with Lemma");
        	}
        	else if (!query_stemming && !query_lemmatization) {
        	    query_indexFile = indexFileStd;
        	    System.out.println("Querying with no Lemma or stemming");
        	}
        	else {
        	    query_indexFile = indexFileStd;
        	    System.out.println("both stemming and lemma flags are on. Invalid combo. Querying with no Lemma or stemming");
        	}
			QueryEngine myQueryEngine = new QueryEngine(query_indexFile);
            myQueryEngine.processQueries(queryFile);
		}
	}
}
