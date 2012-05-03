package org.springframework.data.rest.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * Helper methods for dealing with the metadata of "fluent" beans.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class FluentBeanUtils {

  private static final Logger log = LoggerFactory.getLogger(FluentBeanUtils.class);
  private static final LoadingCache<Class<?>, Metadata> metadata = CacheBuilder.newBuilder().build(
      new CacheLoader<Class<?>, Metadata>() {
        @Override public Metadata load(Class<?> type) throws Exception {
          final Metadata meta = new Metadata();
          ReflectionUtils.doWithFields(
              type,
              new ReflectionUtils.FieldCallback() {
                @Override public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                  final String fname = field.getName();
                  if (!fname.startsWith("_")) {
                    ReflectionUtils.doWithMethods(field.getDeclaringClass(), new ReflectionUtils.MethodCallback() {
                      @Override
                      public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                        if (method.getName().equals(fname)) {
                          if (method.getParameterTypes().length == 0) {
                            meta.getters.put(fname, method);
                          } else if (method.getParameterTypes().length == 1) {
                            meta.setters.put(fname, method);
                          }
                          meta.fieldNames.add(fname);
                        }
                      }
                    });
                  }
                }
              }
          );
          return meta;
        }
      }
  );

  /**
   * Interrogate a bean and collect {@link Metadata} on it.
   *
   * @param targetType The type to interrogate.
   * @return {@link Metadata} for the fluent bean.
   */
  public static Metadata metadata(Class<?> targetType) {
    try {
      return metadata.get(targetType);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Set the property of a fluent bean.
   *
   * @param property Name of the property to set.
   * @param value    Value of the property.
   * @param bean     Bean on which to set this property.
   * @return Usually {@literal null} but will return whatever the "setter" returns, which could be {@this} or something
   *         else.
   */
  public static Object set(String property, Object value, Object bean) {
    if (null == bean) {
      return null;
    }

    Class<?> type = bean.getClass();
    try {
      Method setter = metadata.get(type).setters.get(property);
      if (null != setter) {
        return setter.invoke(bean, value);
      } else {
        return null;
      }
    } catch (Throwable t) {
      if (log.isDebugEnabled()) {
        log.debug(t.getMessage(), t);
      }
      return null;
    }
  }

  /**
   * Get the value of a property.
   *
   * @param property Name of the property.
   * @param bean     Bean of which to get the property.
   * @return Value of the property. Could be {@literal null}
   */
  public static Object get(String property, Object bean) {
    if (null == bean) {
      return null;
    }

    Class<?> type = bean.getClass();
    try {
      Method getter = metadata.get(type).getters.get(property);
      if (null != getter) {
        return getter.invoke(bean);
      } else {
        return null;
      }
    } catch (Throwable t) {
      if (log.isDebugEnabled()) {
        log.debug(t.getMessage(), t);
      }
      return null;
    }
  }

  /**
   * Determines whether a given type looks like a fluent bean. That means it has methods whose names exactly correspond
   * to a field of the same name. A "getter" is that method which is named the same as the field and has 0 parameters.
   * The "setter" is that method which is named the same as the field and has a single argument.
   *
   * @param type The class to inspect.
   * @return {@literal true} if this looks like a fluent bean, {@literal false} otherwise.
   */
  public static boolean isFluentBean(Class<?> type) {
    try {
      return metadata.get(type).getters.size() > 0;
    } catch (ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  public static class Metadata {
    List<String> fieldNames = new ArrayList<String>();
    Map<String, Method> getters = new HashMap<String, Method>();
    Map<String, Method> setters = new HashMap<String, Method>();

    public List<String> fieldNames() {
      return fieldNames;
    }

    public Map<String, Method> getters() {
      return getters;
    }

    public Map<String, Method> setters() {
      return setters;
    }
  }

}
