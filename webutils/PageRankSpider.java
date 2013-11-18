package ir.webutils;

import java.util.*;
import java.net.*;
import java.io.*;



public class PageRankSpider extends Spider
{

  //Graph containing all links processed by spider
  protected Graph PageRankGraph = new Graph();

  //Hashmap containing all links and their PageRanks
  protected Map<String, Double> PageRankMap = new HashMap<String, Double>();

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
      }
      if (count < maxCount)
      {
        List<Link> newLinks = getNewLinks(currentPage);
        // Add new links to end of queue
        linksToVisit.addAll(newLinks);

/***************************************************************************************/
        //Time to build the link graph
        //First, pull node for current link (or create if doesn't exist)
        currentNode = PageRankGraph.getNode(link.toString());

        //For each link, check if node exists in graph
        //if not, create node with link, add incoming edge from current link,
        //and add to graph
        //if it does, pull node from graph, add incoming edge, and put back in graph

        String temp_str;
        for (Link l : newLinks)
        {
          contains = 0;
          newNode = PageRankGraph.getNode(l.toString());

          if (!newNode.edgesIn.contains(currentNode))
          {
            //newNode.addEdgeFrom(currentNode);
            currentNode.addEdge(newNode);
          }
        }

/***************************************************************************************/
      }
    }

/***************************************************************************************/
    //Time to compute PageRanks
    //Will store in a hashMap of PageRanks (key:value = url_name:PageRank)

    //constants for use in algorithm
    double alpha = 0.15;
    Node[] nodes = PageRankGraph.nodeArray();
    int num_pages = nodes.length; //equivalent to |S| in algorithm
    double e_p = alpha / num_pages;
    
    //DEBUG
    //checking incoming nodes
    // for (Node n : nodes)
    // {
    //   System.out.println("Incoming Edges for " + n.toString());
    //   List<Node> in_list = n.getEdgesIn();
    //   for (Node in : in_list)
    //   {
    //     System.out.println(in.toString());
    //   }
    //   System.out.println("Outgoing Edges for " + n.toString());
    //   List<Node> out_list = n.getEdgesOut();
    //   for (Node out : out_list)
    //   {
    //     System.out.println(out.toString());
    //   }
    // }

    //temps/counter
    double new_val = 0;
    double norm = 0;
    int count = 0;
    double r_q = 0;
    int n_q = 0;

    //initialize each PageRank to 1/|s|
    for (Node n : nodes)
    {
      PageRankMap.put(n.toString(), (double)1/num_pages);
    }

    //iterate through nodes, computing page ranks
    while (count < 50)
    {
      for (Node n : nodes)
      {
        //iterate through all pages pointing to current page
        for (Node in_node : n.getEdgesIn())
        {
          //summing R(q)/N_q for all incoming pages
          r_q = PageRankMap.get(in_node.toString());
          n_q = in_node.getEdgesOut().size();
          new_val += r_q / n_q;
        }
        new_val *= (1.0 - alpha);
        new_val += e_p;

        //new_val is now computed R'(p), update value in hashmap
        PageRankMap.put(n.toString(), new_val);
        new_val = 0;
      }

      //sum all total PageRanks to normalize
      norm = 0;
      for (Node n : nodes)
      {
        norm += PageRankMap.get(n.toString());
      }
      norm = 1 / norm;

      //normalize all ranks
      for (Node n : nodes)
      {
        PageRankMap.put(n.toString(), norm * PageRankMap.get(n.toString()));
      }

      count++;
    }





/***************************************************************************************/   
    // System.out.println("Printing Graph");
    // System.out.println();
    // PageRankGraph.print();
    PrintPageRanks();
  }


  private void PrintPageRanks()
  {
    for (Map.Entry<String, Double> entry : PageRankMap.entrySet())
      System.out.println(entry.getKey() + "\t" + entry.getValue());
  }

  public static void main(String args[])
  {
    new PageRankSpider().go(args);
  }
}