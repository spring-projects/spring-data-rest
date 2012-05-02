package org.springframework.data.rest.repository.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.rest.repository.annotation.HandleAfterDelete;
import org.springframework.data.rest.repository.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.repository.annotation.HandleAfterSave;
import org.springframework.data.rest.repository.annotation.HandleBeforeDelete;
import org.springframework.data.rest.repository.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.repository.annotation.HandleBeforeSave;
import org.springframework.data.rest.repository.annotation.RepositoryEventHandler;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ApplicationListener} that will dispatch {@link RepositoryEvent}s to handlers annotated with {@link
 * RepositoryEventHandler}.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class AnnotatedHandlerRepositoryEventListener
    implements ApplicationListener<RepositoryEvent>,
               ApplicationContextAware,
               InitializingBean {

  private String basePackage;
  private ApplicationContext applicationContext;
  private Multimap<Class<? extends RepositoryEvent>, EventHandlerMethod> handlerMethods = ArrayListMultimap.create();

  public AnnotatedHandlerRepositoryEventListener() {
  }

  public AnnotatedHandlerRepositoryEventListener(String basePackage) {
    this.basePackage = basePackage;
  }

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * Get the base package in which to search for event handlers.
   *
   * @return
   */
  public String getBasePackage() {
    return basePackage;
  }

  /**
   * Set the base package in which to search for event handlers.
   *
   * @param basePackage
   * @return
   */
  public AnnotatedHandlerRepositoryEventListener setBasePackage(String basePackage) {
    this.basePackage = basePackage;
    return this;
  }

  /**
   * Get the base package in which to search for event handlers.
   *
   * @return
   */
  public String basePackage() {
    return basePackage;
  }

  /**
   * Set the base package in which to search for event handlers.
   *
   * @param basePackage
   * @return
   */
  public AnnotatedHandlerRepositoryEventListener basePackage(String basePackage) {
    this.basePackage = basePackage;
    return this;
  }

  @Override public void afterPropertiesSet() throws Exception {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RepositoryEventHandler.class, true, true));
    for (BeanDefinition beanDef : scanner.findCandidateComponents(basePackage)) {
      String typeName = beanDef.getBeanClassName();
      Class<?> handlerType = ClassUtils.forName(typeName, ClassUtils.getDefaultClassLoader());
      RepositoryEventHandler typeAnno = handlerType.getAnnotation(RepositoryEventHandler.class);
      Class<?>[] targetTypes = typeAnno.value();
      if (targetTypes.length == 0) {
        targetTypes = new Class<?>[]{null};
      }
      for (final Class<?> targetType : targetTypes) {
        for (final Object handler : applicationContext.getBeansOfType(handlerType).values()) {
          ReflectionUtils.doWithMethods(
              handler.getClass(),
              new ReflectionUtils.MethodCallback() {
                @Override public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                  inspect(targetType, handler, method, HandleBeforeSave.class, BeforeSaveEvent.class);
                  inspect(targetType, handler, method, HandleAfterSave.class, AfterSaveEvent.class);
                  inspect(targetType, handler, method, HandleBeforeLinkSave.class, BeforeLinkSaveEvent.class);
                  inspect(targetType, handler, method, HandleAfterLinkSave.class, AfterLinkSaveEvent.class);
                  inspect(targetType, handler, method, HandleBeforeDelete.class, BeforeDeleteEvent.class);
                  inspect(targetType, handler, method, HandleAfterDelete.class, AfterDeleteEvent.class);
                }
              },
              new ReflectionUtils.MethodFilter() {
                @Override public boolean matches(Method method) {
                  return (!method.isSynthetic()
                      && !method.isBridge()
                      && method.getDeclaringClass() != Object.class
                      && !method.getName().contains("$"));
                }
              }
          );
        }
      }
    }
  }

  @Override public void onApplicationEvent(RepositoryEvent event) {
    Class<? extends RepositoryEvent> eventType = event.getClass();
    if (handlerMethods.containsKey(eventType)) {
      for (EventHandlerMethod handlerMethod : handlerMethods.get(eventType)) {
        try {
          Object src = event.getSource();
          if (ClassUtils.isAssignable(handlerMethod.targetType, src.getClass())) {
            List<Object> params = new ArrayList<Object>();
            params.add(src);
            if (event instanceof BeforeLinkSaveEvent) {
              params.add(((BeforeLinkSaveEvent) event).getLinked());
            } else if (event instanceof AfterLinkSaveEvent) {
              params.add(((AfterLinkSaveEvent) event).getLinked());
            }
            handlerMethod.method.invoke(handlerMethod.handler, params.toArray());
          }
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    }
  }

  private <T extends Annotation> void inspect(Class<?> targetType,
                                              Object handler,
                                              Method method,
                                              Class<T> annoType,
                                              Class<? extends RepositoryEvent> eventType) {
    T anno = method.getAnnotation(annoType);
    if (null != anno) {
      try {
        Class<?>[] targetTypes;
        if (null == targetType) {
          targetTypes = (Class<?>[]) anno.getClass().getMethod("value", new Class[0]).invoke(anno);
        } else {
          targetTypes = new Class<?>[]{targetType};
        }
        for (Class<?> type : targetTypes) {
          handlerMethods.put(eventType,
                             new EventHandlerMethod(type,
                                                    handler,
                                                    method));
        }
      } catch (NoSuchMethodException ignored) {
      } catch (InvocationTargetException ignored) {
      } catch (IllegalAccessException ignored) {
      }
    }
  }

  private class EventHandlerMethod {
    final Class<?> targetType;
    final Method method;
    final Object handler;

    private EventHandlerMethod(Class<?> targetType,
                               Object handler,
                               Method method) {
      this.targetType = targetType;
      this.method = method;
      this.handler = handler;
    }
  }

}
