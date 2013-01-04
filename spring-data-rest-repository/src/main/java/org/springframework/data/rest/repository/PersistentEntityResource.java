package org.springframework.data.rest.repository;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

/**
 * A Spring HATEOAS {@link Resource} subclass that holds a reference to the entity's {@link PersistentEntity} metadata.
 *
 * @author Jon Brisbin
 */
public class PersistentEntityResource<T> extends BaseUriAwareResource<T> {

  @JsonIgnore
  private final PersistentEntity<T, ?> persistentEntity;

  @SuppressWarnings({"unchecked"})
  public static <T> PersistentEntityResource<T> wrap(PersistentEntity persistentEntity,
                                                     T obj,
                                                     URI baseUri) {
    PersistentEntityResource<T> resource = new PersistentEntityResource<T>(persistentEntity, obj);
    resource.setBaseUri(baseUri);
    return resource;
  }

  public PersistentEntityResource(PersistentEntity<T, ?> persistentEntity) {
    this.persistentEntity = persistentEntity;
  }

  public PersistentEntityResource(PersistentEntity<T, ?> persistentEntity,
                                  T content,
                                  Link... links) {
    super(content, links);
    this.persistentEntity = persistentEntity;
  }

  public PersistentEntityResource(PersistentEntity<T, ?> persistentEntity,
                                  T content,
                                  Iterable<Link> links) {
    super(content, links);
    this.persistentEntity = persistentEntity;
  }

  public PersistentEntity<T, ?> getPersistentEntity() {
    return persistentEntity;
  }

}
