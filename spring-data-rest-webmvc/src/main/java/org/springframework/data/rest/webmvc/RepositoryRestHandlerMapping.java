/*
 * Copyright 2012-2013 the original author or authors.
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

import static org.springframework.util.StringUtils.*;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link RequestMappingHandlerMapping} implementation that will only find a handler method if a
 * {@link org.springframework.data.repository.Repository} is exported under that URL path segment. Also ensures the
 * {@link OpenEntityManagerInViewInterceptor} is registered in the application context. The OEMIVI is required for the
 * REST exporter to function properly.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryRestHandlerMapping extends RequestMappingHandlerMapping {

	private final ResourceMappings mappings;
	private final RepositoryRestConfiguration config;

	private JpaHelper jpaHelper;

	/**
	 * Creates a new {@link RepositoryRestHandlerMapping} for the given {@link ResourceMappings} and
	 * {@link RepositoryRestConfiguration}.
	 * 
	 * @param mappings must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 */
	public RepositoryRestHandlerMapping(ResourceMappings mappings, RepositoryRestConfiguration config) {

		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");

		this.mappings = mappings;
		this.config = config;

		setOrder(Ordered.LOWEST_PRECEDENCE - 100);
	}

	/**
	 * @param jpaHelper the jpaHelper to set
	 */
	public void setJpaHelper(JpaHelper jpaHelper) {
		this.jpaHelper = jpaHelper;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod(java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest origRequest) throws Exception {

		String acceptType = origRequest.getHeader("Accept");

		if (null == acceptType) {
			acceptType = config.getDefaultMediaType().toString();
		}

		List<MediaType> acceptHeaderTypes = MediaType.parseMediaTypes(acceptType);
		List<MediaType> acceptableTypes = new ArrayList<MediaType>();

		for (MediaType mt : acceptHeaderTypes) {
			if ("*".equals(mt.getType()) && "*".equals(mt.getSubtype()) || "application".equals(mt.getType())
					&& "*".equals(mt.getSubtype())) {
				mt = config.getDefaultMediaType();
			}
			if (!acceptableTypes.contains(mt)) {
				acceptableTypes.add(mt);
			}
		}

		if (acceptableTypes.size() > 1) {
			acceptType = collectionToDelimitedString(acceptableTypes, ",");
		} else if (acceptableTypes.size() == 1) {
			acceptType = acceptableTypes.get(0).toString();
		} else {
			acceptType = config.getDefaultMediaType().toString();
		}

		HttpServletRequest request = new DefaultAcceptTypeHttpServletRequest(origRequest, acceptType);

		String requestUri = lookupPath;
		if (requestUri.startsWith("/")) {
			requestUri = requestUri.substring(1);
		}

		if (!hasText(requestUri)) {
			return super.lookupHandlerMethod(lookupPath, request);
		}

		String[] parts = requestUri.split("/");

		if (parts.length == 0) {
			// Root request
			return super.lookupHandlerMethod(lookupPath, request);
		}

		if (mappings.exportsTopLevelResourceFor(parts[0])) {
			return super.lookupHandlerMethod(lookupPath, request);
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler(java.lang.Class)
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotationUtils.findAnnotation(beanType, RepositoryRestController.class) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#extendInterceptors(java.util.List)
	 */
	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		if (null != jpaHelper) {
			for (Object o : jpaHelper.getInterceptors()) {
				interceptors.add(o);
			}
		}
	}

	private static class DefaultAcceptTypeHttpServletRequest extends HttpServletRequestWrapper {

		private final String defaultAcceptType;

		private DefaultAcceptTypeHttpServletRequest(HttpServletRequest request, String defaultAcceptType) {
			super(request);
			this.defaultAcceptType = defaultAcceptType;
		}

		@Override
		public String getHeader(String name) {

			if ("accept".equals(name.toLowerCase())) {
				return defaultAcceptType;
			} else {
				return super.getHeader(name);
			}
		}
	}
}
