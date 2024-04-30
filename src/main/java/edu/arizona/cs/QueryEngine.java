package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
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
import org.apache.lucene.search.similarities.Similarity;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;
import com.google.gson.JsonObject;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.util.Collections;
import java.util.Comparator;

//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;

public class QueryEngine {
	private IndexReader reader;
	private IndexSearcher searcher;
	private QueryParser parser;
	private Analyzer analyzer = null;
	private List<String> answers = new ArrayList<>();
	private int hitsPerPage = 10;
	private String apiKey = "apikey";

	private boolean query_lemma = false;
	private boolean query_stem = false;
	private boolean add_category = true;
	private boolean chatgpt_rerank = false;

//	private OkHttpClient client = new OkHttpClient();
//	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	// Constructor initializes the searcher and parser
	public QueryEngine(String indexDirectoryPath, Similarity sim, boolean rerank) throws IOException {

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
//		searcher.setSimilarity(new ClassicSimilarity()); // P: 0.01 MMR: 0.02 hits: 6
//		 searcher.setSimilarity(new BooleanSimilarity()); // P: 0.18 MMR 0.23 hits: 37
//		searcher.setSimilarity(new BM25Similarity(0.25f, 0.6f)); // P: 0.37 MMR: 0.44 hits: 57
		searcher.setSimilarity(sim); // P: 0.38 MMR: 0.44 hits:57
//		 searcher.setSimilarity(new LMDirichletSimilarity(3000)); // P: 0.35 MMR 0.44 hits: 63
		this.chatgpt_rerank = rerank;

	}

	// 1. load Document Index
	private void loadDocIndex(String indexDirectoryPath) throws IOException {
		FSDirectory dir = FSDirectory.open(Paths.get(indexDirectoryPath));
		this.reader = DirectoryReader.open(dir);

		//System.out.println("Read " + reader.numDocs() + " wiki docs index from " + indexDirectoryPath + "...\n");
		//printDoc(this.reader.document(0));
	}

	// debug purpose
	private void printDoc(Document doc) {
		System.out.println("Document Example...");
		System.out.println("Title: " + doc.get("title"));
		System.out.println("Categories: " + doc.get("categories"));
		System.out.println("Summary: " + doc.get("summary"));
		System.out.println("Text: " + doc.get("text") + "\n");
	}

