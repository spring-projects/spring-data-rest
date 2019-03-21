/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link RequestMappingHandlerMapping} implementation that will only find a handler method if a
 * {@link org.springframework.data.repository.Repository} is exported under that URL path segment. Also ensures the
 * {@link OpenEntityManagerInViewInterceptor} is registered in the application context. The OEMIVI is required for the
 * REST exporter to function properly.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class RepositoryRestHandlerMapping extends BasePathAwareHandlerMapping {

	private static final MediaType EVERYTHING_JSON_MEDIA_TYPE = new MediaType("application", "*+json",
			AbstractJackson2HttpMessageConverter.DEFAULT_CHARSET);

	private final ResourceMappings mappings;
	private final RepositoryRestConfiguration configuration;
	private final Repositories repositories;

	private StringValueResolver embeddedValueResolver;
	private JpaHelper jpaHelper;

	/**
	 * Creates a new {@link RepositoryRestHandlerMapping} for the given {@link ResourceMappings} and
	 * {@link RepositoryRestConfiguration}.
	 * 
	 * @param mappings must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 */
	public RepositoryRestHandlerMapping(ResourceMappings mappings, RepositoryRestConfiguration config) {
		this(mappings, config, null);
	}

	/**
	 * Creates a new {@link RepositoryRestHandlerMapping} for the given {@link ResourceMappings}
	 * {@link RepositoryRestConfiguration} and {@link Repositories}.
	 *
	 * @param mappings must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 * @param repositories can be {@literal null} if {@link CrossOrigin} resolution is not required.
	 */
	public RepositoryRestHandlerMapping(ResourceMappings mappings, RepositoryRestConfiguration config,
			Repositories repositories) {

		super(config);

		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");

		this.mappings = mappings;
		this.configuration = config;
		this.repositories = repositories;
	}

	/**
	 * @param jpaHelper the jpaHelper to set
	 */
	public void setJpaHelper(JpaHelper jpaHelper) {
		this.jpaHelper = jpaHelper;
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#setEmbeddedValueResolver(org.springframework.util.StringValueResolver)
	 */
	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {

		embeddedValueResolver = resolver;
		super.setEmbeddedValueResolver(resolver);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod(java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {

		HandlerMethod handlerMethod = super.lookupHandlerMethod(lookupPath, request);

		if (handlerMethod == null) {
			return null;
		}

		String repositoryLookupPath = new BaseUri(configuration.getBaseUri()).getRepositoryLookupPath(lookupPath);

		// Repository root resource
		if (!StringUtils.hasText(repositoryLookupPath)) {
			return handlerMethod;
		}

		return mappings.exportsTopLevelResourceFor(getRepositoryBasePath(repositoryLookupPath)) ? handlerMethod : null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleNoMatch(java.util.Set, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos, String lookupPath,
			HttpServletRequest request) throws ServletException {
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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.BasePathAwareHandlerMapping#process(org.springframework.web.servlet.mvc.condition.ProducesRequestCondition)
	 */
	@Override
	protected ProducesRequestCondition customize(ProducesRequestCondition condition) {

		if (!condition.isEmpty()) {
			return condition;
		}

		HashSet<String> mediaTypes = new LinkedHashSet<String>();
		mediaTypes.add(configuration.getDefaultMediaType().toString());
		mediaTypes.add(MediaType.APPLICATION_JSON_VALUE);
		mediaTypes.add(EVERYTHING_JSON_MEDIA_TYPE.toString());

		return new ProducesRequestCondition(mediaTypes.toArray(new String[mediaTypes.size()]));
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getCorsConfiguration(java.lang.Object, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {

		CorsConfiguration corsConfiguration = super.getCorsConfiguration(handler, request);
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);

		String repositoryLookupPath = new BaseUri(configuration.getBaseUri()).getRepositoryLookupPath(lookupPath);

		if (!StringUtils.hasText(repositoryLookupPath) || repositories == null) {
			return corsConfiguration;
		}

		// Repository root resource
		CorsConfiguration repositoryConfiguration = new CorsConfigurationAccessor(mappings, repositories,
				embeddedValueResolver).findCorsConfiguration(lookupPath);

		if (repositoryConfiguration != null) {
			return corsConfiguration != null ? corsConfiguration.combine(repositoryConfiguration) : repositoryConfiguration;
		}

		return corsConfiguration;
	}

	/**
	 * Returns the first segment of the given repository lookup path.
	 * 
	 * @param repositoryLookupPath must not be {@literal null}.
	 * @return
	 */
	private static String getRepositoryBasePath(String repositoryLookupPath) {

		int secondSlashIndex = repositoryLookupPath.indexOf('/', repositoryLookupPath.startsWith("/") ? 1 : 0);
		return secondSlashIndex == -1 ? repositoryLookupPath : repositoryLookupPath.substring(0, secondSlashIndex);
	}

	/**
	 * Accessor to obtain {@link CorsConfiguration} for exposed repositories.
	 * <p>
	 * Exported Repository classes can be annotated with {@link CrossOrigin} to configure CORS for a specific repository.
	 *
	 * @author Mark Paluch
	 * @since 2.6
	 */
	static class CorsConfigurationAccessor {

		private final ResourceMappings mappings;
		private final Repositories repositories;
		private final StringValueResolver embeddedValueResolver;

		/**
		 * Creates a new {@link CorsConfigurationAccessor} given {@link ResourceMappings}, {@link Repositories} and
		 * {@link StringValueResolver}.
		 *
		 * @param mappings must not be {@literal null}.
		 * @param repositories must not be {@literal null}.
		 * @param embeddedValueResolver may be {@literal null} if not present.
		 */
		CorsConfigurationAccessor(ResourceMappings mappings, Repositories repositories,
				StringValueResolver embeddedValueResolver) {

			Assert.notNull(mappings, "ResourceMappings must not be null!");
			Assert.notNull(repositories, "Repositories must not be null!");

			this.mappings = mappings;
			this.repositories = repositories;
			this.embeddedValueResolver = embeddedValueResolver;
		}

		CorsConfiguration findCorsConfiguration(String lookupPath) {

			ResourceMetadata resource = getResourceMetadata(getRepositoryBasePath(lookupPath));

			return resource != null ? createConfiguration(
					repositories.getRepositoryInformationFor(resource.getDomainType()).getRepositoryInterface()) : null;
		}

		private ResourceMetadata getResourceMetadata(String basePath) {

			if (mappings.exportsTopLevelResourceFor(basePath)) {

				for (ResourceMetadata metadata : mappings) {
					if (metadata.getPath().matches(basePath) && metadata.isExported()) {
						return metadata;
					}
				}
			}

			return null;
		}

		/**
		 * Creates {@link CorsConfiguration} from a repository interface.
		 *
		 * @param repositoryInterface the repository interface
		 * @return {@link CorsConfiguration} or {@literal null}.
		 */
		protected CorsConfiguration createConfiguration(Class<?> repositoryInterface) {

			CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(repositoryInterface, CrossOrigin.class);

			if (typeAnnotation == null) {
				return null;
			}

			CorsConfiguration config = new CorsConfiguration();
			updateCorsConfig(config, typeAnnotation);

			if (CollectionUtils.isEmpty(config.getAllowedOrigins())) {
				config.setAllowedOrigins(Arrays.asList(CrossOrigin.DEFAULT_ORIGINS));
			}
			
			if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
				for (HttpMethod httpMethod : HttpMethod.values()) {
					config.addAllowedMethod(httpMethod);
				}
			}

			if (CollectionUtils.isEmpty(config.getAllowedHeaders())) {
				config.setAllowedHeaders(Arrays.asList(CrossOrigin.DEFAULT_ALLOWED_HEADERS));
			}

			if (config.getAllowCredentials() == null) {
				config.setAllowCredentials(CrossOrigin.DEFAULT_ALLOW_CREDENTIALS);
			}

			if (config.getMaxAge() == null) {
				config.setMaxAge(CrossOrigin.DEFAULT_MAX_AGE);
			}

			return config;
		}

		private void updateCorsConfig(CorsConfiguration config, CrossOrigin annotation) {

			for (String origin : annotation.origins()) {
				config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
			}

			for (RequestMethod method : annotation.methods()) {
				config.addAllowedMethod(method.name());
			}

			for (String header : annotation.allowedHeaders()) {
				config.addAllowedHeader(resolveCorsAnnotationValue(header));
			}

			for (String header : annotation.exposedHeaders()) {
				config.addExposedHeader(resolveCorsAnnotationValue(header));
			}

			String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());

			if ("true".equalsIgnoreCase(allowCredentials)) {
				config.setAllowCredentials(true);
			} else if ("false".equalsIgnoreCase(allowCredentials)) {
				config.setAllowCredentials(false);
			} else if (!allowCredentials.isEmpty()) {
				throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", "
						+ "or an empty string (\"\"): current value is [" + allowCredentials + "]");
			}

			if (annotation.maxAge() >= 0 && config.getMaxAge() == null) {
				config.setMaxAge(annotation.maxAge());
			}
		}

		private String resolveCorsAnnotationValue(String value) {
			return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
		}
	}
}
