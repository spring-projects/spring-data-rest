package org.springframework.data.rest.repository.invoke;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.repository.support.Methods;

/**
 * An abstraction to encapsulate metadata about a repository method.
 *
 * @author Jon Brisbin
 */
public class RepositoryMethod {

  private Method method;
  private List<MethodParameter> methodParameters = new ArrayList<MethodParameter>();
  private List<String>          paramNames       = new ArrayList<String>();
  private boolean               pageable         = false;
  private boolean               sortable         = false;

  public RepositoryMethod(Method method) {
    this.method = method;

    Class<?>[] paramTypes = method.getParameterTypes();
    String[] paramNames = Methods.NAME_DISCOVERER.getParameterNames(method);
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

    int idx = 0;
    for(Class<?> type : paramTypes) {
      if(Pageable.class.isAssignableFrom(type)) {
        pageable = true;
      }
      if(Sort.class.isAssignableFrom(type)) {
        sortable = true;
      }
      methodParameters.add(new MethodParameter(method, idx));
      idx++;
    }

    Collections.addAll(this.paramNames, paramNames);
  }

  /**
   * Get the method parameter types.
   *
   * @return Array of parameter types.
   */
  public List<MethodParameter> getParameters() {
    return methodParameters;
  }

  /**
   * Get the method parameter names.
   *
   * @return Array of parameter names.
   */
  public List<String> getParameterNames() {
    return paramNames;
  }

  /**
   * Get the reflected {@link Method} to invoke.
   *
   * @return The {@link Method} to invoke.
   */
  public Method getMethod() {
    return method;
  }

  /**
   * Flag denoting whether this repository method returns a {@link org.springframework.data.domain.Page} result or not.
   *
   * @return {@literal true} if this method returns a {@link org.springframework.data.domain.Page}, {@literal false}
   *         otherwise.
   */
  public boolean isPageable() {
    return pageable;
  }

  /**
   * Flag denoting whether this repository method accepts sorting information.
   *
   * @return {@literal true} if this method accepts a {@link Sort}, {@literal false} otherwise.
   */
  public boolean isSortable() {
    return sortable;
  }

}
