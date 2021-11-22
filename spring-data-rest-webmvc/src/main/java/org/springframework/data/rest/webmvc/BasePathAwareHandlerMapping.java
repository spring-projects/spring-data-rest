/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.util.ProxyUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.springframework.core.annotation.AnnotatedElementUtils.*;

/**
 * A {@link RequestMappingHandlerMapping} that augments the request mappings
 *
 * @author Oliver Gierke
 */
public class BasePathAwareHandlerMapping extends RequestMappingHandlerMapping {

	private static final String AT_REQUEST_MAPPING_ON_TYPE = "Spring Data REST controller %s must not use @RequestMapping on class level as this would cause double registration with Spring MVC!";
	private final RepositoryRestConfiguration configuration;

	private final String baseUri;

	/**
	 * Creates a new {@link BasePathAwareHandlerMapping} using the given {@link RepositoryRestConfiguration}.
	 *
	 * @param configuration must not be {@literal null}.
	 */
	public BasePathAwareHandlerMapping(RepositoryRestConfiguration configuration) {

		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");

		this.configuration = configuration;
		this.baseUri = configuration.getBasePath().toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod(java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {

		List<MediaType> mediaTypes = new ArrayList<MediaType>();
		boolean defaultFound = false;

		for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader(HttpHeaders.ACCEPT))) {

			MediaType rawtype = mediaType.removeQualityValue();

			if (rawtype.equals(configuration.getDefaultMediaType())) {
				defaultFound = true;
			}

			if (!rawtype.equals(MediaType.ALL)) {
				mediaTypes.add(mediaType);
			}
		}

		if (!defaultFound) {
			mediaTypes.add(configuration.getDefaultMediaType());
		}

		return super.lookupHandlerMethod(lookupPath, new CustomAcceptHeaderHttpServletRequest(request, mediaTypes));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#hasCorsConfigurationSource(java.lang.Object)
	 */
	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#getMappingForMethod(java.lang.reflect.Method, java.lang.Class)
	 */
	@Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {

		RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

		if (info == null) {
			return null;
		}

		ProducesRequestCondition producesCondition = customize(info.getProducesCondition());
		Set<MediaType> mediaTypes = producesCondition.getProducibleMediaTypes();

		BasePathAwareController mergedAnnotation = findMergedAnnotation(handlerType, BasePathAwareController.class);
		if (mergedAnnotation != null) {
			info = appendPathPrefix(info, mergedAnnotation.value());
		}
		info = appendPathPrefix(info, new String[]{this.baseUri});
		return info.mutate()
				.produces(mediaTypes.stream().map(MediaType::toString).toArray(String[]::new))
				.build();
	}

	private RequestMappingInfo appendPathPrefix(RequestMappingInfo info, String[] pathPrefix) {
		if (pathPrefix.length > 0) {
			String[] paths = this.resolveEmbeddedValuesInPatterns(pathPrefix);
			return info.mutate().paths(paths).build().combine(info);
		}
		return info;
	}

	/**
	 * Customize the given {@link ProducesRequestCondition}. Default implementation returns the condition as is.
	 *
	 * @param condition will never be {@literal null}.
	 * @return
	 */
	protected ProducesRequestCondition customize(ProducesRequestCondition condition) {
		return condition;
	}

	/**
	 * Returns whether the given type is considered a handler. Performs additional configuration checks. If you only want
	 * to customize the handler selection criteria, override {@link #isHandlerInternal(Class)}. Will be made final in 4.0.
	 *
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler(java.lang.Class)
	 * @deprecated for overriding in 3.6. Will be made final in 4.0.
	 */
	@Override
	@Deprecated
	protected boolean isHandler(Class<?> beanType) {

		Class<?> type = ProxyUtils.getUserClass(beanType);
		boolean isSpringDataRestHandler = isHandlerInternal(type);

		if (!isSpringDataRestHandler) {
			return false;
		}

		if (AnnotatedElementUtils.hasAnnotation(type, RequestMapping.class)) {
			throw new IllegalStateException(String.format(AT_REQUEST_MAPPING_ON_TYPE, beanType.getName()));
		}

		return isSpringDataRestHandler;
	}

	/**
	 * Returns whether the given controller type is considered a handler.
	 *
	 * @param type will never be {@literal null}.
	 * @return
	 */
	protected boolean isHandlerInternal(Class<?> type) {
		return type.isAnnotationPresent(BasePathAwareController.class);
	}

	/**
	 * {@link HttpServletRequest} that exposes the given media types for the {@code Accept} header.
	 *
	 * @author Oliver Gierke
	 */
	static class CustomAcceptHeaderHttpServletRequest extends HttpServletRequestWrapper {

		private final List<MediaType> acceptMediaTypes;
		private final List<String> acceptMediaTypeStrings;

		/**
		 * Creates a new {@link CustomAcceptHeaderHttpServletRequest} for the given delegate {@link HttpServletRequest} and
		 * the list of {@link MediaType}.
		 *
		 * @param request must not be {@literal null}.
		 * @param acceptMediaTypes must not be {@literal null} or empty.
		 */
		public CustomAcceptHeaderHttpServletRequest(HttpServletRequest request, List<MediaType> acceptMediaTypes) {

			super(request);

			Assert.notEmpty(acceptMediaTypes, "MediaTypes must not be empty!");

			this.acceptMediaTypes = acceptMediaTypes;

			List<String> acceptMediaTypeStrings = new ArrayList<String>(acceptMediaTypes.size());

			for (MediaType mediaType : acceptMediaTypes) {
				acceptMediaTypeStrings.add(mediaType.toString());
			}

			this.acceptMediaTypeStrings = acceptMediaTypeStrings;
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequestWrapper#getHeader(java.lang.String)
		 */
		@Override
		public String getHeader(String name) {

			return HttpHeaders.ACCEPT.equalsIgnoreCase(name) && acceptMediaTypes != null //
					? StringUtils.collectionToCommaDelimitedString(acceptMediaTypes) //
					: super.getHeader(name);
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequestWrapper#getHeaders(java.lang.String)
		 */
		@Override
		public Enumeration<String> getHeaders(String name) {

			return HttpHeaders.ACCEPT.equalsIgnoreCase(name) && acceptMediaTypes != null //
					? Collections.enumeration(acceptMediaTypeStrings) //
					: super.getHeaders(name);
		}
	}
}
