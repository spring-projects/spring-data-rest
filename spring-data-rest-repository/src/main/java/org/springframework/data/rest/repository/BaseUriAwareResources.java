package org.springframework.data.rest.repository;

import static org.springframework.data.rest.core.util.UriUtils.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;

/**
 * @author Jon Brisbin
 */
public class BaseUriAwareResources extends Resources<Resource<?>> {

  @JsonIgnore
  private URI baseUri;

  public BaseUriAwareResources() {
  }

  public BaseUriAwareResources(Iterable<Resource<?>> content, Link... links) {
    super(content, links);
  }

  public BaseUriAwareResources(Iterable<Resource<?>> content, Iterable<Link> links) {
    super(content, links);
  }

  public URI getBaseUri() {
    return baseUri;
  }

  public BaseUriAwareResources setBaseUri(URI baseUri) {
    this.baseUri = baseUri;
    return this;
  }

  @Override public Collection<Resource<?>> getContent() {
    List<Resource<?>> resources = new ArrayList<Resource<?>>();
    for(Resource<?> resource : super.getContent()) {
      if(resource instanceof BaseUriAwareResource) {
        resources.add(((BaseUriAwareResource)resource).setBaseUri(baseUri));
      } else {
        resources.add(new BaseUriAwareResource<Object>(resource.getContent(), resource.getLinks()).setBaseUri(baseUri));
      }
    }
    return resources;
  }

  @Override public Iterator<Resource<?>> iterator() {
    return getContent().iterator();
  }

  @Override public List<Link> getLinks() {
    List<Link> links = new ArrayList<Link>();
    for(Link l : super.getLinks()) {
      links.add(new Link(buildUri(baseUri, l.getHref()).toString(), l.getRel()));
    }
    return links;
  }

  @Override public Link getLink(String rel) {
    Link l = super.getLink(rel);
    return new Link(buildUri(baseUri, l.getHref()).toString(), l.getRel());
  }

}
