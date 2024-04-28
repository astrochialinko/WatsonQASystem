package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.StringReader;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

public class QueryEngine {
	private IndexReader reader;
	private IndexSearcher searcher;
	private QueryParser parser;
	private Analyzer analyzer = null;
	private List<String> answers = new ArrayList<>();
	private int hitsPerPage = 10;
	private boolean query_lemma = false;
	private boolean query_stem = false;

	// Constructor initializes the searcher and parser
	public QueryEngine(String indexDirectoryPath) throws IOException {

		if (indexDirectoryPath.endsWith("std")) {
			analyzer = new StandardAnalyzer();
		} else if (indexDirectoryPath.endsWith("lemma")) {
			analyzer = new LemmaAnalyzer();
			query_lemma = true;
		} else if (indexDirectoryPath.endsWith("stem")) {
			analyzer = new EnglishAnalyzer();
			query_stem = true;
		}
		loadDocIndex(indexDirectoryPath);
		this.parser = new QueryParser("text", this.analyzer);
		this.searcher = new IndexSearcher(this.reader);
//		searcher.setSimilarity(new ClassicSimilarity()); // P: 0.01 MMR: 0.02 hits: 5
//		 searcher.setSimilarity(new BooleanSimilarity()); // P: 0.15 MMR 0.20 hits: 34
		searcher.setSimilarity(new BM25Similarity(0.25f, 0.6f)); // P: 0.30 MMR: 0.38 hits: 58
//		 searcher.setSimilarity(new LMJelinekMercerSimilarity((float) 0.1)); // P: 0.26 MMR: 0.35 hits:56
//		 searcher.setSimilarity(new LMDirichletSimilarity()); // P: 0.28 MMR 0.39 hits: 60

	}

	// 1. load Document Index
	private void loadDocIndex(String indexDirectoryPath) throws IOException {
		FSDirectory dir = FSDirectory.open(Paths.get(indexDirectoryPath));
		this.reader = DirectoryReader.open(dir);

		System.out.println("Read " + reader.numDocs() + " wiki docs index from " + indexDirectoryPath + "...\n");
		printDoc(this.reader.document(0));
	}

	// debug purpose
	private void printDoc(Document doc) {
		System.out.println("Document Example...");
		System.out.println("Title: " + doc.get("title"));
		System.out.println("Categories: " + doc.get("categories"));
		System.out.println("Text: " + doc.get("text") + "\n");
	}

	// 2. and 3. Processes queries from file and displays results
	public void processQueries(String queryFile) throws IOException {
		List<Query> queries = buildQuery(queryFile);
		List<ResultClass> results = executeQueries(queries, hitsPerPage);
//		displayResults(queries, results);
		reader.close();
	}

	// Builds Lucene queries from a file
	private List<Query> buildQuery(String queryFile) throws IOException {
		List<Query> queries = new ArrayList<>();
		File file = new File(getClass().getClassLoader().getResource(queryFile).getFile());
		List<String> lines = Files.readAllLines(file.toPath());

		for (int i = 0; i < lines.size(); i += 4) {
			String category = lines.get(i).trim();
			String clue = lines.get(i + 1).trim();
			String answer = lines.get(i + 2).trim();
//			String queryStr = buildLuceneQuery(clue, category);

			int q_index = i / 4 + 1;
			try {
				Query query = parser.parse(QueryParser.escape(clue));
//				Query query = parser.parse(queryStr);
				queries.add(query);
				this.answers.add(answer);
			} catch (ParseException e) {
				System.err.println("Error parsing query: " + e.getMessage());
			}
		}
		return queries;
	}

	// Executes the built queries and returns results
	private List<ResultClass> executeQueries(List<Query> queries, int hitsPerPage) throws IOException {
		List<ResultClass> results = new ArrayList<>();

		int i = 0;
		int corret_count = 0;
		int correctCountInHit = 0;
		double mmr = 0;
		for (Query query : queries) {
			String answer = answers.get(i);
			System.out.println("Executing query " + (i + 1) + ": " + query.toString());
			System.out.println("Answer: " + answer);

			TopDocs docs = searcher.search(query, hitsPerPage);
			int j = 1;
			List<ResultClass> query_result = new ArrayList<>();
			for (ScoreDoc scoreDoc : docs.scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc);
				String title = doc.get("title");
				double score = scoreDoc.score;
				// results.add(new ResultClass(doc, score)); // here you adding the 10 returned
				// hits for every query into one single list, i dont think we need this.
				query_result.add(new ResultClass(doc, score));
				System.out.println("-> QA " + j + ": " + title + "\t (Score: " + score + ")");
				j++;
			}

			// calculating precision and MMR
			String[] possibleAnswers = answer.split("\\|"); // some questions have two accepted answers
			boolean isAnswerCorrect = false;

			// Check if any of the possible hits the first document
			for (String possibleAnswer : possibleAnswers) {
				if (query_result.get(0).DocName.get("title").equalsIgnoreCase(possibleAnswer.trim())) {
					isAnswerCorrect = true;
					break;
				}
			}

			if (isAnswerCorrect) {
				System.out.println("--- Search Correct --- ");
				corret_count++;
				mmr += 1.0;
				correctCountInHit++;
			} else {
				System.out.println("--- Search Incorrect --- ");
				int right_index = 0;
				for (ResultClass result : query_result) {
					right_index++;
					for (String possibleAnswer : possibleAnswers) {
						if (result.DocName.get("title").equalsIgnoreCase(possibleAnswer.trim())) {
							System.out.printf("--- Search Correct in QA %d --- \n", right_index);
							mmr += (double) 1 / right_index;
							correctCountInHit++;
							isAnswerCorrect = true;
							break;
						}
					}
					if (isAnswerCorrect) {
						
						break;
					}
				}
			}

			System.out.println("");
			i++;
		}
		double precision = (double) corret_count / i;
		double precisionInHit = (double) correctCountInHit / i;
		double MMR = (double) mmr / i;

		System.out.println("-----------------------------------------------------");
		System.out.println(" Measuring performance...");
		System.out.printf(" - Correctly answered questions: %d\n", corret_count);
		System.out.printf(" - Incorrectly answered: %d\n", i - corret_count);
		System.out.printf(" - Correctly answered questions within %d hits: %d\n", hitsPerPage, correctCountInHit);
		System.out.printf(" - Total questions questions: %d\n\n", i);
		System.out.printf(" - Precision at 1 (P@1): %.2f\n", precision);
		System.out.printf(" - Mean Reciprocal Rank (MRR): %.2f\n", MMR);
		System.out.println("-----------------------------------------------------");

		return results;
	}

	// Displays the results of executed queries
	// buggy, not yet finish
	private void displayResults(List<Query> queries, List<ResultClass> results) {
		int i = 0;
		for (Query query : queries) {
			System.out.println("Executing query " + (i + 1) + ": " + query.toString());
			System.out.println("Answer: " + answers.get(i));
			i++;

			for (int j = 0; j < hitsPerPage; j++) {
				String title = results.get(i * j).getTitle();
				double score = results.get(i * j).getScore();
				System.out.println("-> QA " + (j + 1) + ": " + title + "\t (Score: " + score + ")");
			}

			System.out.println("");
		}
	}

	// Builds a Lucene query string from clue and category
	// buggy, not yet finish
	private String buildLuceneQuery(String clue, String category) {
		return String.format("text:%s AND category:%s", QueryParser.escape(clue), QueryParser.escape(category));
	}

}
