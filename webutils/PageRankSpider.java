package ir.webutils;

import java.util.*;
import java.net.*;
import java.io.*;



public class PageRankSpider extends Spider
{

  //Graph containing all links processed by spider
  protected Graph PageRankGraph = new Graph();

  Node currentNode;
  Node newNode;

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
        // System.out.println("Adding the following links" + newLinks);
        // Add new links to end of queue
        linksToVisit.addAll(newLinks);

/***************************************************************************************/
        //First, pull node for current link (or create if doesn't exist)
        currentNode = PageRankGraph.getNode(link.toString());


        //For each link, check if node exists in graph
        //if not, create node with link, add incoming edge from current link,
        //and add to graph
        //if it does, pull node from graph, add incoming edge, and put back in graph

        for (Link l : newLinks)
        {
          newNode = PageRankGraph.getNode(l.toString());
          if (!newNode.edgesIn.contains(currentNode))
          {
            newNode.addEdgeFrom(currentNode);
            currentNode.addEdge(newNode);
          }
        }

/***************************************************************************************/
      }
    }
    System.out.println("Printing Graph");
    System.out.println();
    PageRankGraph.print();
  }

  public static void main(String args[])
  {
    new PageRankSpider().go(args);
  }
}