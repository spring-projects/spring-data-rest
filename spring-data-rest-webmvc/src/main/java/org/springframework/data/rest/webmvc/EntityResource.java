package org.springframework.data.rest.webmvc;

import static org.springframework.data.rest.core.util.UriUtils.*;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.springframework.data.rest.repository.AttributeMetadata;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

/**
 * @author Jon Brisbin
 */
public class EntityResource extends Resource<Map<String, Object>> {

  public EntityResource(Map<String, Object> dto, Set<Link> links) {
    super(dto, links);
  }

  @SuppressWarnings({"unchecked"})
  public static EntityResource wrap(Object entity, RepositoryMetadata repoMeta, URI baseUri) {

    Set<Link> links = new HashSet<Link>();
    for(Object attrName : repoMeta.entityMetadata().linkedAttributes().keySet()) {
      URI uri = buildUri(baseUri, attrName.toString());
      String rel = repoMeta.rel() + "." + entity.getClass().getSimpleName() + "." + attrName;
      links.add(new Link(uri.toString(), rel));
    }
    links.add(new Link(baseUri.toString(), "self"));

    Map<String, Object> entityDto = new HashMap<String, Object>();
    for(Map.Entry<String, AttributeMetadata> attrMeta : ((Map<String, AttributeMetadata>)repoMeta.entityMetadata()
                                                                                                 .embeddedAttributes()).entrySet()) {
      String name = attrMeta.getKey();
      Object val;
      if(null != (val = attrMeta.getValue().get(entity))) {
        entityDto.put(name, val);
      }
    }

    return new EntityResource(entityDto, links);
  }

  @JsonAnyGetter
  @Override public Map<String, Object> getContent() {
    return super.getContent();
  }

}
