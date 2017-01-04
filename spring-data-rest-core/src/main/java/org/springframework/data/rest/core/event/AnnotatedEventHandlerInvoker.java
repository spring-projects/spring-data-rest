/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
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
import org.springframework.data.rest.core.util.Methods;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * Component to discover annotated repository event handlers and trigger them on {@link ApplicationEvent}s.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class AnnotatedEventHandlerInvoker implements ApplicationListener<RepositoryEvent>, BeanPostProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(AnnotatedEventHandlerInvoker.class);
	private static final String PARAMETER_MISSING = "Invalid event handler method %s! At least a single argument is required to determine the domain type for which you are interested in events.";

	private final MultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod> handlerMethods = new LinkedMultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod>();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(RepositoryEvent event) {

		Class<? extends RepositoryEvent> eventType = event.getClass();

		if (!handlerMethods.containsKey(eventType)) {
			return;
		}

		for (EventHandlerMethod handlerMethod : handlerMethods.get(eventType)) {

			Object src = event.getSource();

			if (!ClassUtils.isAssignable(handlerMethod.targetType, src.getClass())) {
				continue;
			}

			List<Object> parameters = new ArrayList<Object>();
			parameters.add(src);

			if (event instanceof LinkedEntityEvent) {
				parameters.add(((LinkedEntityEvent) event).getLinked());
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("Invoking {} handler for {}.", event.getClass().getSimpleName(), event.getSource());
			}

			ReflectionUtils.invokeMethod(handlerMethod.method, handlerMethod.handler, parameters.toArray());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {

		Class<?> beanType = ClassUtils.getUserClass(bean);
		RepositoryEventHandler typeAnno = AnnotationUtils.findAnnotation(beanType, RepositoryEventHandler.class);

		if (typeAnno == null) {
			return bean;
		}

		ReflectionUtils.doWithMethods(beanType, new ReflectionUtils.MethodCallback() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.util.ReflectionUtils.MethodCallback#doWith(java.lang.reflect.Method)
			 */
			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

				inspect(bean, method, HandleBeforeCreate.class, BeforeCreateEvent.class);
				inspect(bean, method, HandleAfterCreate.class, AfterCreateEvent.class);
				inspect(bean, method, HandleBeforeSave.class, BeforeSaveEvent.class);
				inspect(bean, method, HandleAfterSave.class, AfterSaveEvent.class);
				inspect(bean, method, HandleBeforeLinkSave.class, BeforeLinkSaveEvent.class);
				inspect(bean, method, HandleAfterLinkSave.class, AfterLinkSaveEvent.class);
				inspect(bean, method, HandleBeforeDelete.class, BeforeDeleteEvent.class);
				inspect(bean, method, HandleAfterDelete.class, AfterDeleteEvent.class);
				inspect(bean, method, HandleBeforeLinkDelete.class, BeforeLinkDeleteEvent.class);
				inspect(bean, method, HandleAfterLinkDelete.class, AfterLinkDeleteEvent.class);
			}
		}, Methods.USER_METHODS);
		
		for(List<EventHandlerMethod> events : handlerMethods.values())
		    Collections.sort(events);
		
		return bean;
	}

	/**
	 * Inspects the given handler method for an annotation of the given type. If the annotation present an
	 * {@link EventHandlerMethod} is registered for the given {@link RepositoryEvent} type.
	 * 
	 * @param handler must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @param annotationType must not be {@literal null}.
	 * @param eventType must not be {@literal null}.
	 */
	private <T extends Annotation> void inspect(Object handler, Method method, Class<T> annotationType,
			Class<? extends RepositoryEvent> eventType) {

		T annotation = AnnotationUtils.findAnnotation(method, annotationType);

		if (annotation == null) {
			return;
		}

		Class<?>[] parameterTypes = method.getParameterTypes();

		if (parameterTypes.length == 0) {
			throw new IllegalStateException(String.format(PARAMETER_MISSING, method));
		}

		EventHandlerMethod handlerMethod = new EventHandlerMethod(parameterTypes[0], handler, method);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Annotated handler method found: {}", handlerMethod);
		}

		handlerMethods.add(eventType, handlerMethod);
	}

	static class EventHandlerMethod implements Comparable<EventHandlerMethod>{

		final Class<?> targetType;
		final Method method;
		final Object handler;
		private int order;

		private EventHandlerMethod(Class<?> targetType, Object handler, Method method) {

			this.targetType = targetType;
			this.method = method;
			this.handler = handler;
			
			order = Ordered.LOWEST_PRECEDENCE;
			if(method.isAnnotationPresent(Order.class)){
			    Order orderAnno = method.getAnnotation(Order.class);
			    order = orderAnno.value();
			}

			ReflectionUtils.makeAccessible(this.method);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("EventHandlerMethod{ targetType=%s, method=%s, handler=%s }", targetType, method, handler);
		}

        @Override
        public int compareTo(EventHandlerMethod o) {
            return this.order - o.order;
        }
	}
}
