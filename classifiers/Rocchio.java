package ir.classifiers;

import java.io.*;
import java.util.*;

import ir.vsr.*;
import ir.utilities.*;


public class Rocchio extends Classifier {
  /**
   * Flag to set Laplace smoothing when estimating probabilities
   */
  boolean isLaplace = true;

  /**
   * Small value to be used instead of 0 in probabilities, if Laplace smoothing is not used
   */
  double EPSILON = 1e-6;

  /**
   * Stores the training result, set by the train function
   */
  BayesResult trainResult;

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

  /**
   * List of prototype vectors
   */
  List<HashMapVector> prototypes = new ArrayList<HashMapVector>();

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
    initiatePrototypes();
  }

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
   * Returns training result
   */
  public BayesResult getTrainResult() {
    return trainResult;
  }

  /**
   * Returns value of isLaplace
   */
  public boolean getIsLaplace() {
    return (isLaplace);
  }



  public void train(List<Example> trainExamples)
  {
    //iterate through categories, constructing prototype vectors
    int temp_id = 1;
    double scale;
    for (String c : categories)
    {
      HashMapVector prototype = prototypes.get(temp_id-1);
      for (Example e : trainExamples)
      {
        if (e.getCategory() == temp_id)  //example matches category, add to vector
        {
          scale = e.hashVector.maxWeight();
          prototype.addScaled(e.hashVector, scale);
        }
        else  //example doesn't match category, subtract if neg flag is set
        {
          if (neg)
          {
            scale = e.hashVector.maxWeight();
            prototype.addScaled(e.hashVector, -scale);
          }
        }
      }
      temp_id++;
    }
  }


  public boolean test(Example testExample)
  {
    double maxCosSim = -2.0;
    HashMapVector d = testExample.getHashMapVector();
    double sim;
    int predictedClass = 0;
    HashMapVector p;

    HashMapVector[] protos = prototypes.toArray(new HashMapVector[prototypes.size()]);
    for (int i=0; i<protos.length; i++)
    {
      p = protos[i];
      sim = d.cosineTo(p);
      if (sim > maxCosSim)
      {
        maxCosSim = sim;
        predictedClass = i+1;
        //predictedClass = i;
      }
    }

    if (debug) {
      System.out.print("Document: " + testExample.name + "\nResults: ");
      System.out.println("\nCorrect class: " + testExample.getCategory() + ", Predicted class: " + predictedClass + "\n");
    }
    return (predictedClass == testExample.getCategory());
  }

  public void initiatePrototypes()
  {
    for (int i=0; i<numCategories; i++)
      prototypes.add(new HashMapVector());
  }
}