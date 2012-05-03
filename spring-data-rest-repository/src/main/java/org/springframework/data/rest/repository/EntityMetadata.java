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
   * @return Type of this domain class.
   */
  Class<?> type();

  /**
   * A Map of attribute metadata keyed on the attribute's name.
   *
   * @return Attributes that do not involve relationships.
   */
  Map<String, A> embeddedAttributes();

  /**
   * A Map of linked attribute metadata keyed on the attribute's name.
   *
   * @return Attributes that involve relationships.
   */
  Map<String, A> linkedAttributes();

  /**
   * The {@link AttributeMetadata} representing the ID of the entity.
   *
   * @return {@link AttributeMetadata} for the ID.
   */
  A idAttribute();

  /**
   * The {@link AttributeMetadata} representing the version of the entity, if applicable.
   *
   * @return {@link AttributeMetadata} or {@literal null} if no version attributes exists.
   */
  A versionAttribute();

  /**
   * Get {@link AttributeMetadata} by name.
   *
   * @param name The name of the attribute.
   * @return {@link AttributeMetadata} or {@literal null} if that attribute doesn't exist.
   */
  A attribute(String name);

}
