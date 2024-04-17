package edu.arizona.cs;

import java.io.File;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class BuildIndex {

    public BuildIndex(){
        
    }

    public void fileIndex(String inputFileDir) throws java.io.FileNotFoundException,java.io.IOException {
        // create the index file
        String indexFile = "index-file";
        File indexF = new File(indexFile);
        // using Lucene to index
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(indexF.toPath());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);

        // loop through all the wiki files under the directory
        File wikiDirF = new File(inputFileDir);
        for(String i : wikiDirF.list()) {
            String singleFilePath = inputFileDir + "/" + i;
            System.out.println(singleFilePath);
        }
        w.close();
        index.close();
    }
    
}
