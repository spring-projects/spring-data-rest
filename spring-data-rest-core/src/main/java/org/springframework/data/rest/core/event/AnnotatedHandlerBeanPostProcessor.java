package org.springframework.data.rest.core.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterLinkDelete;
import org.springframework.data.rest.core.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * @author Jon Brisbin
 */
public class AnnotatedHandlerBeanPostProcessor implements ApplicationListener<RepositoryEvent>, BeanPostProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(AnnotatedHandlerBeanPostProcessor.class);
	private final MultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod> handlerMethods = new LinkedMultiValueMap<Class<? extends RepositoryEvent>, AnnotatedHandlerBeanPostProcessor.EventHandlerMethod>();

	@Override
	public void onApplicationEvent(RepositoryEvent event) {
		Class<? extends RepositoryEvent> eventType = event.getClass();
		if (!handlerMethods.containsKey(eventType)) {
			return;
		}

		for (EventHandlerMethod handlerMethod : handlerMethods.get(eventType)) {
			try {
				Object src = event.getSource();

				if (!ClassUtils.isAssignable(handlerMethod.targetType, src.getClass())) {
					continue;
				}

				List<Object> params = new ArrayList<Object>();
				params.add(src);
				if (event instanceof BeforeLinkSaveEvent) {
					params.add(((BeforeLinkSaveEvent) event).getLinked());
				} else if (event instanceof AfterLinkSaveEvent) {
					params.add(((AfterLinkSaveEvent) event).getLinked());
				}

				if (LOG.isDebugEnabled()) {
					LOG.debug("Invoking " + event.getClass().getSimpleName() + " handler for " + event.getSource());
				}
				handlerMethod.method.invoke(handlerMethod.handler, params.toArray());

			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
		final Class<?> beanType = bean.getClass();

		RepositoryEventHandler typeAnno = AnnotationUtils.findAnnotation(beanType, RepositoryEventHandler.class);
		if (null == typeAnno) {
			return bean;
		}

		Class<?>[] targetTypes = typeAnno.value();
		if (targetTypes.length == 0) {
			targetTypes = new Class<?>[] { null };
		}

		for (final Class<?> targetType : targetTypes) {
			ReflectionUtils.doWithMethods(beanType, new ReflectionUtils.MethodCallback() {
				@Override
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					inspect(targetType, bean, method, HandleBeforeCreate.class, BeforeCreateEvent.class);
					inspect(targetType, bean, method, HandleAfterCreate.class, AfterCreateEvent.class);
					inspect(targetType, bean, method, HandleBeforeSave.class, BeforeSaveEvent.class);
					inspect(targetType, bean, method, HandleAfterSave.class, AfterSaveEvent.class);
					inspect(targetType, bean, method, HandleBeforeLinkSave.class, BeforeLinkSaveEvent.class);
					inspect(targetType, bean, method, HandleAfterLinkSave.class, AfterLinkSaveEvent.class);
					inspect(targetType, bean, method, HandleBeforeDelete.class, BeforeDeleteEvent.class);
					inspect(targetType, bean, method, HandleAfterDelete.class, AfterDeleteEvent.class);
					inspect(targetType, bean, method, HandleBeforeLinkDelete.class, BeforeLinkDeleteEvent.class);
					inspect(targetType, bean, method, HandleAfterLinkDelete.class, AfterLinkDeleteEvent.class);
				}
			}, new ReflectionUtils.MethodFilter() {
				@Override
				public boolean matches(Method method) {
					return (!method.isSynthetic() && !method.isBridge() && method.getDeclaringClass() != Object.class && !method
							.getName().contains("$"));
				}
			});
		}

		return bean;
	}

	private <T extends Annotation> void inspect(Class<?> targetType, Object handler, Method method, Class<T> annoType,
			Class<? extends RepositoryEvent> eventType) {
		T anno = method.getAnnotation(annoType);
		if (null != anno) {
			try {
				Class<?>[] targetTypes;
				if (null == targetType) {
					targetTypes = (Class<?>[]) anno.getClass().getMethod("value", new Class[0]).invoke(anno);
				} else {
					targetTypes = new Class<?>[] { targetType };
				}
				for (Class<?> type : targetTypes) {
					EventHandlerMethod m = new EventHandlerMethod(type, handler, method);
					if (LOG.isDebugEnabled()) {
						LOG.debug("Annotated handler method found: " + m);
					}
					handlerMethods.add(eventType, m);
				}
			} catch (NoSuchMethodException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(e.getMessage(), e);
				}
			} catch (InvocationTargetException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(e.getMessage(), e);
				}
			} catch (IllegalAccessException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(e.getMessage(), e);
				}
			}
		}
	}

	private class EventHandlerMethod {
		final Class<?> targetType;
		final Method method;
		final Object handler;

		private EventHandlerMethod(Class<?> targetType, Object handler, Method method) {
			this.targetType = targetType;
			this.method = method;
			this.handler = handler;
		}

		@Override
		public String toString() {
			return "EventHandlerMethod{" + "targetType=" + targetType + ", method=" + method + ", handler=" + handler + '}';
		}
	}

}
