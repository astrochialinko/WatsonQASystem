# WatsonQASystem
Building (a part of) IBM’s Watson Question Answering (QA) system

- CSC 583 Final Project (Spring 2024)
- Authors: Junfeng Xu and Chia-Lin Ko

## Motivation
IBM’s Watson is a Question Answering (QA) system that “can compete at the human champion level in real time on the TV quiz show, Jeopardy.” This, as we will see in class, is a complex undertaking. However, the answers to many of the Jeopardy questions are actually titles of Wikipedia pages. For example, the answer to the clue “This woman who won consecutive heptathlons at the Olympics went to UCLA on a basketball scholarship” is “Jackie Joyner-Kersee”, who has a Wikipedia page with the same title: http://en.wikipedia.org/wiki/Jackie_Joyner-Kersee. In these situations, the task reduces to the classification of Wikipedia pages, that is, finding which page is the most likely answer to the given clue. This is the focus of this project.

## What the code does?
- Indexing and Retrieval
- Measuring Performance
- Error Analysis
- Improved Implementation


## How to run the code?

1. clone the git repository https://github.com/astrochialinko/WatsonQASystem
2. download all 4 index files from https://drive.google.com/drive/folders/1G-6E7y7_5KKqEu-CcnfOB4f7YBQtIDkK?usp=sharing and unzip them under the git repository directory.
3. change directory to the git repository folder and there are 5 tests you can run:
>`TestWastonStd` This test runs query on index with stop words removal, lowercasefilter, standard tokenizer.</p>
>`TestWastonLemma` This test runs query on index with stop words removal, lowercasefilter, standard tokenizer, Lemmanization with OpenNLPLemmatizerFilter.</p>
>`TestWastonWiki` This test runs query on index with stop words removal, lowercasefilter, wikipedia tokenizer.</p>
>`TestWastonStem` This test runs query on index with stop words removal, lowercasefilter, standard tokenizer, Porter Stemming. </p>
>`TestWastonStemChat` This test runs query on index with stop words removal, lowercasefilter, standard tokenizer, Porter Stemming, then rerank top 10/100 results by ChatGPT.</p>
4. to run a test above. Issue `$ mvn -Dtest=<TestName> test`. Note: if you want to run the `TestWastonStemChat` test you need to go to QueryEngine.java and update the apiKey field with your ChatGPT secret key.
5. the output will show the performance result including Precision at 1, Mean Reciprocal Rank, etc. for each of the 5 similarity formulas below:
>`BM25Similarity`</p>
>`BooleanSimilarity`</p>
>`ClassicSimilarity`</p>
>`LMDirichletSimilarity`</p>
>`LMJelinekMercerSimilarity`</p>

## Dataset
- 100 questions from previous Jeopardy games, whose answers appear as Wikipedia pages. The questions are listed in a single file, with 4 lines per question, in the following format: `CATEGORY CLUE ANSWER NEWLINE`.
  For example:
  ```
  NEWSPAPERS
  The dominant paper in our nation’s capital, it’s among the top 10 U.S. papers in circulation
  The Washington Post
  ```
- A collection of approximately 280,000 Wikipedia pages, which include the correct answers for the above 100 questions. The pages are stored in 80 files (thus each file contains several thousand pages). Each page starts with its title, encased in double square brackets. For example, BBC’s page starts with “[[BBC]]”.

## File structures
```
WatsonQASystem
├── README.md
├── en-lemmatizer.dict.txt
├── index-file-lemma/
├── index-file-std/
├── index-file-stem/
├── index-file-wiki/
├── pom.xml
├── notebooks
│   └── plot_analysis.ipynb
├── src
│   ├── main/java/edu/arizona/cs
│   │   │               ├── BuildIndex.java
│   │   │               ├── LemmaAnalyzer.java
│   │   │               ├── MainWatson.java
│   │   │               ├── QueryEngine.java
│   │   │               ├── ResultClass.java
│   │   │               └── WikipediaAnalyzer.java
│   │   └── resources
│   │       ├── questions.txt
│   │       └── wiki-folder
│   └── test/java/edu/arizona/cs
│                       ├── TestWastonLemma.java
│                       ├── TestWastonStd.java
│                       ├── TestWastonStem.java
│                       ├── TestWastonWiki.java
│                       └── TestWastonStemChat.java
└── target/
```

## References
https://lucene.apache.org/ </p>
https://platform.openai.com/docs/api-reference </p>


## Acknowledgements

We would like to acknowledge Prof. Mihai Surdeanu and TA Haris Riaz for their guidance and support throughout the semester.
