package org.springframework.data.rest.core.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ClassUtils;

/**
 * A "fluent" bean is one that does not use the JavaBean conventions of "setProperty" and "getProperty" but instead
 * uses just "property" with 0 or 1 arguments to distinguish between getter (0 arg) and setter (1 arg).
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class FluentBeanDeserializer extends StdDeserializer {

  private ConversionService        conversionService;
  private FluentBeanUtils.Metadata beanMeta;

  @SuppressWarnings({"unchecked"})
  public FluentBeanDeserializer(final Class<?> valueClass, ConversionService conversionService) {
    super(valueClass);
    this.conversionService = conversionService;
    this.beanMeta = FluentBeanUtils.metadata(valueClass);

    if(!FluentBeanUtils.isFluentBean(valueClass)) {
      throw new IllegalArgumentException("Class of type " + valueClass + " is not a FluentBean");
    }
  }

  @Override
  public Object deserialize(JsonParser jp,
                            DeserializationContext ctxt) throws IOException {
    if(jp.getCurrentToken() != JsonToken.START_OBJECT) {
      throw ctxt.mappingException(_valueClass);
    }

    Object bean;
    try {
      bean = _valueClass.newInstance();
    } catch(InstantiationException e) {
      throw new IllegalStateException(e);
    } catch(IllegalAccessException e) {
      throw new IllegalStateException(e);
    }

    while(jp.nextToken() != JsonToken.END_OBJECT) {
      String name = jp.getCurrentName();
      Method setter = beanMeta.setters().get(name);

      Object obj;
      if(null != setter) {
        Class<?> targetType = setter.getParameterTypes()[0];
        if(ClassUtils.isAssignable(targetType, Long.class)) {
          obj = jp.nextLongValue(-1);
        } else if(ClassUtils.isAssignable(targetType, Integer.class)) {
          obj = jp.nextIntValue(-1);
        } else if(ClassUtils.isAssignable(targetType, Boolean.class)) {
          obj = jp.nextBooleanValue();
        } else {
          obj = jp.nextTextValue();
        }

        if(null != obj) {
          if(!ClassUtils.isAssignable(obj.getClass(), targetType)) {
            obj = conversionService.convert(obj, targetType);
          }

          try {
            setter.invoke(bean, obj);
          } catch(IllegalAccessException e) {
            throw new IllegalStateException(e);
          } catch(InvocationTargetException e) {
            throw new IllegalStateException(e);
          }
        }
      }

    }

    return bean;
  }

}
