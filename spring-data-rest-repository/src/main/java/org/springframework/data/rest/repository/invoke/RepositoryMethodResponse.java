package org.springframework.data.rest.repository.invoke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.Link;

/**
 * JSON-serializable response for returns that have a mix of results and links. Also used in responses that have just
 * links (in that case, 'results' will be an empty array).
 *
 * @author Jon Brisbin
 */
public class RepositoryMethodResponse {

  @JsonProperty("results")
  private List<Object> results     = new ArrayList<Object>();
  @JsonProperty("links")
  private List<Link>   links       = new ArrayList<Link>();
  private long         totalCount  = 0;
  private int          totalPages  = 1;
  private int          currentPage = 1;

  public RepositoryMethodResponse addLink(Link l) {
    links.add(l);
    return this;
  }

  public RepositoryMethodResponse addResult(Object obj) {
    results.add(obj);
    return this;
  }

  public RepositoryMethodResponse addAllResults(Iterator results) {
    if(null == results) {
      return this;
    }

    while(results.hasNext()) {
      addResult(results.next());
    }

    return this;
  }

  public List<Object> getResults() {
    return results;
  }

  public boolean hasResults() {
    return (results.size() > 0);
  }

  public RepositoryMethodResponse setResults(List<Object> results) {
    if(null == results) {
      this.results = Collections.emptyList();
    } else {
      this.results = results;
    }
    return this;
  }

  public List<Link> getLinks() {
    return links;
  }

  public RepositoryMethodResponse setLinks(List<Link> links) {
    if(null == links) {
      this.links = Collections.emptyList();
    } else {
      this.links = links;
    }
    return this;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public RepositoryMethodResponse setTotalCount(long totalCount) {
    this.totalCount = totalCount;
    return this;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public RepositoryMethodResponse setTotalPages(int totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  public int getCurrentPage() {
    return currentPage;
  }

  public RepositoryMethodResponse setCurrentPage(int currentPage) {
    this.currentPage = currentPage;
    return this;
  }

}
