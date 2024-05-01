package edu.arizona.cs;

import java.io.IOException;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.junit.jupiter.api.Test;

public class TestWastonLemma {

    // query flags
	static boolean query_lemmatization = true; // lemmatization and stemming are mutually exclusive
	static boolean query_stemming = false;
	static boolean query_wiki = false;
	static boolean chatgpt = false;

    static String queryFile = "questions.txt"; // input questions as query

	// 3 different index files under the folder WastonQASystem for query
	// Update these paths to be dynamic based on flags
	static String indexFileStd = "index-file-std";
	static String indexFileLemma = "index-file-lemma";
	static String indexFileStem = "index-file-stem";
	static String indexFileWiki = "index-file-wiki";

    @Test
    public void runTest() throws IOException {
        String query_indexFile = determineIndexFileName(query_lemmatization, query_stemming, query_wiki);
        printQueryInfo(query_lemmatization, query_stemming, query_wiki);

        System.out.println("Similarity Classic:");
        QueryEngine myQueryEngine1 = new QueryEngine(query_indexFile, new ClassicSimilarity(), chatgpt);
		myQueryEngine1.processQueries(queryFile);

        System.out.println("Similarity Boolean:");
        QueryEngine myQueryEngine2 = new QueryEngine(query_indexFile, new BooleanSimilarity(), chatgpt);
		myQueryEngine2.processQueries(queryFile);

        System.out.println("Similarity BM25(k:0.25, b:0.6):");
        QueryEngine myQueryEngine3 = new QueryEngine(query_indexFile, new BM25Similarity(0.25f, 0.6f), chatgpt);
		myQueryEngine3.processQueries(queryFile);

        System.out.println("Similarity LMJelinekMercer(0.05):");
        QueryEngine myQueryEngine4 = new QueryEngine(query_indexFile, new LMJelinekMercerSimilarity((float) 0.05), chatgpt);
		myQueryEngine4.processQueries(queryFile);

        System.out.println("Similarity LMDirichlet(3000):");
        QueryEngine myQueryEngine5 = new QueryEngine(query_indexFile, new LMDirichletSimilarity(3000), chatgpt);
		myQueryEngine5.processQueries(queryFile);
    }

    private static String determineIndexFileName(boolean lemmatization, boolean stemming, boolean index_wiki) {
		if (stemming && !lemmatization && !index_wiki) {
			return indexFileStem;
		} else if (!stemming && lemmatization && !index_wiki) {
			return indexFileLemma;
		} else if (index_wiki) {
			return indexFileWiki;
		} else {
			return indexFileStd; // Default or both flags on/off
		}
	}

	private static void printQueryInfo(boolean query_lemmatization, boolean query_stemming, boolean query_wiki) {
		if (query_stemming && !query_lemmatization && !query_wiki) {
			System.out.println("Querying with stemming");
		} else if (!query_stemming && query_lemmatization && !query_wiki) {
			System.out.println("Querying with Lemma");
		} else if (!query_stemming && !query_lemmatization && !query_wiki) {
			System.out.println("Querying with no Lemma or stemming");
		} else if (query_wiki) {
			System.out.println("Querying with Wiki");
		} else {
			System.out
					.println("both stemming and lemma flags are on. Invalid combo. Querying with no Lemma or stemming");
		}
	}
}
