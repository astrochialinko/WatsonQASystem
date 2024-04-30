package edu.arizona.cs;
import org.apache.lucene.document.Document;

public class ResultClass {
    Document DocName;
    double docScore = 0;
    String docTitle = DocName.get("title");
    
    public ResultClass(Document doc, double score) {
        this.DocName = doc;
        this.docScore = score;
    }
    
    public String getTitle() {
        return docTitle;
    }

    public double getScore() {
        return docScore;
    }

	public void setScore(double score) {
		this.docScore = score;
	}
	
	public void setTitle(String title) {
		this.docTitle = title;
	}
}
