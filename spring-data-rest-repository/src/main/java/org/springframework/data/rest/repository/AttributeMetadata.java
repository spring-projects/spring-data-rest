package org.springframework.data.rest.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface AttributeMetadata {

  String name();

  Class<?> type();

  Class<?> elementType();

  boolean isCollectionLike();

  Collection<?> asCollection(Object target);

  boolean isSetLike();

  Set<?> asSet(Object target);

  boolean isMapLike();

  Map asMap(Object target);

  Object get(Object target);

  AttributeMetadata set(Object value, Object target);

}
