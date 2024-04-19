package edu.arizona.cs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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
        File indexF = new File(indexFile);  // output index file
        
        // using Lucene to index
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(indexF.toPath());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);

        // get wiki dir FileD from Resource
        File wikiDirF = new File(classLoader.getResource(inputFileDir).getFile());

        int docCtr = 0;
        // loop through all the wiki files under the directory
        for(String i : wikiDirF.list()) {
            String singleFilePath = inputFileDir + "/" + i;
            System.out.println(i);
            docCtr++;

            //Get file from resources folder
            File file = new File(classLoader.getResource(singleFilePath).getFile());

            // scan through each line of a file
            String title = "";
            String categories = "";
            String text = "";
            try (Scanner inputScanner = new Scanner(new FileInputStream(file))) {
                while (inputScanner.hasNextLine()) {
                    String currLine = inputScanner.nextLine();
                    
                    // parsing each line
                    int lineLen = currLine.length();
                    // checking title line
                    if (currLine.startsWith("[[") && currLine.endsWith("]]") && !currLine.contains("Image:") && !currLine.contains("File:")) {
                        // add the previous stored title, categaries, and text as Doc to lucene
                        if (!title.equals("")) {
                            addDoc(w, title, categories, text);
                        }
                        
                        // store new title and starts a new page
                        title = currLine.substring(2, lineLen - 2);
                         System.out.println(title);

                    }
                    else if (currLine.startsWith("CATEGORIES:")) {
                        categories = currLine.substring(12).trim();
                        // System.out.println(categories);
                    }
                    else {
                        text = text + currLine.trim() + " ";
                    }

                }
                //add the last page to w
                addDoc(w, title, categories, text);
                inputScanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        System.out.println(docCtr);
        w.close();
        index.close();
    }

    private void addDoc(IndexWriter w, String title, String categories, String text) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("title", title, Field.Store.YES));
        doc.add(new TextField("categories", categories, Field.Store.YES));
        doc.add(new TextField("text", text, Field.Store.YES));
        w.addDocument(doc);
    }
    
}
