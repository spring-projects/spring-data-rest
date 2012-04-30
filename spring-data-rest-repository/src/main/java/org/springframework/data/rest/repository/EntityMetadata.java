package org.springframework.data.rest.repository;

import java.util.Map;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface EntityMetadata<A extends AttributeMetadata> {

  Class<?> type();

  Map<String, A> embeddedAttributes();

  Map<String, A> linkedAttributes();

  A idAttribute();

  A versionAttribute();

  A attribute(String name);

}
