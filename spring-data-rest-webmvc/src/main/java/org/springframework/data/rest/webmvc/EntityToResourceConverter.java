package org.springframework.data.rest.webmvc;

import static org.springframework.data.rest.core.util.UriUtils.*;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.rest.repository.AttributeMetadata;
import org.springframework.data.rest.repository.EntityMetadata;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.util.Assert;

/**
 * A {@link Converter} to turn domain entities into {@link Resource}s by segregating embedded entities (those entities
 * not managed by a {@link org.springframework.data.repository.Repository}) from linked or related entities (which
 * don't get inlined into an entity's representation but are replaced by links instead.
 *
 * @author Jon Brisbin
 */
public class EntityToResourceConverter implements Converter<Object, Resource> {

  private final RepositoryRestConfiguration config;
  private final RepositoryMetadata          repositoryMetadata;
  private final EntityMetadata              entityMetadata;

  public EntityToResourceConverter(RepositoryRestConfiguration config,
                                   RepositoryMetadata repositoryMetadata) {
    this.config = config;
    Assert.notNull(repositoryMetadata, "RepositoryMetadata cannot be null!");
    this.repositoryMetadata = repositoryMetadata;
    this.entityMetadata = repositoryMetadata.entityMetadata();
  }

  @SuppressWarnings({"unchecked"})
  @Override public Resource convert(Object source) {
    if(null == repositoryMetadata || null == source) {
      return new Resource<Object>(source);
    }

    Serializable id = (Serializable)repositoryMetadata.entityMetadata().idAttribute().get(source);
    URI selfUri = buildUri(config.getBaseUri(), repositoryMetadata.name(), String.format("%s", id));

    Set<Link> links = new HashSet<Link>();
    for(Object attrName : entityMetadata.linkedAttributes().keySet()) {
      URI uri = buildUri(selfUri, attrName.toString());
      String rel = repositoryMetadata.rel() + "." + source.getClass().getSimpleName() + "." + attrName;
      links.add(new Link(uri.toString(), rel));
    }
    links.add(new Link(selfUri.toString(), "self"));

    Map<String, Object> entityDto = new HashMap<String, Object>();
    for(Map.Entry<String, AttributeMetadata> attrMeta : ((Map<String, AttributeMetadata>)entityMetadata.embeddedAttributes())
        .entrySet()) {
      String name = attrMeta.getKey();
      Object val;
      if(null != (val = attrMeta.getValue().get(source))) {
        entityDto.put(name, val);
      }
    }

    return new EntityResource(entityDto, links);
  }

}
