/*
 * Copyright 2012-2021 the original author or authors.
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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.HttpMethods;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.data.util.Streamable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

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

	public static final HttpMethods DEFAULT_ALLOWED_METHODS = HttpMethods.none()
			.and(HttpMethod.values())
			.butWithout(HttpMethod.TRACE);

	private static final PathPatternParser PARSER = new PathPatternParser();
	static final String EFFECTIVE_LOOKUP_PATH_ATTRIBUTE = RepositoryRestHandlerMapping.class.getName()
			+ ".EFFECTIVE_REPOSITORY_RESOURCE_LOOKUP_PATH";

	private final ResourceMappings mappings;
	private final RepositoryRestConfiguration configuration;
	private final Optional<Repositories> repositories;

	private RepositoryCorsConfigurationAccessor corsConfigurationAccessor;
	private Optional<JpaHelper> jpaHelper = Optional.empty();

	/**
	 * Creates a new {@link RepositoryRestHandlerMapping} for the given {@link ResourceMappings} and
	 * {@link RepositoryRestConfiguration}.
	 *
	 * @param mappings must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 */
	public RepositoryRestHandlerMapping(ResourceMappings mappings, RepositoryRestConfiguration config) {
		this(mappings, config, Optional.empty());
	}

	/**
	 * Creates a new {@link RepositoryRestHandlerMapping} for the given {@link ResourceMappings}
	 * {@link RepositoryRestConfiguration} and {@link Repositories}.
	 *
	 * @param mappings must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 * @param repositories must not be {@literal null}.
	 */
	public RepositoryRestHandlerMapping(ResourceMappings mappings, RepositoryRestConfiguration config,
			Repositories repositories) {

		this(mappings, config, Optional.of(repositories));
	}

	private RepositoryRestHandlerMapping(ResourceMappings mappings, RepositoryRestConfiguration config,
			Optional<Repositories> repositories) {

		super(config);

		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");
		Assert.notNull(repositories, "Repositories must not be null!");

		this.mappings = mappings;
		this.configuration = config;
		this.repositories = repositories;
		this.corsConfigurationAccessor = new RepositoryCorsConfigurationAccessor(mappings, NoOpStringValueResolver.INSTANCE,
				repositories);
	}

	/**
	 * @param jpaHelper the jpaHelper to set
	 */
	public void setJpaHelper(JpaHelper jpaHelper) {
		this.jpaHelper = Optional.ofNullable(jpaHelper);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#setEmbeddedValueResolver(org.springframework.util.StringValueResolver)
	 */
	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {

		super.setEmbeddedValueResolver(resolver);

		this.corsConfigurationAccessor = new RepositoryCorsConfigurationAccessor(mappings,
				resolver == null ? NoOpStringValueResolver.INSTANCE : resolver, repositories);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod(java.lang.String, jakarta.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {

		HandlerMethod handlerMethod = super.lookupHandlerMethod(lookupPath, request);

		if (handlerMethod == null) {
			return null;
		}

		String repositoryLookupPath = new BaseUri(configuration.getBasePath()).getRepositoryLookupPath(lookupPath);

		// Repository root resource
		if (!StringUtils.hasText(repositoryLookupPath)) {
			return handlerMethod;
		}

		String repositoryBasePath = getRepositoryBasePath(repositoryLookupPath);

		if (!mappings.exportsTopLevelResourceFor(repositoryBasePath)) {
			return null;
		}

		exposeEffectiveLookupPathKey(handlerMethod, request, repositoryBasePath);

		return handlerMethod;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleNoMatch(java.util.Set, java.lang.String, jakarta.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos, String lookupPath,
			HttpServletRequest request) throws ServletException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.BasePathAwareHandlerMapping#isHandlerInternal(java.lang.Class)
	 */
	@Override
	protected boolean isHandlerInternal(Class<?> type) {
		return AnnotatedElementUtils.hasAnnotation(type, RepositoryRestController.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#extendInterceptors(java.util.List)
	 */
	@Override
	protected void extendInterceptors(List<Object> interceptors) {

		jpaHelper.map(JpaHelper::getInterceptors) //
				.orElseGet(() -> Collections.emptyList()) //
				.forEach(interceptors::add);
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

		Set<String> mediaTypes = new LinkedHashSet<String>();
		mediaTypes.add(configuration.getDefaultMediaType().toString());
		mediaTypes.add(MediaType.APPLICATION_JSON_VALUE);
		mediaTypes.add(MediaTypes.HAL_FORMS_JSON_VALUE);

		return new ProducesRequestCondition(mediaTypes.toArray(new String[mediaTypes.size()]));
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getCorsConfiguration(java.lang.Object, jakarta.servlet.http.HttpServletRequest)
	 */
	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {

		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		String repositoryLookupPath = new BaseUri(configuration.getBasePath()).getRepositoryLookupPath(lookupPath);
		CorsConfiguration corsConfiguration = super.getCorsConfiguration(handler, request);

		return repositories.filter(it -> StringUtils.hasText(repositoryLookupPath))//
				.flatMap(it -> corsConfigurationAccessor.findCorsConfiguration(repositoryLookupPath))
				.map(it -> it.combine(corsConfiguration))//
				.orElse(corsConfiguration);
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
	 * Exposes the effective repository resource lookup path as request attribute via
	 * {@link #EFFECTIVE_LOOKUP_PATH_ATTRIBUTE}, i.e. {@code /people/search/\{search\}} instead of
	 * {@code /\{repository\}/search/\{search\}}.
	 *
	 * @param method must not be {@literal null}.
	 * @param request must not be {@literal null}.
	 * @param repositoryBasePath must not be {@literal null}.
	 */
	private void exposeEffectiveLookupPathKey(HandlerMethod method, HttpServletRequest request,
			String repositoryBasePath) {

		RequestMappingInfo mappingInfo = getMappingForMethod(method.getMethod(), method.getBeanType());

		if (mappingInfo == null) {
			return;
		}

		String pattern = getPattern(mappingInfo, request);

		PathPatternParser parser = getPatternParser();
		parser = parser != null ? parser : PARSER;

		request.setAttribute(EFFECTIVE_LOOKUP_PATH_ATTRIBUTE,
				parser.parse(pattern.replace("/{repository}", repositoryBasePath)));
	}

	private static String getPattern(RequestMappingInfo info, HttpServletRequest request) {

		PathPatternsRequestCondition pathPatternsCondition = info.getPathPatternsCondition();

		if (pathPatternsCondition != null) {
			return pathPatternsCondition
					.getMatchingCondition(request)
					.getFirstPattern().getPatternString();
		}

		return info.getPatternsCondition() //
				.getMatchingCondition(request)//
				.getPatterns()
				.iterator().next();
	}

	/**
	 * No-op {@link StringValueResolver} that returns the given {@link String} value as is.
	 *
	 * @author Oliver Gierke
	 * @since 2.6
	 */
	enum NoOpStringValueResolver implements StringValueResolver {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.util.StringValueResolver#resolveStringValue(java.lang.String)
		 */
		@Override
		public String resolveStringValue(String value) {
			return value;
		}
	}

	/**
	 * Accessor to obtain {@link CorsConfiguration} for exposed repositories.
	 * <p>
	 * Exported repository classes can be annotated with {@link CrossOrigin} to configure CORS for a specific repository.
	 *
	 * @author Mark Paluch
	 * @author Oliver Gierke
	 * @since 2.6
	 */
	static class RepositoryCorsConfigurationAccessor {

		private final ResourceMappings mappings;
		private final StringValueResolver embeddedValueResolver;
		private final Optional<Repositories> repositories;

		public RepositoryCorsConfigurationAccessor(ResourceMappings mappings, StringValueResolver embeddedValueResolver,
				Optional<Repositories> repositories) {

			Assert.notNull(mappings, "ResourceMappings must not be null!");
			Assert.notNull(embeddedValueResolver, "StringValueResolver must not be null!");
			Assert.notNull(repositories, "Repositories must not be null!");

			this.mappings = mappings;
			this.embeddedValueResolver = embeddedValueResolver;
			this.repositories = repositories;
		}

		Optional<CorsConfiguration> findCorsConfiguration(String lookupPath) {

			return getResourceMetadata(getRepositoryBasePath(lookupPath))//
					.flatMap(it -> repositories.flatMap(foo -> foo.getRepositoryInformationFor(it.getDomainType())))//
					.map(it -> it.getRepositoryInterface())//
					.map(it -> createConfiguration(it));
		}

		private Optional<ResourceMetadata> getResourceMetadata(String basePath) {

			if (!mappings.exportsTopLevelResourceFor(basePath)) {
				return Optional.empty();
			}

			return mappings.stream()//
					.filter(it -> it.getPath().matches(basePath) && it.isExported())//
					.findFirst();
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
			config.applyPermitDefaultValues();

			return config;
		}

		private void updateCorsConfig(CorsConfiguration config, CrossOrigin annotation) {

			for (String origin : annotation.origins()) {
				config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
			}

			for (HttpMethod method : getAllowedMethods(annotation)) {
				config.addAllowedMethod(method.name());
			}

			for (String header : annotation.allowedHeaders()) {
				config.addAllowedHeader(resolveCorsAnnotationValue(header));
			}

			for (String header : annotation.exposedHeaders()) {
				config.addExposedHeader(resolveCorsAnnotationValue(header));
			}

			for (String originPattern : annotation.originPatterns()) {
				config.addAllowedOriginPattern(originPattern);
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
			return this.embeddedValueResolver.resolveStringValue(value);
		}

		/**
		 * Returns the {@link HttpMethods} configured on the given annotation or the default methods to support.
		 *
		 * @param annotation must not be {@literal null}.
		 * @return
		 * @see #DEFAULT_ALLOWED_METHODS
		 */
		private static HttpMethods getAllowedMethods(CrossOrigin annotation) {

			RequestMethod[] methods = annotation.methods();

			return methods.length == 0
					? DEFAULT_ALLOWED_METHODS
					: HttpMethods.of(Streamable.of(methods)
							.map(RequestMethod::name)
							.map(HttpMethod::resolve)
							.toList());
		}
	}
}
