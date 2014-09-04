/*
 * Copyright 2014 the original author or authors.
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
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Special {@link RequestMappingHandlerMapping} that uses the base URI configured in the
 * {@link RepositoryRestConfiguration}, strips it from incoming requests in case they start with it and hands the
 * altered URI to the superclass for normal handler method lookup.
 * 
 * @author Oliver Gierke
 */
public class BaseUriAwareHandlerMapping extends RequestMappingHandlerMapping {

	private final RepositoryRestConfiguration configuration;

	/**
	 * Creates a new {@link BaseUriAwareHandlerMapping} using the given {@link RepositoryRestConfiguration}.
	 * 
	 * @param configuration must not be {@literal null}.
	 */
	public BaseUriAwareHandlerMapping(RepositoryRestConfiguration configuration) {

		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");
		this.configuration = configuration;
		setOrder(Ordered.LOWEST_PRECEDENCE - 150);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod(java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {

		String acceptType = request.getHeader("Accept");

		if (null == acceptType) {
			acceptType = configuration.getDefaultMediaType().toString();
		}

		List<MediaType> acceptHeaderTypes = MediaType.parseMediaTypes(acceptType);
		List<MediaType> acceptableTypes = new ArrayList<MediaType>();

		for (MediaType mt : acceptHeaderTypes) {
			if ("*".equals(mt.getType()) && "*".equals(mt.getSubtype()) || "application".equals(mt.getType())
					&& "*".equals(mt.getSubtype())) {
				mt = configuration.getDefaultMediaType();
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
			acceptType = configuration.getDefaultMediaType().toString();
		}

		String uri = new BaseUri(configuration.getBaseUri()).getRepositoryLookupPath(lookupPath);

		if (uri == null) {
			return null;
		}

		uri = StringUtils.hasText(uri) ? uri : "/";

		HttpServletRequest wrapper = new DefaultAcceptTypeHttpServletRequest(request, acceptType, uri);

		return supportsLookupPath(uri) ? super.lookupHandlerMethod(uri, wrapper) : null;
	}

	protected boolean supportsLookupPath(String lookupPath) {
		return true;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler(java.lang.Class)
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return beanType.getAnnotation(BaseUriAwareController.class) != null;
	}

	private static class DefaultAcceptTypeHttpServletRequest extends HttpServletRequestWrapper {

		private final String defaultAcceptType;
		private final String requestUri;

		private DefaultAcceptTypeHttpServletRequest(HttpServletRequest request, String defaultAcceptType) {
			this(request, defaultAcceptType, null);
		}

		private DefaultAcceptTypeHttpServletRequest(HttpServletRequest request, String defaultAcceptType, String requestUri) {
			super(request);
			this.defaultAcceptType = defaultAcceptType;
			this.requestUri = requestUri;
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequestWrapper#getHeader(java.lang.String)
		 */
		@Override
		public String getHeader(String name) {

			if ("accept".equals(name.toLowerCase())) {
				return defaultAcceptType;
			} else {
				return super.getHeader(name);
			}
		}

		/* 
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequestWrapper#getRequestURI()
		 */
		@Override
		public String getRequestURI() {
			return requestUri != null ? requestUri : super.getRequestURI();
		}

		/* 
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequestWrapper#getServletPath()
		 */
		@Override
		public String getServletPath() {
			return requestUri != null ? requestUri : super.getServletPath();
		}
	}
}
