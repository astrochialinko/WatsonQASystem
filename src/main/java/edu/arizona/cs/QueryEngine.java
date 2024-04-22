package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

public class QueryEngine {

	public QueryEngine(String indexDirectoryPath) {
	}

	public void buildQuery(String queryFile) {
		System.out.println("Start building querys from " + queryFile);

		// query Strategy
		Boolean isExtractKeywords = true;
		Boolean addCategory = true;

		try {
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource(queryFile).getFile());
			List<String> lines = Files.readAllLines(file.toPath());

			// Process each question block (4 lines per block)
			for (int i = 0; i < lines.size(); i += 4) {
				String category = lines.get(i).trim();
				String clue = lines.get(i + 1).trim();
				String answer = lines.get(i + 2).trim(); // Not used directly in query building

				List<String> queryKeywords = new ArrayList<String>();

				// Optionally use category to refine keywords or add context
				if (addCategory) {
					queryKeywords.add(category);
				}

				// Different Strategy for the query
				if (isExtractKeywords) {
					queryKeywords = extractKeywords(clue);

				} else { // original texts
					queryKeywords.add(clue);
				}

				// Debugging output to see processed query
				int q_index = i / 4 + 1;
				System.out.println("Query " + q_index + ": " + String.join(" ", queryKeywords));
			}

		} catch (IOException e) {
			System.err.println("Error reading file: " + e.getMessage());
		}

	}

	public static List<String> extractKeywords(String text) {
		List<String> result = new ArrayList<>();
		try (Analyzer analyzer = new EnglishAnalyzer()) {
			TokenStream stream = analyzer.tokenStream(null, text);
			CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			while (stream.incrementToken()) {
				result.add(attr.toString());
			}
			stream.end();
		} catch (IOException e) {
			System.err.println("Error processing text: " + e.getMessage());
		}
		return result;
	}

}
