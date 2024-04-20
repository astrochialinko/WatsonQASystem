package edu.arizona.cs;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MainWatson {
    static boolean buildIndex = false;
	static boolean runQuery = true;
	
	// input files under the folder src/main/resources
	static String wikiDir = "wiki-folder"; // input wiki pages
	static String queryFile = "questions.txt"; // input questions as query
	
	// output files under the folder WastonQASystem
    static String indexFile = "index-file"; // saving the built index
    public static void main(String[] args ) throws FileNotFoundException, IOException {
		if(buildIndex) {
			BuildIndex myBuildIndex = new BuildIndex();
			myBuildIndex.fileIndex(wikiDir, indexFile);
		}
		if(runQuery) {
			QueryEngine myQueryEngine = new QueryEngine(indexFile);
            myQueryEngine.buildQuery(queryFile);
		}
	}
}
