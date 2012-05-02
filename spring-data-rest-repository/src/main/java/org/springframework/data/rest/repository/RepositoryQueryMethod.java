package org.springframework.data.rest.repository;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.repository.query.Param;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class RepositoryQueryMethod {

  private static final LocalVariableTableParameterNameDiscoverer nameLookup = new LocalVariableTableParameterNameDiscoverer();

  private Method method;
  private Class<?>[] paramTypes;
  private String[] paramNames;

  public RepositoryQueryMethod(Method method) {
    this.method = method;
    paramTypes = method.getParameterTypes();
    paramNames = nameLookup.getParameterNames(method);
    if (null == paramNames) {
      paramNames = new String[paramTypes.length];
    }
    Annotation[][] paramAnnos = method.getParameterAnnotations();
    for (int i = 0; i < paramAnnos.length; i++) {
      if (paramAnnos[i].length > 0) {
        for (Annotation anno : paramAnnos[i]) {
          if (Param.class.isAssignableFrom(anno.getClass())) {
            Param p = (Param) anno;
            paramNames[i] = p.value();
            break;
          }
        }
      }
      if (null == paramNames[i]) {
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

}
