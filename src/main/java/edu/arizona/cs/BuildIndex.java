package edu.arizona.cs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Scanner;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
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

	private boolean lemmatization; 
	private boolean stemming;

    public BuildIndex(boolean lemma, boolean stem){
        this.lemmatization = lemma;
        this.stemming = stem;
    }

    public void fileIndex(String inputFileDir, String indexFile) throws java.io.FileNotFoundException,java.io.IOException {
        // create the index file 
        ClassLoader classLoader = getClass().getClassLoader();
        File indexF = new File(indexFile);  // output index file
        
        // using Lucene to index
        StandardAnalyzer analyzer = new StandardAnalyzer();   // use default stop word set, lowercases the generated tokens.
        EnglishAnalyzer analyzer_stem = new EnglishAnalyzer();  // stop word, lowercase, porterstem
        LemmaAnalyzer analyzer_lemma = new LemmaAnalyzer();  // stop word, OpenNLPLemmatizer, lowercase 

        Directory index = FSDirectory.open(indexF.toPath());
        IndexWriterConfig config;
        if (stemming) {
            config = new IndexWriterConfig(analyzer_stem);
        }
        else if (lemmatization) {
            config = new IndexWriterConfig(analyzer_lemma);
        }
        else {
            config = new IndexWriterConfig(analyzer);
        }
        IndexWriter w = new IndexWriter(index, config);

        // get wiki dir FileD from Resource
        File wikiDirF = new File(classLoader.getResource(inputFileDir).getFile());

        int docCtr = 0;



        // print current time
        Date date = new Date();
        Timestamp ts = new Timestamp(date.getTime());
        System.out.println(ts);
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
            StringBuilder text = new StringBuilder();
            
            try (Scanner inputScanner = new Scanner(new FileInputStream(file))) {
                while (inputScanner.hasNextLine()) {
                    String currLine = inputScanner.nextLine();
                    
                    // parsing each line
                    int lineLen = currLine.length();
                    // checking title line
                    if (currLine.startsWith("[[") && currLine.endsWith("]]") && !currLine.contains("Image:") && !currLine.contains("File:")) {
                        // add the previous stored title, categaries, and text as Doc to lucene
                        if (!title.equals("")) {
                            addDoc(w, title, categories, text.toString());
                            // System.out.println(title);
                        }
                        // reinitialize fields
                        title = "";
                        categories = "";
                        text = new StringBuilder();
                        // store new title and starts a new page
                        title = currLine.substring(2, lineLen - 2);
                    }
                    else if (currLine.startsWith("CATEGORIES:")) {
                        categories = currLine.substring(12).trim();
                    }
                    else {

                        // I used plus operator to do string concat at first, the performance was slow. Then I switched to stringbuilder  
                        text.append(currLine.trim() + " ");                          
                    }

                }
                //add the last page to w
                addDoc(w, title, categories, text.toString());

                inputScanner.close();
                // print current time
                Date date1 = new Date();
                Timestamp ts1 = new Timestamp(date1.getTime());
                System.out.println(ts1);
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
