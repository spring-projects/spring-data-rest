package org.springframework.data.rest.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates necessary information about an attribute of a generic entity.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface AttributeMetadata {

  /**
   * Name of the attribute.
   *
   * @return
   */
  String name();

  /**
   * The type of this attribute.
   *
   * @return
   */
  Class<?> type();

  /**
   * The element type of this attribute, if this attribute is a "plural"-like attribute (a Collection, Map, etc...).
   *
   * @return
   */
  Class<?> elementType();

  /**
   * Can this attribute look like a {@link Collection}?
   *
   * @return
   */
  boolean isCollectionLike();

  /**
   * Get the path of this attribute as a {@link Collection}.
   *
   * @param target
   * @return
   */
  Collection<?> asCollection(Object target);

  /**
   * Can this attribute look like a {@link Set}?
   *
   * @return
   */
  boolean isSetLike();

  /**
   * Get the path of this attribute as a {@link Set}.
   *
   * @param target
   * @return
   */
  Set<?> asSet(Object target);

  /**
   * Can this attribute look like a {@link Map}?
   *
   * @return
   */
  boolean isMapLike();

  /**
   * Get the path of this attribute as a {@link Map}.
   *
   * @param target
   * @return
   */
  Map asMap(Object target);

  /**
   * Get the path of this attribute.
   *
   * @param target
   * @return
   */
  Object get(Object target);

  /**
   * Set the path of this attribute.
   *
   * @param value
   * @param target
   * @return
   */
  AttributeMetadata set(Object value, Object target);

}
