package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

/**
 * An inverted index for vector-space information retrieval. Contains
 * methods for creating an inverted index from a set of documents
 * and retrieving ranked matches to queries using standard TF/IDF
 * weighting and cosine similarity.
 *
 * @author Ray Mooney
 */
public class InvertedPhraseIndex extends InvertedIndex{

  /**
   * The maximum number of retrieved documents for a query to present to the user
   * at a time
   */
  public static final int MAX_RETRIEVALS = 10;

  /**
   * A HashMap where tokens are indexed. Each indexed token maps
   * to a TokenInfo.
   */
  public Map<String, TokenInfo> tokenHash = null;

  /**
   * A list of all indexed documents.  Elements are DocumentReference's.
   */
  public List<DocumentReference> docRefs = null;

  /**
   * The directory from which the indexed documents come.
   */
  public File dirFile = null;

  /**
   * The type of Documents (text, HTML). See docType in DocumentIterator.
   */
  public short docType = DocumentIterator.TYPE_TEXT;

  /**
   * Whether tokens should be stemmed with Porter stemmer
   */
  public boolean stem = false;

  /**
   * Whether relevance feedback using the Ide_regular algorithm is used
   */
  public boolean feedback = false;

  /**
   * Create an inverted index of the documents in a directory.
   *
   * @param dirFile  The directory of files to index.
   * @param docType  The type of documents to index (See docType in DocumentIterator)
   * @param stem     Whether tokens should be stemmed with Porter stemmer.
   * @param feedback Whether relevance feedback should be used.
   */
  public InvertedPhraseIndex(File dirFile, short docType, boolean stem, boolean feedback) {
    this.dirFile = dirFile;
    this.docType = docType;
    this.stem = stem;
    this.feedback = feedback;
    tokenHash = new HashMap<String, TokenInfo>();
    docRefs = new ArrayList<DocumentReference>();
    reviewDocuments();
    indexDocuments();
  }

  /**
   * Create an inverted index of the documents in a List of Example objects of documents
   * for text categorization.
   *
   * @param examples A List containing the Example objects for text categorization to index
   */
  public InvertedPhraseIndex(List<Example> examples) {
    tokenHash = new HashMap<String, TokenInfo>();
    docRefs = new ArrayList<DocumentReference>();
    reviewDocuments();
    indexDocuments(examples);
  }


  /*******************************************************************************/
  /*******************************************************************************/
  /* New
    9/22/2013 */

  /* Preprocesses documents to find most common phrases */
  protected void reviewDocuments()
  {
    DocumentIterator docIter = new DocumentIterator(dirFile, docType, stem);
    System.out.println("Reviewing documents in " + dirFile);


    while (docIter.hasMoreDocuments()) {
      FileDocument doc = docIter.nextDocument();
      // Create a document vector for this document
      System.out.print(doc.file.getName() + ",");
      //HashMapVector vector = doc.hashMapVector();
      HashMapVector vector = doc.bigramHashMapVector();
      reviewDocument(doc, vector);
    }
  }


  /* Preprocesses documents to find most common phrases */
  protected void reviewDocuments(List<Example> examples)
  {
    for (Example example : examples) {
      FileDocument doc = example.getDocument();
      // Create a document vector for this document
      HashMapVector vector = example.getHashMapVector();
      reviewDocument(doc, vector);
    }
  }


  /**
   * Review the given document using its corresponding vector
   * Iterate through tokens (should be bigrams), keeping track of their counts in knownPhrases
   */
  protected void reviewDocument(FileDocument doc, HashMapVector vector) {
    // Iterate through each of the tokens in the document
    // for (Map.Entry<String, Weight> entry : vector.entrySet()) {
    // }
    // Iterator entries = vector.entrySet().iterator();
    // String token1, token2, token;
    // token1 = entries.next().getKey();
    // while (entries.hasNext())
    // {
    //   token2 = entries.next().getKey();
    //   token = token1 + " " + token2; //this is the bigram

    //   if (knownPhrases.containsKey(token))
    //   {
    //     double val = knownPhrases.get(token);
    //     knownPhrases.put(token, val+1.0);
    //   }
    //   else
    //     knownPhrases.put(token, 1.0);

    //   token1 = token2;
    // }
    String token;
    for (Map.Entry<String, Weight> entry : vector.entrySet()) {
      token = entry.getKey();
      if (knownPhrases.containsKey(token))
      {
        double val = knownPhrases.get(token);
        knownPhrases.put(token, val+1.0);
      }
      else
        knownPhrases.put(token, 1.0);
    }
  }


  //sort list of known phrases on occurrences throughout corpus
  //trim size down to maxPhrases, while simultaneously printing key and value pairs
  public void printKnownPhrases()
  {
    //Map<String, Double> sortedPhrases = knownPhrases.sort();
    Map<String, Double> sortedPhrases = knownPhrases;
    Map<String, Double> newSortedPhrases = new HashMap<String, Double>();
    int idx = 0;
    while (idx < maxPhrases)
    {
      for (Map.Entry<String, Double> entry : sortedPhrases.entrySet())
      {
        newSortedPhrases.put(entry.getKey(), entry.getValue());
        System.out.println(entry.getKey() + ": " + entry.getValue());
      }
      idx++;
    }
    sortedPhrases = newSortedPhrases;
  }


  /*******************************************************************************/
  /*******************************************************************************/
}

   
