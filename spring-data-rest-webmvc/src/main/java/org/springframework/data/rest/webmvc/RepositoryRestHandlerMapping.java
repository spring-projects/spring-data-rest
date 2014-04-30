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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

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

		String uri = extractRepositoryLookupPath(lookupPath, config.getBaseUri());

		if (uri == null) {
			return null;
		}

		HttpServletRequest request = new DefaultAcceptTypeHttpServletRequest(origRequest, acceptType, uri);

		// Root request
		if (!StringUtils.hasText(uri) || uri.equals("/")) {
			return super.lookupHandlerMethod("/", request);
		}

		String[] parts = uri.split("/");

		if (mappings.exportsTopLevelResourceFor(parts[uri.startsWith("/") ? 1 : 0])) {
			return super.lookupHandlerMethod(uri, request);
		}

		return null;
	}

	/**
	 * Extracts the actual lookup path within the Spring Data REST managed URI space. This includes stripping the
	 * necessary parts of the base URI from the source lookup path.
	 * 
	 * @param lookupPath must not be {@literal null}.
	 * @param baseUri must not be {@literal null}.
	 * @return the stripped lookup path with then the repository URI space or {@literal null} in case the lookup path is
	 *         not pointing into the repository URI space.
	 */
	private static String extractRepositoryLookupPath(String lookupPath, URI baseUri) {

		Assert.notNull(lookupPath, "Lookup path must not be null!");
		Assert.notNull(baseUri, "Base URI must not be null!");

		lookupPath = StringUtils.trimTrailingCharacter(lookupPath, '/');

		if (!baseUri.isAbsolute()) {

			String uri = baseUri.toString();

			if (!StringUtils.hasText(uri)) {
				return lookupPath;
			}

			uri = uri.startsWith("/") ? uri : "/".concat(uri);
			return lookupPath.startsWith(uri) ? lookupPath.substring(uri.length(), lookupPath.length()) : null;
		}

		List<String> baseUriSegments = UriComponentsBuilder.fromUri(baseUri).build().getPathSegments();
		Collections.reverse(baseUriSegments);
		String tail = "";

		for (String tailSegment : baseUriSegments) {

			tail = "/".concat(tailSegment).concat(tail);

			if (lookupPath.startsWith(tail)) {
				return lookupPath.substring(tail.length(), lookupPath.length());
			}
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
		private final String requestUri;

		private DefaultAcceptTypeHttpServletRequest(HttpServletRequest request, String defaultAcceptType) {
			this(request, defaultAcceptType, null);
		}

		private DefaultAcceptTypeHttpServletRequest(HttpServletRequest request, String defaultAcceptType, String requestUri) {
			super(request);
			this.defaultAcceptType = defaultAcceptType;
			this.requestUri = requestUri;
		}

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
