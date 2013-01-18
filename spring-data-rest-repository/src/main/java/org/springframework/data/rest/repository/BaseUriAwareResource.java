package org.springframework.data.rest.repository;

import static org.springframework.data.rest.core.util.UriUtils.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

/**
 * @author Jon Brisbin
 */
public class BaseUriAwareResource<T> extends Resource<T> {

  @JsonIgnore
  private URI baseUri;

  public BaseUriAwareResource() {
  }

  public BaseUriAwareResource(T content, Link... links) {
    super(content, links);
  }

  public BaseUriAwareResource(T content, Iterable<Link> links) {
    super(content, links);
  }

  public URI getBaseUri() {
    return baseUri;
  }

  public BaseUriAwareResource<T> setBaseUri(URI baseUri) {
    this.baseUri = baseUri;
    return this;
  }

  @Override public List<Link> getLinks() {
    String baseUriStr = baseUri.toString();
    List<Link> links = new ArrayList<Link>();
    for(Link l : super.getLinks()) {
      if(!l.getHref().startsWith(baseUriStr)) {
        links.add(new Link(buildUri(baseUri, l.getHref()).toString(), l.getRel()));
      } else {
        links.add(l);
      }
    }
    return links;
  }

  @Override public Link getLink(String rel) {
    Link l = super.getLink(rel);
    if(null == l) {
      return null;
    }
    if(!l.getHref().startsWith(baseUri.toString())) {
      return new Link(buildUri(baseUri, l.getHref()).toString(), l.getRel());
    } else {
      return l;
    }
  }

}
