package edu.arizona.cs;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class MainWatson {
	static boolean buildIndex = false;
	static boolean runQuery = false;

	// pre-index processing flags
	static boolean index_lemmatization = false; // lemmatization and stemming are mutually exclusive
	static boolean index_stemming = false;
	static boolean index_wiki = true;

	// query flags
	static boolean query_lemmatization = false; // lemmatization and stemming are mutually exclusive
	static boolean query_stemming = true;
	static boolean query_wiki = false;
	static boolean chatgpt = false; // if true, need to add you chatGPT API in QueryEngine.java

	static String wikiDir = "wiki-folder"; // input wiki pages
	static String queryFile = "questions.txt"; // input questions as query

	// different index files under the folder WastonQASystem for query
	// Update these paths to be dynamic based on flags
	static String indexFileStd = "index-file-std";
	static String indexFileLemma = "index-file-lemma";
	static String indexFileStem = "index-file-stem";
	static String indexFileWiki = "index-file-wiki";
	static String query_indexFile = "";

	// Similarity
	static Integer similarityStrategy = 6; // choose different similarity

	public static void main(String[] args) throws FileNotFoundException, IOException {

		if (buildIndex) {
			String indexFile = determineIndexFileName(index_lemmatization, index_stemming, index_wiki);
			BuildIndex myBuildIndex = new BuildIndex(index_lemmatization, index_stemming, index_wiki);
			myBuildIndex.fileIndex(wikiDir, indexFile);
		}
		if (runQuery) {
			String query_indexFile = determineIndexFileName(query_lemmatization, query_stemming, query_wiki);
			printQueryInfo(query_lemmatization, query_stemming, query_wiki);
			Similarity sim = getSimilarity(similarityStrategy);
			QueryEngine myQueryEngine = new QueryEngine(query_indexFile, sim, chatgpt);
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

	private static Similarity getSimilarity(int similarityStrategy) {
		if (similarityStrategy == 1) {
			return new ClassicSimilarity(); // P: 0.01 MMR: 0.02 hits: 6
		} else if (similarityStrategy == 2) {
			return new BooleanSimilarity(); // P: 0.18 MMR 0.23 hits: 37
		} else if (similarityStrategy == 3) {
			return new BM25Similarity();
		} else if (similarityStrategy == 4) {
			return new BM25Similarity(0.25f, 0.6f); // P: 0.37 MMR: 0.44 hits: 57
		} else if (similarityStrategy == 5) {
			return new LMJelinekMercerSimilarity((float) 0.5);
		} else if (similarityStrategy == 6) {
			return new LMJelinekMercerSimilarity((float) 0.05); // P: 0.38 MMR: 0.44 hits:57
		} else if (similarityStrategy == 7) {
			return new LMDirichletSimilarity(2000);
		} else if (similarityStrategy == 8) {
			return new LMDirichletSimilarity(3000); // P: 0.35 MMR 0.44 hits: 63
		} else {
			return new LMJelinekMercerSimilarity((float) 0.05);
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
