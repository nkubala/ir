package ir.webutils;

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.DecimalFormat;

import ir.utilities.*;



public class PageRankSpider extends Spider
{

  //Graph containing all links processed by spider
  protected Graph pageRankGraph = new Graph();

  //Hashmap containing all links and their PageRanks
  protected Map<String, Double> pageRankMap = new HashMap<String, Double>();

  //hashmap used in computing pageranks
  protected Map<String, Double> tempRankMap = new HashMap<String, Double>();

  //final map containing sorted page ranks
  protected Map<String, Double> sortedRanks = new TreeMap<String, Double>();
  
  //maps link urls to Pxxx.html names
  protected Map<String, String> linkNameMap = new HashMap<String, String>();

  Node currentNode;
  Node newNode;

  //annoying boolean for checking edges in
  int contains = 0;

  @Override
  public void doCrawl()
  {
    if (linksToVisit.size() == 0)
    {
      System.err.println("Exiting: No pages to visit.");
      System.exit(0);
    }
    visited = new HashSet<Link>();
    while (linksToVisit.size() > 0 && count < maxCount)
    {
      // Pause if in slow mode
      if (slow) {
        synchronized (this) {
          try {
            wait(1000);
          }
          catch (InterruptedException e) {
          }
        }
      }
      // Take the top link off the queue
      Link link = linksToVisit.remove(0);
      System.out.println("Trying: " + link);
      // Skip if already visited this page
      if (!visited.add(link)) {
        System.out.println("Already visited");
        continue;
      }
      if (!linkToHTMLPage(link)) {
        System.out.println("Not HTML Page");
        continue;
      }
      HTMLPage currentPage = null;
      // Use the page retriever to get the page
      try {
        currentPage = retriever.getHTMLPage(link);
      }
      catch (PathDisallowedException e) {
        System.out.println(e);
        continue;
      }
      if (currentPage.empty()) {
        System.out.println("No Page Found");
        continue;
      }
      if (currentPage.indexAllowed()) {
        count++;
        System.out.println("Indexing" + "(" + count + "): " + link);
        indexPage(currentPage);

        //create (url, pxxx.html) pair and add to linkNameMap
        String nameStr = "P" + MoreString.padWithZeros(count, (int) Math.floor(MoreMath.log(maxCount, 10)) + 1);
        nameStr += ".html";
        linkNameMap.put(link.toString(), nameStr);
      }
      if (count < maxCount)
      {
        List<Link> newLinks = getNewLinks(currentPage);
        // Add new links to end of queue
        linksToVisit.addAll(newLinks);


        //Time to build the link graph
        //First, pull node for current link (or create if doesn't exist)
        if (linkToHTMLPage(link))
        {
          System.out.println("Adding Node " + link.toString());
          currentNode = pageRankGraph.getNode(link.toString());

          //For each link, check if node exists in graph
          //if not, create node with link, add incoming edge from current link,
          //and add to graph
          //if it does, pull node from graph, add incoming edge, and put back in graph

          String temp_str;
          for (Link l : newLinks)
          {
            String[] temp = l.toString().split("/");
            String tmp = temp[temp.length-1];
            if (linkToHTMLPage(l) && !tmp.contains("?"))
            {
              newNode = pageRankGraph.getNode(l.toString());
              if (!newNode.edgesIn.contains(currentNode))
              {
                //newNode.addEdgeFrom(currentNode);
                currentNode.addEdge(newNode);
              }

              // List<Node> nodes_out = currentNode.getEdgesOut();
              // contains = 0;
              // for (Node out : nodes_out)
              // {
              //   if (out.toString().equals(newNode.toString()))
              //     contains = 1;
              // }
              // if (contains == 0)
              // {
              //   currentNode.addEdge(newNode);
              // }
            }
          }
        }
      }
    }

    //Time to compute PageRanks
    //Will store in a hashMap of PageRanks (key:value = url_name:PageRank)

    // Iterator it = linkNameMap.entrySet().iterator();
    // while (it.hasNext())
    // {
    //   Map.Entry pair = (Map.Entry)it.next();
    //   System.out.println(pair.getValue() + ":" + pair.getKey());
    // }


    //pageRankGraph.print();

    computePageRanks();
    sortPageRanks();
    //printFileMappings();
    // printPageRanks();
    writePageRanks();
  }

