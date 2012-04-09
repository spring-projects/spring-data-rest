package org.springframework.data.rest.repository;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.rest.core.Handler;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class JpaEntityMetadata {

  private Class<?> targetType;
  private Map<String, Attribute> embeddedAttributes = new HashMap<String, Attribute>();
  private Map<String, Field> fields = new HashMap<String, Field>();
  private Map<String, Method> setters = new HashMap<String, Method>();
  private Map<String, Method> getters = new HashMap<String, Method>();
  private Map<String, Attribute> linkedAttributes = new HashMap<String, Attribute>();
  private final Attribute idAttribute;
  private final Attribute versionAttribute;

  @SuppressWarnings({"unchecked"})
  public JpaEntityMetadata(EntityType entityType, JpaRepositoryMetadata repositoryMetadata) {
    targetType = entityType.getJavaType();

    Attribute idAttribute = entityType.getId(entityType.getIdType().getJavaType());
    Attribute versionAttribute = entityType.getVersion(Long.class);
    for (Attribute attr : (Set<Attribute>) entityType.getAttributes()) {
      String name = attr.getName();
      Field f = ReflectionUtils.findField(targetType, attr.getName());
      ReflectionUtils.makeAccessible(f);
      fields.put(name, f);
      Method setter = null;
      try {
        setter = targetType.getMethod("set" + StringUtils.capitalize(name), attr.getJavaType());
      } catch (NoSuchMethodException e) {}
      if (null != setter) {
        setters.put(name, setter);
      }
      Method getter = null;
      try {
        getter = targetType.getMethod("get" + StringUtils.capitalize(name));
      } catch (NoSuchMethodException e) {}
      if (null != setter) {
        getters.put(name, getter);
      }

      if (attr instanceof SingularAttribute) {
        SingularAttribute sattr = (SingularAttribute) attr;
        if (null != repositoryMetadata.repositoryFor(attr.getJavaType())) {
          linkedAttributes.put(name, attr);
        } else if (!sattr.isId() && !sattr.isVersion()) {
          embeddedAttributes.put(name, attr);
        }
      } else if (attr instanceof PluralAttribute) {
        PluralAttribute pattr = (PluralAttribute) attr;
        if (pattr.getElementType() instanceof EntityType
            && null != repositoryMetadata.repositoryFor(pattr.getElementType().getJavaType())) {
          linkedAttributes.put(name, attr);
        } else {
          embeddedAttributes.put(name, attr);
        }
      }
    }

    this.idAttribute = idAttribute;
    this.versionAttribute = versionAttribute;
  }

  public Class<?> targetType() {
    return targetType;
  }

  public Map<String, Attribute> embeddedAttributes() {
    return Collections.unmodifiableMap(embeddedAttributes);
  }

  public Map<String, Attribute> linkedAttributes() {
    return Collections.unmodifiableMap(linkedAttributes);
  }

  public Attribute idAttribute() {
    return idAttribute;
  }

  public Attribute versionAttribute() {
    return versionAttribute;
  }

  public void id(Serializable id, Object target) {
    set(idAttribute.getName(), id, target);
  }

  public Object id(Object target) {
    return get(idAttribute.getName(), target);
  }

  public Object version(Object target) {
    return (null != versionAttribute ? get(versionAttribute.getName(), target) : null);
  }

  public <V> V doWithEmbedded(Handler<Attribute, V> handler) {
    if (null == handler) {
      return null;
    }
    V v = null;
    for (Attribute attr : embeddedAttributes.values()) {
      v = handler.handle(attr);
    }
    return v;
  }

  public <V> V doWithLinked(String name, Handler<Attribute, V> handler) {
    if (null == handler) {
      return null;
    }
    V v = null;
    Attribute attr = linkedAttributes.get(name);
    if (null != attr) {
      v = handler.handle(attr);
    }
    return v;
  }

  public <V> V doWithLinked(Handler<Attribute, V> handler) {
    if (null == handler) {
      return null;
    }
    V v = null;
    for (Attribute attr : linkedAttributes.values()) {
      v = handler.handle(attr);
    }
    return v;
  }

  public Object get(String name, Object target) {
    try {
      Method getter = getters.get(name);
      if (null != getter) {
        return getter.invoke(target);
      } else {
        Field f = fields.get(name);
        return (null != f ? f.get(target) : null);
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }

  public void set(String name, Object arg, Object target) {
    try {
      Method setter = setters.get(name);
      if (null != setter) {
        setter.invoke(target, arg);
      } else {
        Field f = fields.get(name);
        if (null != f) {
          f.set(target, arg);
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }

}
