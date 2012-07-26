package org.springframework.data.rest.repository;

import java.io.Serializable;
import java.util.Map;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.rest.repository.invoke.RepositoryQueryMethod;

/**
 * Encapsulates necessary metadata about a {@link Repository}.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface RepositoryMetadata<E extends EntityMetadata<? extends AttributeMetadata>> {

  /**
   * The name this {@link Repository} is exported under.
   *
   * @return Name used in the URL for this Repository.
   */
  String name();

  /**
   * Get the string value to be used as part of a link {@literal rel} attribute.
   *
   * @return Rel value used in links.
   */
  String rel();

  /**
   * The type of domain object this {@link Repository} is repsonsible for.
   *
   * @return Type of the domain class.
   */
  Class<?> domainType();

  /**
   * The Class of the {@link Repository} subinterface.
   *
   * @return Type of the Repository being proxied.
   */
  Class<?> repositoryClass();

  /**
   * The {@link Repository} instance.
   *
   * @return The actual {@link Repository} instance.
   */
  CrudRepository<Object, Serializable> repository();

  /**
   * The {@link EntityMetadata} associated with the domain type of this {@literal Repository}.
   *
   * @return EntityMetadata associated with this Repository's domain type.
   */
  E entityMetadata();

  /**
   * Get a {@link org.springframework.data.rest.repository.invoke.RepositoryQueryMethod} by key.
   *
   * @param key
   *     Segment of the URL to find a query method for.
   *
   * @return Found {@link org.springframework.data.rest.repository.invoke.RepositoryQueryMethod} or {@literal null} if
   *         none found.
   */
  RepositoryQueryMethod queryMethod(String key);

  /**
   * Get a Map of all {@link RepositoryQueryMethod}s, keyed by name.
   *
   * @return All query methods for this Repository.
   */
  Map<String, RepositoryQueryMethod> queryMethods();

}