  private void printFileMappings()
  {
    Iterator it = linkNameMap.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry pair = (Map.Entry)it.next();
      System.out.println(pair.getKey() + ":" + pair.getValue());
    }
  }

  private void computePageRanks()
  {
    //constants for use in algorithm
    double alpha = 0.15;
    Node[] nodes = pageRankGraph.nodeArray();
    double num_pages = (double)nodes.length; //equivalent to |S| in algorithm
    double e_p = alpha / num_pages;


    //DEBUG: checking edges in/out
    // for (Node n : nodes)
    // {
    //   System.out.println("Nodes into " + n.toString());
    //   for (Node in_node : n.getEdgesIn())
    //   {
    //     System.out.println(in_node.toString());
    //   }

    //   System.out.println("Nodes out of " + n.toString());
    //   for (Node out_node : n.getEdgesOut())
    //   {
    //     System.out.println(out_node.toString());
    //   }
    // }


    //temps/counter
    double new_val = 0;
    double norm = 0;
    int counter = 0;
    double r_q = 0;
    double n_q = 0;
    double sum = 0;

    //initialize each PageRank to 1/|s|
    for (Node n : nodes)
    {
      pageRankMap.put(n.toString(), (double)1.0/num_pages);
    }



    //iterate through nodes, computing page ranks
    while (counter < 50)
    {

      //DEBUG: summing pageranks
      sum = 0;
      Iterator it = pageRankMap.entrySet().iterator();
      while (it.hasNext())
      {
        Map.Entry pair = (Map.Entry)it.next();
        sum += (double)pair.getValue();
      }
      System.out.println("sum of ranks is " + sum);

      
      for (Node n : nodes)
      {
        //iterate through all pages pointing to current page
        for (Node in_node : n.getEdgesIn())
        {
          //summing R(q)/N_q for all incoming pages
          r_q = pageRankMap.get(in_node.toString());
          n_q = (double)in_node.getEdgesOut().size();
          new_val += r_q / n_q;
        }
        new_val *= (1.0 - alpha);
        new_val += e_p;

        //new_val is now computed R'(p), update value in hashmap
        //pageRankMap.put(n.toString(), new_val);
        tempRankMap.put(n.toString(), new_val);
        new_val = 0;
      }

      //sum all total PageRanks to normalize
      norm = 0;
      for (Node n1 : nodes)
      {
        norm += tempRankMap.get(n1.toString());
      }
      norm = 1.0 / norm;

      //normalize all ranks
      for (Node n2 : nodes)
      {
        pageRankMap.put(n2.toString(), norm * tempRankMap.get(n2.toString()));
      }

      counter++;
    }
  }

  private void sortPageRanks()
  {
    Iterator it = linkNameMap.entrySet().iterator();
    String temp;
    while (it.hasNext())
    {
      Map.Entry pair = (Map.Entry)it.next();
      temp = (String)pair.getKey();
      sortedRanks.put((String)pair.getValue(), pageRankMap.get(temp));
    }

    // it = sortedRanks.entrySet().iterator();
    // while (it.hasNext())
    // {
    //   Map.Entry pair = (Map.Entry)it.next();
    //   System.out.println(pair.getKey() + ":" + pair.getValue());
    // }
  }

  private void printPageRanks()
  {
    DecimalFormat df = new DecimalFormat("0.0000000000");
    for (Map.Entry<String, Double> entry : pageRankMap.entrySet())
    {
      if (linkNameMap.get(entry.getKey()) != null)
          //System.out.printf(linkNameMap.get(entry.getKey()) + "\t" + "%f\n", entry.getValue());
        System.out.println(linkNameMap.get(entry.getKey()) + "\t" + df.format(entry.getValue()));
    }
  }

  private void writePageRanks()
  {
    // DecimalFormat df = new DecimalFormat("0.0000000000");
    // try {
    //   PrintWriter out = new PrintWriter(new FileWriter(new File(saveDir, "pageRanks")));
    //   for (Map.Entry<String, Double> entry : pageRankMap.entrySet())
    //   {
    //     if (linkNameMap.get(entry.getKey()) != null)
    //       //out.printf(linkNameMap.get(entry.getKey()) + "\t" + "%f\n", entry.getValue());
    //       out.println(linkNameMap.get(entry.getKey()) + "\t" + df.format(entry.getValue()));
    //   }
    //   out.close();
    // }
    // catch (IOException e) {
    //   System.err.println("HTMLPage.write(): " + e);
    // }

    DecimalFormat df = new DecimalFormat("0.0000000000");
    try {
      PrintWriter out = new PrintWriter(new FileWriter(new File(saveDir, "pageRanks")));
      for (Map.Entry<String, Double> entry : sortedRanks.entrySet())
      {
          out.println(entry.getKey() + "\t" + df.format(entry.getValue()));
      }
      out.close();
    }
    catch (IOException e) {
      System.err.println("HTMLPage.write(): " + e);
    }
  }

  public static void main(String args[])
  {
    new PageRankSpider().go(args);
  }
}