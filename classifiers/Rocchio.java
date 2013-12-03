package ir.classifiers;

import java.io.*;
import java.util.*;

import ir.vsr.*;
import ir.utilities.*;


public class Rocchio extends Classifier {

/**************************************************************/
  /**
   * Flag to set Laplace smoothing when estimating probabilities
   */
  boolean isLaplace = true;

  /**
   * Small value to be used instead of 0 in probabilities, if Laplace smoothing is not used
   */
  double EPSILON = 1e-6;

  /**
   * Name of classifier
   */
  public static final String name = "Rocchio";

  /**
   * Number of categories
   */
  int numCategories;

  /**
   * Number of features
   */
  int numFeatures;

  /**
   * Number of training examples, set by train function
   */
  int numExamples;

  /**
   * Flag for debug prints
   */
  boolean debug = false;

  /**
   * Flag for modified Rocchio classification
   */
  boolean neg = false;

  protected List<HashMapVector> prototypes = new ArrayList<HashMapVector>();

  protected HashMapVector[] prototype_array;

  protected Map<String, Double> idfMap = new HashMap<String, Double>();

  int test_count = 0;

/**
   * Create a Rocchio classifier with these attributes
   *
   * @param categories The array of Strings containing the category names
   * @param debug      Flag to turn on detailed output
   */
  public Rocchio(String[] categories, boolean debug, boolean neg) {
    this.categories = categories;
    this.debug = debug;
    this.neg = neg;
    numCategories = categories.length;
    initializePrototypes();
  }

/**************************************************************/
  /**
   * Sets the debug flag
   */
  public void setDebug(boolean bool) {
    debug = bool;
  }

  /**
   * Sets the Laplace smoothing flag
   */
  public void setLaplace(boolean bool) {
    isLaplace = bool;
  }

  /**
   * Sets the value of EPSILON (default 1e-6)
   */
  public void setEpsilon(double ep) {
    EPSILON = ep;
  }

  /**
   * Returns the name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns value of EPSILON
   */
  public double getEpsilon() {
    return EPSILON;
  }

  /**
   * Returns value of isLaplace
   */
  public boolean getIsLaplace() {
    return (isLaplace);
  }

/**************************************************************/

  public void train(List<Example> trainExamples)
  {
    int cat_id;
    HashMapVector p, r;
    String token;
    double scale, weight, idf;

    for (Example e : trainExamples)
    {
      //index all token in vector into idfMap
      for (Map.Entry<String, Weight> entry : e.getHashMapVector().entrySet())
      {
        token = entry.getKey();
        if (idfMap.containsKey(token))
          idfMap.put(token, idfMap.get(token)+1.0);
        else
          idfMap.put(token, 1.0);
      }

      cat_id = e.getCategory();
      p = prototype_array[cat_id];

      scale = e.getHashMapVector().maxWeight();
      p.addScaled(e.getHashMapVector(), 1.0/scale);


      //if neg, iterate through all prototypes, subtracting this example
      if (neg)
      {
        Iterator<HashMapVector> it = prototypes.iterator();
        while (it.hasNext())
        {
          r = it.next();
          if (r != p)
          {
            scale = e.hashVector.maxWeight();
            if (scale == 0)
              scale = 1.0;
            r.addScaled(e.getHashMapVector(), -1.0/scale);
          }
        }
      }
    }


    //iterate through idfMap, converting containing document counts to idfs
    for (Map.Entry<String, Double> entry : idfMap.entrySet())
    {
      idfMap.put(entry.getKey(), Math.log10((double)trainExamples.size() / entry.getValue()));
    }

    // //print idfMap to make sure all entries added correctly
    // for (Map.Entry<String, Double> entry : idfMap.entrySet())
    // {
    //   System.out.println(entry.getKey() + ": " + entry.getValue());
    // }




    //now all prototypes are constructed, iterate through each, scaling each entry by idf
    //for (HashMapVector pr : prototypes)
    for (HashMapVector pr : prototype_array)
    {
      for (Map.Entry<String, Weight> entry : pr.entrySet())
      {
        //System.out.println(entry.getKey() + ": " + entry.getValue().getValue());
        weight = entry.getValue().getValue();
        //if (weight != 0 && Double.isNaN(weight))
        if (weight != 0) //token value isn't 0
        {
          token = entry.getKey();
          idf = idfMap.get(token);
          //System.out.println("weight of " + token + " is " + weight);
          Weight w = new Weight();
          w.setValue(weight * idf);
          pr.hashMap.put(token, w);
        }
      }
    }
  }

  public boolean test(Example testExample)
  {
    double maxCosSim = -2.0;
    HashMapVector d = testExample.getHashMapVector();
    double sim, idf, weight, w;
    String token;
    int predictedClass = -1;
    HashMapVector p, copy;

    // //for printing idfMap
    // int print = 0;
    // for (Map.Entry<String, Weight> entry : d.entrySet())
    // {
    //   if (print > 50)
    //     break;
    //   //System.out.println("test count " + test_count);
    //   System.out.println("Example " + entry.getKey() + ": " + entry.getValue().getValue());
    // }

    //MAKE DEEP COPY OF EXAMPLE, LEST YOU GET FUCKED!!!!!!
    copy = d.copy();

    //scale example hashMapVector by idf weightings of all terms
    for (Map.Entry<String, Weight> entry : copy.entrySet())
    {
      if (entry.getValue().getValue() != 0)  //token is not 0
      {
        token = entry.getKey();
        //System.out.println("token is " + token);
        weight = entry.getValue().getValue(); //double value of weight
        //idf = idfMap.get(token);
        if (idfMap.get(token) == null)
        {
          //System.out.println("value of " + token + " is null");
          idf = 0;
        }
        else
        {
          idf = idfMap.get(token);
          //System.out.println("token " + token + " has idf " + idf);
        }

        if (idf != 0)
        {
          w = weight * idf;
          //copy.hashMap.put(token, w); //update value in e's HMV
          copy.hashMap.get(token).setValue(w);
        }
      }
    }




    for (int i=0; i<prototype_array.length; i++)
    {
      p = prototype_array[i];
      sim = copy.cosineTo(p);
      if (sim > maxCosSim)
      {
        maxCosSim = sim;
        predictedClass = i;
      }
    }
    test_count++;
    return (predictedClass == testExample.getCategory());
  }


  public void initializePrototypes()
  {
    for (int i=0; i<numCategories; i++)
      prototypes.add(new HashMapVector());
    prototype_array = prototypes.toArray(new HashMapVector[prototypes.size()]);
  }

}