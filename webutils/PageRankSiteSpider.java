package ir.webutils;

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.DecimalFormat;

import ir.utilities.*;



public class PageRankSiteSpider extends PageRankSpider
{
	@Override
  public List<Link> getNewLinks(HTMLPage page) {
    List<Link> links = new LinkExtractor(page).extractLinks();
    URL url = page.getLink().getURL();
    ListIterator<Link> iterator = links.listIterator();
    while (iterator.hasNext()) {
      Link link = iterator.next();
      if (!url.getHost().equals(link.getURL().getHost()))
        iterator.remove();
    }
    return links;
  }

  public static void main(String args[])
  {
    new PageRankSiteSpider().go(args);
  }
}