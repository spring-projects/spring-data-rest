/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * {@link RequestMappingHandlerAdapter} implementation that adds a couple argument resolvers for controller method
 * parameters used in the REST exporter controller. Also only looks for handler methods in the Spring Data REST provided
 * controller classes to help isolate this handler adapter from other handler adapters the user might have configured in
 * their Spring MVC context.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryRestHandlerAdapter
		extends org.springframework.hateoas.mvc.ResourceProcessorInvokingHandlerAdapter {

	private final List<HandlerMethodArgumentResolver> argumentResolvers;

	/**
	 * Creates a new {@link RepositoryRestHandlerAdapter} using the given {@link HandlerMethodArgumentResolver} and
	 * {@link org.springframework.hateoas.mvc.ResourceProcessorInvoker}.
	 *
	 * @param argumentResolvers must not be {@literal null}.
	 * @param invoker must not be {@literal null}.
	 */
	public RepositoryRestHandlerAdapter(List<HandlerMethodArgumentResolver> argumentResolvers,
			org.springframework.hateoas.mvc.ResourceProcessorInvoker invoker) {

		super(invoker);
		this.argumentResolvers = argumentResolvers;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.ResourceProcessorInvokingHandlerAdapter#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		setCustomArgumentResolvers(argumentResolvers);
		super.afterPropertiesSet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter#getOrder()
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#supportsInternal(org.springframework.web.method.HandlerMethod)
	 */
	@Override
	protected boolean supportsInternal(HandlerMethod handlerMethod) {

		Class<?> controllerType = handlerMethod.getBeanType();

		return AnnotationUtils.findAnnotation(controllerType, BasePathAwareController.class) != null;
	}
}
