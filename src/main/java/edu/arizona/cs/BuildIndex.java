package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class BuildIndex {

    public BuildIndex(){
        
    }

    public void fileIndex(String inputFileDir, String indexFile) throws java.io.FileNotFoundException,java.io.IOException {
        // create the index file 
        ClassLoader classLoader = getClass().getClassLoader();
        File indexF = new File(indexFile);
        // using Lucene to index
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(indexF.toPath());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);

        // get wiki dir FileD
        File wikiDirF = new File(classLoader.getResource(inputFileDir).getFile());

        // loop through all the wiki files under the directory
        for(String i : wikiDirF.list()) {
            String singleFilePath = inputFileDir + "/" + i;
            System.out.println(i);
            //Get file from resources folder
           
            File file = new File(classLoader.getResource(singleFilePath).getFile());

            // scan through each line in a document
            try (Scanner inputScanner = new Scanner(file)) {
                while (inputScanner.hasNextLine()) {
                    String currLine = inputScanner.nextLine();
                    System.out.println(currLine);


                }
                inputScanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }



            
        }
        w.close();
        index.close();
    }
    
}
