package edu.arizona.cs;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MainWatson {
    static boolean buildIndex = true;
	static boolean runQuery = true;

	// pre-index processing flags
	static boolean lemmatization = false;   // lemmatization and stemming are mutually exclusive
	static boolean stemming = true;

	static String wikiDir = "wiki-folder"; // input wiki pages
	static String indexFile = "index-file";  // saving the built index
    public static void main(String[] args ) throws FileNotFoundException, IOException {
		if(buildIndex) {
			BuildIndex myBuildIndex = new BuildIndex(lemmatization, stemming);
			myBuildIndex.fileIndex(wikiDir, indexFile);
		}
		if(runQuery) {
			System.out.println("hello watson");
		}
	}
}