	// 2. and 3. Processes queries from file and displays results
	public void processQueries(String queryFile) throws IOException {
		List<Query> queries = buildQuery(queryFile);
		List<List<ResultClass>> results = executeQueries(queries, hitsPerPage);
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

			try {
				Query query;
				if (add_category) {
					// Builds a Lucene query string from clue and category
					String queryStr = String.format("categories:%s OR text:%s ", QueryParser.escape(category),
							QueryParser.escape(clue));

//					String queryStr = String.format("categories:%s summary:%s ", 
//					QueryParser.escape(category), QueryParser.escape(clue));

//					String queryStr = String.format("categories:%s text:\"%s\" text:%s ", 
//							QueryParser.escape(category), QueryParser.escape(category), 
//							QueryParser.escape(clue));

					query = parser.parse(queryStr);
				} else {
					query = parser.parse(QueryParser.escape(clue));
				}
				queries.add(query);
				this.answers.add(answer);
			} catch (ParseException e) {
				System.err.println("Error parsing query: " + e.getMessage());
			}
		}
		return queries;
	}

	// Executes the built queries and returns results
	private List<List<ResultClass>> executeQueries(List<Query> queries, int hitsPerPage) throws IOException {
		List<List<ResultClass>> results = new ArrayList<>();
		int correctCount = 0;
		int correctCountInHit = 0;
		double mmr = 0;

		for (int i = 0; i < queries.size(); i++) {
			String answer = answers.get(i);
			Query query = queries.get(i);
			//System.out.println("Executing query " + (i + 1) + ": " + query.toString());
			//System.out.println("Answer: " + answer);

			List<ResultClass> initialResults = executeSingleQuery(queries.get(i), hitsPerPage);
			List<ResultClass> queryResult;

			if (chatgpt_rerank) {
				List<ResultClass> rerankedResults = rerankWithChatGPT(initialResults, query.toString());
				queryResult = rerankedResults;
			} else {
				queryResult = initialResults;
			}

			results.add(queryResult);

			// calculating precision and MMR
			String[] possibleAnswers = answer.split("\\|"); // some questions have two accepted answers
			boolean isAnswerCorrect = false;

			// Check if any of the possible hits the first document
			for (String possibleAnswer : possibleAnswers) {
				if (!queryResult.isEmpty()
						&& queryResult.get(0).DocName.get("title").equalsIgnoreCase(possibleAnswer.trim())) {
					isAnswerCorrect = true;
					break;
				}
			}

			if (isAnswerCorrect) {
				// System.out.println("--- Search Correct --- ");
				correctCount++;
				mmr += 1.0;
				correctCountInHit++;
			} else {
				// System.out.println("--- Search Incorrect --- ");
				int right_index = 0;
				for (ResultClass result : queryResult) {
					right_index++;
					for (String possibleAnswer : possibleAnswers) {
						if (result.DocName.get("title").equalsIgnoreCase(possibleAnswer.trim())) {
							// System.out.printf("--- Search Correct in QA %d --- \n", right_index);
							mmr += (double) 1 / right_index;
							correctCountInHit++;
							isAnswerCorrect = true;
							break;
						}
					}
					if (isAnswerCorrect) { // isAnswerCorrect can never be true here, because this is in the else path of if (isAnswerCorrect) checking
						break;
					}
				}
			}

			//System.out.println("");
		}

		calculateMetrics(queries.size(), correctCount, mmr, hitsPerPage, correctCountInHit);
		return results;
	}

	private List<ResultClass> executeSingleQuery(Query query, int hitsPerPage) throws IOException {
		List<ResultClass> queryResult = new ArrayList<>();
		TopDocs docs = searcher.search(query, hitsPerPage);

		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			String title = doc.get("title");
			double score = scoreDoc.score;
			queryResult.add(new ResultClass(doc, score));
			//System.out.println("-> QA: " + title + "\t (Score: " + score + ")");
		}
		return queryResult;
	}

	private void calculateMetrics(int totalQueries, int correctCount, double mmr, int hitsPerPage,
			int correctCountInHit) {
		double precision = (double) correctCount / totalQueries;
		double MMR = mmr / totalQueries;

		System.out.println("-----------------------------------------------------");
		System.out.println(" Measuring performance...");
		System.out.printf(" - Correctly answered questions: %d\n", correctCount);
		System.out.printf(" - Incorrectly answered: %d\n", totalQueries - correctCount);
		System.out.printf(" - Correctly answered questions within %d hits: %d\n", hitsPerPage, correctCountInHit);
		System.out.printf(" - Total questions: %d\n\n", totalQueries);
		System.out.printf(" - Precision at 1 (P@1): %.2f\n", precision);
		System.out.printf(" - Mean Reciprocal Rank (MRR): %.2f\n", MMR);
		System.out.println("-----------------------------------------------------");
	}

	public List<ResultClass> rerankWithChatGPT(List<ResultClass> initialResults, String query) throws IOException {
		List<ResultClass> rerankedResults = new ArrayList<>(initialResults);
		OkHttpClient client = new OkHttpClient();
		MediaType JSON = MediaType.get("application/json; charset=utf-8");

		for (ResultClass result : rerankedResults) {
			String documentText = result.DocName.get("summary"); // Use summary or text
			String prompt = String.format(
					"{\"prompt\": " + "\"How relevant is this text to the query: %s? " + "Text: %s\"}", query,
					documentText);

			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("prompt", "Query: " + query + "\nDocument: " + documentText);
			jsonObject.addProperty("max_tokens", 50);
			jsonObject.addProperty("model", "gpt-3.5-turbo");
			jsonObject.addProperty("temperature", 0.5);

			String json = jsonObject.toString();
			System.out.println("JSON Payload: " + prompt); // for debug
			RequestBody body = RequestBody.create(json, JSON);

			Request request = new Request.Builder().url("https://api.openai.com/v1/chat/completions")
					.addHeader("Authorization", "Bearer " + apiKey).addHeader("Content-Type", "application/json")
					.post(body).build();

			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					String errorBody = response.body().string();
					System.out.println("Error response: " + errorBody);
					throw new IOException("Unexpected code " + response);
				}
				String responseBody = response.body().string();
				double relevanceScore = extractRelevanceScore(responseBody); // Implement this method based on response
				result.setScore(relevanceScore);
			}
		}

		// Sort by the new score in descending order
		Collections.sort(rerankedResults, Comparator.comparingDouble(ResultClass::getScore).reversed());
		return rerankedResults;
	}

	// public List<ResultClass> rerankWithChatGPT_JF(List<ResultClass> initialResults, String query) throws IOException {
	// 	List<ResultClass> rerankedResults = new ArrayList<>(initialResults);
	// 	
	// 	long apiTimeout = 300;
	// 	OpenAiService openAiService = new OpenAiService(apiKey, Duration.ofSeconds(apiTimeout));
	// 	String SYSTEM_TASK_MESSAGE = "You are an answer evaluator of a trivia question. The trivia question is provided in the format of ";
	// 	
	// 	ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
    // 		.builder()
    // 		.model("gpt-3.5-turbo")
    // 		.temperature(0.8)
    // 		.messages(
    // 		    List.of(
    // 		        new ChatMessage("system", SYSTEM_TASK_MESSAGE),
    // 		        new ChatMessage("user", String.format("I want to visit %s and have a budget of %d dollars", city, budget))))
 	// 		.build();
	// 	return rerankedResults;
	// }

	// Dummy implementation: extract relevance score from API response
	private double extractRelevanceScore(String responseBody) {
		// You need to parse the JSON response and extract the score
		// This is a placeholder implementation
		return Math.random();
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

}
