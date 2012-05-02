package org.springframework.data.rest.repository;

import java.util.Map;

/**
 * Encapsulates necessary metadata about a generic entity.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface EntityMetadata<A extends AttributeMetadata> {

  /**
   * The class of this entity.
   *
   * @return
   */
  Class<?> type();

  /**
   * A Map of attribute metadata keyed on the attribute's name.
   *
   * @return
   */
  Map<String, A> embeddedAttributes();

  /**
   * A Map of linked attribute metadata keyed on the attribute's name.
   *
   * @return
   */
  Map<String, A> linkedAttributes();

  /**
   * The {@link AttributeMetadata} representing the ID of the entity.
   *
   * @return
   */
  A idAttribute();

  /**
   * The {@link AttributeMetadata} representing the version of the entity, if applicable.
   *
   * @return
   */
  A versionAttribute();

  /**
   * Get {@link AttributeMetadata} by name.
   *
   * @param name
   * @return
   */
  A attribute(String name);

}
