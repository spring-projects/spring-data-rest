/*
 * Copyright 2024-present the original author or authors.
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

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.server.core.DummyInvocationUtils;
import org.springframework.hateoas.server.core.LastInvocationAware;
import org.springframework.hateoas.server.core.MethodInvocation;
import org.springframework.hateoas.server.core.WebHandler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilderFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A {@link WebMvcLinkBuilderFactory} that is aware of the Spring Data REST {@code basePath} configuration. When
 * building links via {@link WebMvcLinkBuilder#linkTo(Object)} for controllers annotated with
 * {@link BasePathAwareController} (or {@link RepositoryRestController}), this factory automatically prepends the
 * configured {@code basePath} to the generated URI, ensuring that links are consistent with the actual URL space
 * managed by Spring Data REST.
 * <p>
 * This fixes the issue where {@code WebMvcLinkBuilder.linkTo(methodOn(MyRepositoryRestController.class).someMethod())}
 * would produce a URL without the configured {@code basePath}.
 *
 * @author Steve Rutherford
 * @see BasePathAwareController
 * @see RepositoryRestController
 * @see RepositoryRestConfiguration#getBasePath()
 * @since 5.0.8
 */
public class BasePathAwareWebMvcLinkBuilderFactory extends WebMvcLinkBuilderFactory {

	private static final Constructor<WebMvcLinkBuilder> LINK_BUILDER_CONSTRUCTOR;

	static {
		try {
			LINK_BUILDER_CONSTRUCTOR = WebMvcLinkBuilder.class.getDeclaredConstructor(
					UriComponents.class, TemplateVariables.class, List.class);
			LINK_BUILDER_CONSTRUCTOR.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(
					"Could not find WebMvcLinkBuilder(UriComponents, TemplateVariables, List) constructor", e);
		}
	}

	private final RepositoryRestConfiguration configuration;

	/**
	 * Creates a new {@link BasePathAwareWebMvcLinkBuilderFactory} for the given {@link RepositoryRestConfiguration}.
	 *
	 * @param configuration must not be {@literal null}.
	 */
	public BasePathAwareWebMvcLinkBuilderFactory(RepositoryRestConfiguration configuration) {

		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null");

		this.configuration = configuration;
	}

	/**
	 * Builds a {@link WebMvcLinkBuilder} for the given invocation value. If the target controller type is annotated with
	 * {@link BasePathAwareController}, the configured Spring Data REST {@code basePath} is prepended to the resulting
	 * URI.
	 *
	 * @param invocationValue the method invocation proxy created by {@code methodOn(...)}, must not be {@literal null}.
	 * @return a {@link WebMvcLinkBuilder} with the base path applied when appropriate.
	 */
	@Override
	public WebMvcLinkBuilder linkTo(Object invocationValue) {

		if (!isBasePathAwareController(invocationValue)) {
			return super.linkTo(invocationValue);
		}

		String basePath = configuration.getBasePath().toString();

		if (!StringUtils.hasText(basePath)) {
			return super.linkTo(invocationValue);
		}

		String normalizedBasePath = basePath.startsWith("/") ? basePath : "/" + basePath;

		return WebHandler.linkTo(invocationValue, this::createLinkBuilder, null, mapping -> {

			// Build the base URI from the current servlet mapping
			UriComponentsBuilder base = getBaseBuilder();

			// Prepend the basePath to the mapping path
			String mappingPath = mapping.getMapping();
			String fullPath = normalizedBasePath + (mappingPath.startsWith("/") ? mappingPath : "/" + mappingPath);

			return base.path(fullPath);

		}, () -> null);
	}

	/**
	 * Creates a {@link WebMvcLinkBuilder} from the given {@link UriComponents}, {@link TemplateVariables} and
	 * {@link Affordance} list using the package-private constructor via reflection.
	 */
	private WebMvcLinkBuilder createLinkBuilder(UriComponents components, TemplateVariables variables,
			List<Affordance> affordances) {
		try {
			return LINK_BUILDER_CONSTRUCTOR.newInstance(components, variables,
					affordances != null ? affordances : Collections.emptyList());
		} catch (Exception e) {
			throw new IllegalStateException("Could not create WebMvcLinkBuilder instance", e);
		}
	}

	/**
	 * Returns a {@link UriComponentsBuilder} based on the current servlet mapping, or a relative builder if no request
	 * context is available.
	 */
	private static UriComponentsBuilder getBaseBuilder() {

		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
			return ServletUriComponentsBuilder.fromCurrentServletMapping();
		}

		return UriComponentsBuilder.fromPath("/");
	}

	/**
	 * Returns whether the given invocation value targets a controller annotated with {@link BasePathAwareController}.
	 *
	 * @param invocationValue the method invocation proxy, must not be {@literal null}.
	 * @return {@literal true} if the target controller is annotated with {@link BasePathAwareController}.
	 */
	private static boolean isBasePathAwareController(Object invocationValue) {

		if (!(invocationValue instanceof LastInvocationAware)) {
			return false;
		}

		LastInvocationAware invocations = (LastInvocationAware) DummyInvocationUtils
				.getLastInvocationAware(invocationValue);

		if (invocations == null) {
			return false;
		}

		MethodInvocation invocation = invocations.getLastInvocation();

		if (invocation == null) {
			return false;
		}

		return AnnotatedElementUtils.hasAnnotation(invocation.getTargetType(), BasePathAwareController.class);
	}
}
