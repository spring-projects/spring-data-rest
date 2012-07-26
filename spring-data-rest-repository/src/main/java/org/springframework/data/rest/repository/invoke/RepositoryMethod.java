package org.springframework.data.rest.repository.invoke;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.util.ReflectionUtils;

/**
 * @author Jon Brisbin
 */
public class RepositoryMethod {

  public enum Type {
    COUNT,
    CUSTOM,
    DELETE,
    FIND_ALL,
    FIND_ONE,
    SAVE;

    public static Type fromMethodName(String s) {
      if("count".equals(s)) {
        return COUNT;
      } else if("delete".equals(s)) {
        return DELETE;
      } else if("findAll".equals(s)) {
        return FIND_ALL;
      } else if("findOne".equals(s)) {
        return FIND_ONE;
      } else if("save".equals(s)) {
        return SAVE;
      } else {
        return CUSTOM;
      }
    }

    public String toMethodName() {
      switch(this) {
        case COUNT:
          return "count";
        case DELETE:
          return "delete";
        case FIND_ALL:
          return "findAll";
        case FIND_ONE:
          return "findOne";
        case SAVE:
          return "save";
        default:
          return null;
      }
    }

  }

  public static final ReflectionUtils.MethodFilter              USER_METHODS    = new ReflectionUtils.MethodFilter() {
    @Override public boolean matches(Method method) {
      return (!method.isSynthetic()
          && !method.isBridge()
          && method.getDeclaringClass() != Object.class
          && !method.getName().contains("$"));
    }
  };
  public static final LocalVariableTableParameterNameDiscoverer NAME_DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

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
    paramNames = NAME_DISCOVERER.getParameterNames(method);
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
