package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.similarities.Similarity;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QueryEngine {
	private IndexReader reader;
	private IndexSearcher searcher;
	private QueryParser parser;
	private Analyzer analyzer = null;
	private List<String> questions = new ArrayList<>();
	private List<String> answers = new ArrayList<>();
	
	// parameters
	private int hitsPerPage = 10;
	private boolean add_category = true;

	// print the progress for 1st doc and each QAs
	private boolean print_QA = false;
	private boolean print_doc = false;

	// rerank with chatGPT
	private boolean chatgpt_rerank = false;
	private boolean print_GPT = false; // for debug
	private int chatgpt_sleep_sec = 0; // set sleep time if reach the gpt API limit
	private String apiKey = "apikey"; // input you chatGPT API here

	// Constructor initializes the searcher and parser
	public QueryEngine(String indexDirectoryPath, Similarity sim, boolean rerank) throws IOException {

		if (indexDirectoryPath.endsWith("std")) {
			analyzer = new StandardAnalyzer();
		} else if (indexDirectoryPath.endsWith("lemma")) {
			analyzer = new LemmaAnalyzer();
		} else if (indexDirectoryPath.endsWith("stem")) {
			analyzer = new EnglishAnalyzer();
		} else if (indexDirectoryPath.endsWith("wiki")) {
			analyzer = new WikipediaAnalyzer();
		}
		loadDocIndex(indexDirectoryPath);
		this.parser = new QueryParser("text", this.analyzer);
		this.searcher = new IndexSearcher(this.reader);
		this.searcher.setSimilarity(sim);
		this.chatgpt_rerank = rerank;
	}

	// 1. load Document Index
	private void loadDocIndex(String indexDirectoryPath) throws IOException {
		FSDirectory dir = FSDirectory.open(Paths.get(indexDirectoryPath));
		this.reader = DirectoryReader.open(dir);

		if (print_doc) {
			System.out.println("Read " + reader.numDocs() + " wiki docs index from " + indexDirectoryPath + "...\n");
			printDoc(this.reader.document(0));
		}

	}
	
	// 2. Processes queries from file and displays results
	public void processQueries(String queryFile) throws IOException {
		List<Query> queries = buildQuery(queryFile);
		executeQueries(queries, hitsPerPage);
		reader.close();
	}

	// -----------------------------------------------------------------
	// Helper functions
	
	// for debug purpose
	private void printDoc(Document doc) {
		System.out.println("Document Example...");
		System.out.println("Title: " + doc.get("title"));
		System.out.println("Categories: " + doc.get("categories"));
		System.out.println("Summary: " + doc.get("summary"));
		System.out.println("Text: " + doc.get("text") + "\n");
	}

	// Builds Lucene queries from a file
	private List<Query> buildQuery(String queryFile) throws IOException {
		List<Query> queries = new ArrayList<>();
		File file = new File(getClass().getClassLoader().getResource(queryFile).getFile());
		List<String> lines = Files.readAllLines(file.toPath());

		for (int i = 0; i < lines.size(); i += 4) {
			String category = lines.get(i).trim().replaceAll("\\s*\\(.*?\\)\\s*", "");
			String clue = lines.get(i + 1).trim();
			String answer = lines.get(i + 2).trim();
			questions.add(clue);

			try {
				// Builds a Lucene query string from clue and category
				Query query;
				if (add_category) {

					int queryStrategy = 1; // best model
					String queryStr;

					if (queryStrategy == 1) {
						queryStr = String.format("categories:%s OR text:%s ", QueryParser.escape(category),
								QueryParser.escape(clue));
					} else if (queryStrategy == 2) {
						queryStr = String.format("summary:%s ", QueryParser.escape(clue));
					} else if (queryStrategy == 3) {
						queryStr = String.format("categories:%s summary:%s ", QueryParser.escape(category),
								QueryParser.escape(clue));
					} else if (queryStrategy == 3) {
						queryStr = String.format("categories:%s text:\"%s\" text:%s", QueryParser.escape(category),
								QueryParser.escape(category), QueryParser.escape(clue));
					} else {
						queryStr = String.format("categories:%s OR text:%s ", QueryParser.escape(category),
								QueryParser.escape(clue));
					}
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
	private void executeQueries(List<Query> queries, int hitsPerPage) throws IOException {
		List<List<String>> results = new ArrayList<>();
		List<Integer> wrongQA = new ArrayList<>();
		List<Integer> wrongGPT = new ArrayList<>();
		List<Integer> rightGPT = new ArrayList<>();
	
		int correctCount = 0;
		int correctCountInHit = 0;
		int correctCountRerank = 0;
		int wrongCountRerank = 0;
		double mmr = 0;

		for (int i = 0; i < questions.size(); i++) {
			String question = questions.get(i);
			String answer = answers.get(i);
			Query query = queries.get(i);

			if (print_QA) {
				System.out.println("Executing query " + (i + 1) + ": " + query.toString());
				System.out.println("Answer: " + answer);
			}

			List<String> initialResults = executeSingleQuery(query, hitsPerPage);
			List<String> queryResult;

			if (chatgpt_rerank) {
				List<String> rerankedResults = rerankWithChatGPT(initialResults, question);
				queryResult = rerankedResults;
				if (print_QA) {
					System.out.println("-> ChatGPT: " + rerankedResults.get(0));
				}

				// e.g., wait 20 sec, since gpt-3.5-turbo has limit of 3 RPM : (
				if (chatgpt_sleep_sec > 0) {
					try {
						TimeUnit.SECONDS.sleep(chatgpt_sleep_sec);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			} else {
				queryResult = initialResults;
			}

			results.add(queryResult);

			// calculating precision and MMR
			String[] possibleAnswers = answer.split("\\|"); // some questions have two accepted answers
			boolean isAnswerCorrect = false;
			boolean isInitAnswerWrong = chatgpt_rerank;

			// Check if any of the possible hits the first document
			for (String possibleAnswer : possibleAnswers) {
				if (!queryResult.isEmpty() && queryResult.get(0).equalsIgnoreCase(possibleAnswer.trim())) {
					isAnswerCorrect = true;
				}

				if (chatgpt_rerank && !initialResults.isEmpty()
						&& initialResults.get(0).equalsIgnoreCase(possibleAnswer.trim())) {
					isInitAnswerWrong = false;
				}

				if (isAnswerCorrect && (!chatgpt_rerank || !isInitAnswerWrong)) {
					break;
				}
			}

			if (isAnswerCorrect) {
				if (print_QA) {
					System.out.println("--- Search Correct --- ");
				}
				correctCount++;
				mmr += 1.0;
				correctCountInHit++;

				if (isInitAnswerWrong) {
					correctCountRerank++;
					rightGPT.add(i);
					if (print_QA) {
						System.out.println("--- Search Correct by Rerank --- ");
					}
				}

			} else {
				if (print_QA) {
					System.out.println("--- Search Incorrect --- ");
				}
				wrongQA.add(i);
				int right_index = 0;
				for (String result : queryResult) {
					right_index++;
					for (String possibleAnswer : possibleAnswers) {
						if (result.equalsIgnoreCase(possibleAnswer.trim())) {
							if (print_QA)
								System.out.printf("--- Search Correct in QA %d --- \n", right_index);
							mmr += (double) 1 / right_index;
							correctCountInHit++;
							isAnswerCorrect = true;
							break;
						}
					}
				}

				if (!isInitAnswerWrong) {
					wrongCountRerank++;
					wrongGPT.add(i);
					if (print_QA) {
						System.out.printf("--- Original Search Correct --- \n", right_index);
					}
				}
			}
			if (print_QA) {
				System.out.println("");
			}
		}

		calculateMetrics(queries.size(), correctCount, mmr, hitsPerPage, correctCountInHit, correctCountRerank,
				wrongCountRerank);

		if (print_QA) {
			System.out.println("WrongQAs = " + wrongQA);
		}
		if (print_QA && chatgpt_rerank) {
			System.out.println("WrongGPTQAs = " + wrongGPT);
		}
		if (print_QA && chatgpt_rerank) {
			System.out.println("RightGPTQAs = " + rightGPT);
		}
			

	}

	private List<String> executeSingleQuery(Query query, int hitsPerPage) throws IOException {
		List<String> queryResult = new ArrayList<>();
		TopDocs docs = searcher.search(query, hitsPerPage);

		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			String title = doc.get("title");
			double score = scoreDoc.score;
			queryResult.add(title);
			if (print_QA) {
				System.out.println("-> QA: " + title + "\t (Score: " + score + ")");
			}
		}
		return queryResult;
	}

	public List<String> rerankWithChatGPT(List<String> initialResults, String question) throws IOException {
		List<String> rerankedResults = new ArrayList<>();
		OkHttpClient client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build();

		MediaType JSON = MediaType.get("application/json; charset=utf-8");
		StringBuilder options = new StringBuilder();
		boolean isgpt35 = true;

		for (int i = 0; i < initialResults.size(); i++) {
			options.append(initialResults.get(i) + "\n");
		}

		Request request;

		if (isgpt35) {

			// Prepare the messages as a JsonArray
			JsonArray messages = new JsonArray();
			JsonObject message = new JsonObject();
			message.addProperty("role", "user");
			message.addProperty("content", "Given the clue and the options, "
					+ "please list the options in order of relevance to the clue from most relevant to least relevant. "
					+ "List the options as plain text, each on a new line, "
					+ "without numbers or any additional formatting.\n\nClue: " + question + "\n\nOptions: \n" + options
					+ "\nRerank Options: \n");
			messages.add(message);

			// Create the JSON object for the Chat API
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("messages", messages);
			jsonObject.addProperty("max_tokens", 50);
			jsonObject.addProperty("model", "gpt-3.5-turbo");
			jsonObject.addProperty("temperature", 0.5);
			String json = jsonObject.toString();
			if (print_GPT) {
				System.out.println("JSON : " + messages); // for debug
			}		

			// Send the request to OpenAI's API
			RequestBody body = RequestBody.create(json, JSON);
			request = new Request.Builder().url("https://api.openai.com/v1/chat/completions")
					.addHeader("Authorization", "Bearer " + apiKey).addHeader("Content-Type", "application/json")
					.post(body).build();

		} else {
			// Prepare the data for embedding
			// Preparing the JSON object with 'input' field
			JsonObject inputObject = new JsonObject();
			inputObject.addProperty("input", question); // Adding the question as the input for embedding

			JsonArray documents = new JsonArray();
			for (String result : initialResults) {
				JsonObject doc = new JsonObject();
				doc.addProperty("input", result); // Each document needs an 'input' field
				documents.add(doc);
			}

			JsonObject jsonObject = new JsonObject();
			jsonObject.add("input", inputObject); // Question for context embedding
			jsonObject.add("documents", documents); // Documents to embed
			jsonObject.addProperty("model", "text-embedding-ada-002");

			String json = jsonObject.toString();
			System.out.println("json: " + json);
			RequestBody body = RequestBody.create(json, JSON);
			request = new Request.Builder().url("https://api.openai.com/v1/embeddings")
					.addHeader("Authorization", "Bearer " + apiKey).addHeader("Content-Type", "application/json")
					.post(body).build();
		}

		// Handle the response from the API
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				String errorBody = response.body().string();
				System.out.println("Error response: " + errorBody);
				throw new IOException("Unexpected code " + response);
			}

			// Parse the JSON response
			String responseBody = response.body().string();
			if (print_GPT) {
				System.out.println(responseBody); // for debug
			}
			JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
			JsonObject choice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
			String content = choice.getAsJsonObject("message").get("content").getAsString();

			// Convert the content string into a list
			rerankedResults = Arrays.asList(content.split("\\n"));
		}
		return rerankedResults;
	}

	private void calculateMetrics(int totalQueries, int correctCount, double mmr, int hitsPerPage,
			int correctCountInHit, int correctCountRerank, int wrongCountRerank) {
		double precision = (double) correctCount / totalQueries;
		double MMR = mmr / totalQueries;

		System.out.println("\n-----------------------------------------------------");
		System.out.println(" Measuring performance...");
		System.out.printf(" - Correctly answered questions: %d\n", correctCount);
		System.out.printf(" - Incorrectly answered: %d\n\n", totalQueries - correctCount);
		System.out.printf(" - Correctly answered questions within %d hits: %d\n", hitsPerPage, correctCountInHit);
		System.out.printf(" - Correctly answered questions by ChatGPT Rerank if applied: %d\n", correctCountRerank);
		System.out.printf(" - Incorrectly answered questions by ChatGPT Rerank: %d\n", wrongCountRerank);
		System.out.printf(" - Total questions: %d\n\n", totalQueries);
		System.out.printf(" - Precision at 1 (P@1): %.2f\n", precision);
		System.out.printf(" - Mean Reciprocal Rank (MRR): %.2f\n", MMR);
		System.out.println("-----------------------------------------------------");
	}

}
