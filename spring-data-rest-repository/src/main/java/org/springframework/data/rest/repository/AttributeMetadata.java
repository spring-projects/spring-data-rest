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
   * @return name of the attribute.
   */
  String name();

  /**
   * The type of this attribute.
   *
   * @return type of this attribute.
   */
  Class<?> type();

  /**
   * The type of this map's key, if it's map-like.
   *
   * @return
   */
  Class<?> keyType();

  /**
   * The element type of this attribute, if this attribute is a "plural"-like attribute (a Collection, Map, etc...).
   *
   * @return Class of element type or {@literal null} if not a plural attribute.
   */
  Class<?> elementType();

  /**
   * Can this attribute look like a {@link Collection}?
   *
   * @return {@literal true} if attribute is a Collection, {@literal false} otherwise.
   */
  boolean isCollectionLike();

  /**
   * Get the path of this attribute as a {@link Collection}.
   *
   * @param target
   *     The entity to inspect for this attribute.
   *
   * @return attribute value as a {@link Collection}
   */
  Collection<?> asCollection(Object target);

  /**
   * Can this attribute look like a {@link Set}?
   *
   * @return {@literal true} if attribute is a Set, {@literal false} otherwise.
   */
  boolean isSetLike();

  /**
   * Get the path of this attribute as a {@link Set}.
   *
   * @param target
   *     The entity to inspect for this attribute.
   *
   * @return attribute value as a {@link Set}
   */
  Set<?> asSet(Object target);

  /**
   * Can this attribute look like a {@link Map}?
   *
   * @return {@literal true} if attribute is a Map, {@literal false} otherwise.
   */
  boolean isMapLike();

  /**
   * Get the path of this attribute as a {@link Map}.
   *
   * @param target
   *     The entity to inspect for this attribute.
   *
   * @return attribute value as a {@link Map}
   */
  Map asMap(Object target);

  /**
   * Get the path of this attribute.
   *
   * @param target
   *     The entity to inspect for this attribute.
   *
   * @return attribute value
   */
  Object get(Object target);

  /**
   * Set the path of this attribute.
   *
   * @param value
   *     Value to set on this attribute.
   * @param target
   *     The entity to set this attribute's value on.
   *
   * @return @this
   */
  AttributeMetadata set(Object value, Object target);

}
