package org.springframework.data.rest.repository.jpa;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.EntityMetadata;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link EntityMetadata} for JPA.
 *
 * @author Jon Brisbin
 */
public class JpaEntityMetadata implements EntityMetadata<JpaAttributeMetadata> {

  private Class<?>             type;
  private JpaAttributeMetadata idAttribute;
  private JpaAttributeMetadata versionAttribute;
  private Map<String, JpaAttributeMetadata> embeddedAttributes = new HashMap<String, JpaAttributeMetadata>();
  private Map<String, JpaAttributeMetadata> linkedAttributes   = new HashMap<String, JpaAttributeMetadata>();

  @SuppressWarnings({"unchecked"})
  public JpaEntityMetadata(Repositories repositories, EntityType<?> entityType) {
    type = entityType.getJavaType();
    idAttribute = new JpaAttributeMetadata(entityType, entityType.getId(entityType.getIdType().getJavaType()));
    try {
      if(null != entityType.getVersion(Long.class)) {
        versionAttribute = new JpaAttributeMetadata(entityType, entityType.getVersion(Long.class));
      }
    } catch(IllegalArgumentException ignored) {
      // No version exists, just ignore it
    }

    for(Attribute attr : entityType.getAttributes()) {
      boolean exported = true;
      Field field = ReflectionUtils.findField(type, attr.getJavaMember().getName());
      if(null == field) {
        continue;
      }

      RestResource fieldResourceAnno = field.getAnnotation(RestResource.class);
      if(null != fieldResourceAnno) {
        exported = fieldResourceAnno.exported();
      }
      if(exported) {
        String name = attr.getName();
        if(null != fieldResourceAnno && StringUtils.hasText(fieldResourceAnno.path())) {
          name = fieldResourceAnno.path();
        }
        Class<?> attrType = (attr instanceof PluralAttribute
                             ? ((PluralAttribute)attr).getElementType().getJavaType()
                             : attr.getJavaType());
        if(repositories.hasRepositoryFor(attrType)) {
          linkedAttributes.put(name, new JpaAttributeMetadata(entityType, attr));
        } else {
          if((attr instanceof SingularAttribute && ((SingularAttribute)attr).isId())) {
            // Don't export the id attribute
            continue;
          } else if(((attr instanceof SingularAttribute) && ((SingularAttribute)attr).isVersion())
              && (null == fieldResourceAnno || !StringUtils.hasText(fieldResourceAnno.path()))) {
            // Don't export the version attribute
            continue;
          }
          embeddedAttributes.put(name, new JpaAttributeMetadata(entityType, attr));
        }
      }
    }
  }

  @Override public Class<?> type() {
    return type;
  }

  @Override public Map<String, JpaAttributeMetadata> embeddedAttributes() {
    return embeddedAttributes;
  }

  @Override public Map<String, JpaAttributeMetadata> linkedAttributes() {
    return linkedAttributes;
  }

  @Override public JpaAttributeMetadata idAttribute() {
    return idAttribute;
  }

  @Override public JpaAttributeMetadata versionAttribute() {
    return versionAttribute;
  }

  @Override public JpaAttributeMetadata attribute(String name) {
    if(idAttribute.name().equals(name)) {
      return idAttribute;
    } else if(null != versionAttribute && versionAttribute.name().equals(name)) {
      return versionAttribute;
    } else if(embeddedAttributes.containsKey(name)) {
      return embeddedAttributes.get(name);
    } else if(linkedAttributes.containsKey(name)) {
      return linkedAttributes.get(name);
    }
    return null;
  }

  @Override public String toString() {
    return "JpaEntityMetadata{" +
        "type=" + type +
        ", idAttribute=" + idAttribute +
        ", versionAttribute=" + versionAttribute +
        ", embeddedAttributes=" + embeddedAttributes +
        ", linkedAttributes=" + linkedAttributes +
        '}';
  }

}
