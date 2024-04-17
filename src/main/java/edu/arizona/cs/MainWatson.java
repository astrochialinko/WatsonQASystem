package edu.arizona.cs;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MainWatson {
    static boolean buildIndex = true;
	static boolean runQuery = true;
	static String wikiDir = "src/main/resources/wiki-folder";
    public static void main(String[] args ) throws FileNotFoundException, IOException {
		if(buildIndex) {
			BuildIndex myBuildIndex = new BuildIndex();
			myBuildIndex.fileIndex(wikiDir);
		}
		if(runQuery) {
			System.out.println("hello watson");
		}
	}
}
