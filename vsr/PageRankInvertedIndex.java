package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

public class PageRankInvertedIndex extends InvertedIndex
{

	//weight for PageRanks
	private int weight = 0;

	//file containing PageRanks
	private File pageRankFile = null;

	//hashMap used when adding PageRanks to document scores
	private Map<String, Double> pageRankMap = new HashMap<String, Double>();

	//overridden constructor
	//@Override
  	public PageRankInvertedIndex(File dirFile, short docType, boolean stem, boolean feedback, int weight, File pageRankFile)
  	{
  		super(dirFile, docType, stem, feedback);

	    this.dirFile = dirFile;
	    this.docType = docType;
	    this.stem = stem;
	    this.feedback = feedback;
	    this.weight = weight;
	    this.pageRankFile = pageRankFile;
	    tokenHash = new HashMap<String, TokenInfo>();
	    docRefs = new ArrayList<DocumentReference>();
	    indexDocuments();
  	}

  	//Overriden processQueries() which incorporates PageRanks into scores
  	//@Override
	public void processQueries()
	{

	    System.out.println("Now able to process queries. When done, enter an empty query to exit.");
	    // Loop indefinitely answering queries
	    do {
	      // Get a query from the console
	      String query = UserInput.prompt("\nEnter query:  ");
	      // If query is empty then exit the interactive loop
	      if (query.equals(""))
	        break;
	      // Get the ranked retrievals for this query string and present them
	      HashMapVector queryVector = (new TextStringDocument(query, stem)).hashMapVector();
	      Retrieval[] retrievals = retrieve(queryVector);
	      addPageRanks(retrievals);
	      presentRetrievals(queryVector, retrievals);
	    }
	    while (true);
	}

	//Overridden indexDocuments() which skips pageRanks file
    protected void indexDocuments()
    {
	    if (!tokenHash.isEmpty() || !docRefs.isEmpty()) {
	      // Currently can only index one set of documents when an index is created
	      throw new IllegalStateException("Cannot indexDocuments more than once in the same InvertedIndex");
	    }
	    // Get an iterator for the documents
	    DocumentIterator docIter = new DocumentIterator(dirFile, docType, stem);
	    System.out.println("Indexing documents in " + dirFile);
	    // Loop, processing each of the documents

	    while (docIter.hasMoreDocuments()) {
	      FileDocument doc = docIter.nextDocument();
	      // Create a document vector for this document
	      System.out.print(doc.file.getName() + ",");
	      //skip pageRanks file
	      if (!doc.file.getName().contains("page") && !doc.file.getName().contains("ranks")) 
	      {
		      HashMapVector vector = doc.hashMapVector();
		      indexDocument(doc, vector);
		  }
	    }
	    // Now that all documents have been processed, we can calculate the IDF weights for
	    // all tokens and the resulting lengths of all weighted document vectors.
	    computeIDFandDocumentLengths();
	    System.out.println("\nIndexed " + docRefs.size() + " documents with " + size() + " unique terms.");
    }

    //adds PageRank to every document score
	public void addPageRanks(Retrieval[] retrievals)
	{
		String line, page, rankStr, retName;
		double rank, retScore;

		//move PageRanks from file to hashmap (for easy parsing)
		try {
			Scanner sc = new Scanner(pageRankFile);
			while (sc.hasNextLine())
			{
				line = sc.nextLine();
				//System.out.println("line is " + line);
				page = line.split("\t")[0];
				rankStr = line.split("\t")[1];
				rank = Double.parseDouble(rankStr);
				pageRankMap.put(page, rank);
			}
			sc.close();
		}
		catch (FileNotFoundException e) {
    		System.err.println("FileNotFoundException: " + e.getMessage());
    		//throw new SampleException(e);
		} catch (IOException e) {
    		System.err.println("Caught IOException: " + e.getMessage());
		}

		//iteratively update retrieval scores
		for (Retrieval r : retrievals)
		{
			retName = r.docRef.file.getName();
			retScore = r.score;
			rank = pageRankMap.get(retName);
			r.score = ((1.0 - (weight / 100)) * retScore) + ((weight / 100) * rank);
		}
	}

	public static void main(String[] args)
	{
	    //Parse the arguments into a directory name and optional flag
	    String dirName = args[args.length - 1];
	    short docType = DocumentIterator.TYPE_TEXT;
	    boolean stem = false, feedback = false;
	    int weight = 0;

	    for (int i = 0; i < args.length - 1; i++) {
	      String flag = args[i];
	      if (flag.equals("-html"))
	        // Create HTMLFileDocuments to filter HTML tags
	        docType = DocumentIterator.TYPE_HTML;
	      else if (flag.equals("-stem"))
	        // Stem tokens with Porter stemmer
	        stem = true;
	      else if (flag.equals("-feedback"))
	        // Use relevance feedback
	        feedback = true;
	      else if (flag.equals("-weight"))
	      	//get weight and open PageRank file
	      {
	      	weight = Integer.parseInt(args[++i]);
	      }
	      else {
	        throw new IllegalArgumentException("Unknown flag: "+ flag);
	      }
	    }


	    // Create an inverted index for the files in the given directory.
	    PageRankInvertedIndex index = new PageRankInvertedIndex(new File(dirName), docType, stem, feedback, weight, new File(dirName, "pageRanks"));
	    // index.print();
	    // Interactively process queries to this index.
	    index.processQueries();
	}
}