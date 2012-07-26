package org.springframework.data.rest.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class BeanUtils {

  private BeanUtils() {
  }

  public static ConfigurableConversionService CONVERSION_SERVICE = new DefaultConversionService();

  private static final LoadingCache<Object[], Field>  fields  = CacheBuilder.newBuilder().build(
      new CacheLoader<Object[], Field>() {
        @Override public Field load(Object[] key)
            throws Exception {
          Class<?> clazz = (Class<?>)key[0];
          String name = (String)key[1];
          Field f = ReflectionUtils.findField(clazz, name);
          if(null != f) {
            ReflectionUtils.makeAccessible(f);
            return f;
          } else {
            throw new IllegalArgumentException("Field " + clazz.getName() + "." + name + " not found");
          }
        }
      }
  );
  private static final LoadingCache<Object[], Method> methods = CacheBuilder.newBuilder().build(
      new CacheLoader<Object[], Method>() {
        @Override public Method load(Object[] key)
            throws Exception {
          Class<?> clazz = (Class<?>)key[0];
          String name = (String)key[1];
          Integer paramCnt = key.length == 3 ? (Integer)key[2] : 0;

          for(Method m : clazz.getDeclaredMethods()) {
            if(m.getName().equals(name)) {
              if(m.getParameterTypes().length == paramCnt) {
                ReflectionUtils.makeAccessible(m);
                return m;
              }
            }
          }

          throw new IllegalArgumentException("Method " + clazz.getName() + "." + name + " not found");
        }
      }
  );

  public static boolean hasProperty(String property, Object... objs) {
    for(Object obj : objs) {
      if(obj instanceof Map) {
        return ((Map)obj).containsKey(property);
      }
      Class<?> type = obj.getClass();
      try {
        if(FluentBeanUtils.isFluentBean(type)) {
          return null != methods.get(new Object[]{type, property});
        } else {
          if(null == methods.get(new Object[]{type, "get" + StringUtils.capitalize(property)})) {
            return null != fields.get(new Object[]{type, property});
          } else {
            return true;
          }
        }
      } catch(UncheckedExecutionException e) {
        if(e.getCause().getClass() == IllegalArgumentException.class) {
          return false;
        } else {
          throw new IllegalStateException(e);
        }
      } catch(ExecutionException e) {
        throw new IllegalStateException(e);
      }
    }
    return false;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T findFirst(Class<T> clazz, List<?> stack) {
    for(Object o : stack) {
      if(ClassUtils.isAssignable(clazz, o.getClass())) {
        return (T)o;
      }
    }
    return null;
  }

  @SuppressWarnings({"unchecked"})
  public static Object findFirst(Object o, Object... objs) {
    for(Object obj : objs) {
      if(o == obj || null != o && o.equals(obj)) {
        return obj;
      } else if(obj instanceof List) {
        return Collections.binarySearch((List)obj, o);
      } else if(obj instanceof Object[]) {
        return Arrays.binarySearch((Object[])obj, o);
      }
    }
    return null;
  }

  public static Object findFirst(String property, Object... objs) {
    for(Object obj : objs) {
      if(obj instanceof Map) {
        return ((Map)obj).get(property);
      }
      Class<?> type = obj.getClass();
      try {
        Field f = fields.get(new Object[]{type, property});
        if(FluentBeanUtils.isFluentBean(type)) {
          return FluentBeanUtils.get(property, obj);
        } else {
          Method getter = methods.get(new Object[]{type, "get" + StringUtils.capitalize(property)});
          try {
            if(null != getter) {
              return getter.invoke(obj);
            } else {
              return f.get(obj);
            }
          } catch(IllegalAccessException e) {
            throw new IllegalStateException(e);
          } catch(InvocationTargetException e) {
            throw new IllegalStateException(e);
          }
        }
      } catch(IllegalArgumentException e) {
      } catch(ExecutionException e) {
        throw new IllegalArgumentException(e);
      }
    }

    return null;
  }

  public static boolean containsType(Class<?> type, List<Object> objs) {
    return containsType(type, objs.toArray());
  }

  public static boolean containsType(Class<?> type, Object[] objs) {
    for(Object obj : objs) {
      if(null != obj && ClassUtils.isAssignable(obj.getClass(), type)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings({"unchecked"})
  public static Object invoke(String methodName, Object target, Object... args) {
    return invoke(methodName, target, Object.class, args);
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T invoke(String methodName, Object target, Class<T> returnType, Object... args) {
    if(null == target) {
      return null;
    }

    Class<?> type = target.getClass();
    try {
      Method m = methods.get(new Object[]{type, methodName, args.length});
      List<Object> newArgs = new ArrayList<Object>(args.length);
      Class<?>[] paramTypes = m.getParameterTypes();
      for(int i = 0; i < args.length; i++) {
        Object o = args[i];
        Class<?> oType = o.getClass();
        Class<?> pType = paramTypes[i];
        if(!ClassUtils.isAssignable(oType, pType)) {
          newArgs.add(CONVERSION_SERVICE.convert(o, pType));
        } else {
          newArgs.add(o);
        }
      }

      Object rtnVal = m.invoke(target, newArgs.toArray());
      if((returnType != Void.TYPE || returnType != Object.class)
          && null != rtnVal
          && !ClassUtils.isAssignable(returnType, rtnVal.getClass())) {
        return CONVERSION_SERVICE.convert(rtnVal, returnType);
      } else {
        return (T)rtnVal;
      }
    } catch(IllegalArgumentException e) {
    } catch(Exception e) {
      throw new IllegalStateException(e);
    }

    return null;
  }

}
