package edu.arizona.cs;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

public class MainWatson {
	static boolean buildIndex = false;
	static boolean runQuery = true;

	// pre-index processing flags
	static boolean index_lemmatization = false; // lemmatization and stemming are mutually exclusive
	static boolean index_stemming = false;
	static boolean index_wiki = true;

	// query flags
	static boolean query_lemmatization = false; // lemmatization and stemming are mutually exclusive
	static boolean query_stemming = false;
	static boolean query_wiki = true;
	static boolean chatgpt = true;

	static String wikiDir = "wiki-folder"; // input wiki pages
	static String queryFile = "questions.txt"; // input questions as query

	// 3 different index files under the folder WastonQASystem for query
	// Update these paths to be dynamic based on flags
	static String indexFileStd = "index-file-std";
	static String indexFileLemma = "index-file-lemma";
	static String indexFileStem = "index-file-stem";
	static String indexFileWiki = "index-file-wiki";
	static String query_indexFile = "";

	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (buildIndex) {
			String indexFile = determineIndexFileName(index_lemmatization, index_stemming, index_wiki);
			BuildIndex myBuildIndex = new BuildIndex(index_lemmatization, index_stemming, index_wiki);
			myBuildIndex.fileIndex(wikiDir, indexFile);
		}
		if (runQuery) {
			String query_indexFile = determineIndexFileName(query_lemmatization, query_stemming, query_wiki);
			printQueryInfo(query_lemmatization, query_stemming, query_wiki);
			QueryEngine myQueryEngine = new QueryEngine(query_indexFile, new BM25Similarity(0.25f, 0.6f), chatgpt);
			myQueryEngine.processQueries(queryFile);
		}
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
