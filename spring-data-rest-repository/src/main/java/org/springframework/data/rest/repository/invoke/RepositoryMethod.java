package org.springframework.data.rest.repository.invoke;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.repository.support.Methods;

/**
 * @author Jon Brisbin
 */
public class RepositoryMethod {

  private Method     method;
  private Class<?>[] paramTypes;
  private String[]   paramNames;
  private boolean pageable = false;
  private boolean sortable = false;

  public RepositoryMethod(Method method) {
    this.method = method;
    paramTypes = method.getParameterTypes();
    for(Class<?> type : paramTypes) {
      if(Pageable.class.isAssignableFrom(type)) {
        pageable = true;
      }
      if(Sort.class.isAssignableFrom(type)) {
        sortable = true;
      }
    }
    paramNames = Methods.NAME_DISCOVERER.getParameterNames(method);
    if(null == paramNames) {
      paramNames = new String[paramTypes.length];
    }
    Annotation[][] paramAnnos = method.getParameterAnnotations();
    for(int i = 0; i < paramAnnos.length; i++) {
      if(paramAnnos[i].length > 0) {
        for(Annotation anno : paramAnnos[i]) {
          if(Param.class.isAssignableFrom(anno.getClass())) {
            Param p = (Param)anno;
            paramNames[i] = p.value();
            break;
          }
        }
      }
      if(null == paramNames[i]) {
        paramNames[i] = "arg" + i;
      }
    }
  }

  public Class<?>[] paramTypes() {
    return paramTypes;
  }

  public String[] paramNames() {
    return paramNames;
  }

  public Method method() {
    return method;
  }

  public boolean pageable() {
    return pageable;
  }

  public boolean sortable() {
    return sortable;
  }

}
