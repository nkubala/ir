package ir.classifiers;

import java.util.*;


public class TestRocchio {
  /**
   * A driver method for testing the NaiveBayes classifier using
   * 10-fold cross validation.
   *
   * @param args a list of command-line arguments.  Specifying "-debug"
   *             will provide detailed output
   */
  public static void main(String args[]) throws Exception {
    String dirName = "/u/mooney/ir-code/corpora/yahoo-science/";
    String[] categories = {"bio", "chem", "phys"};
    System.out.println("Loading Examples from " + dirName + "...");
    List<Example> examples = new DirectoryExamplesConstructor(dirName, categories).getExamples();
    System.out.println("Initializing Rocchio classifier...");


    Rocchio RC;
    boolean debug = false;
    boolean neg = false;

    //handle command line arguments
    for (int i=0; i<args.length; i++)
    {
      if (args[i].equals("-debug"))
        debug = true;
      if (args[i].equals("-neg"))
        neg = true;
    }

    RC = new Rocchio(categories, debug, neg);

    // Perform 10-fold cross validation to generate learning curve
    CVLearningCurve cvCurve = new CVLearningCurve(RC, examples);
    cvCurve.run();
  }
}