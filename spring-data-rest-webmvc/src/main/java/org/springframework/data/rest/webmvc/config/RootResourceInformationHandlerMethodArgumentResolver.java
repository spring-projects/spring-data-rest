/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to create {@link RootResourceInformation} for injection into Spring MVC
 * controller methods.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RootResourceInformationHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final RepositoryInvokerFactory invokerFactory;
	private final ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver;

	/**
	 * Creates a new {@link RootResourceInformationHandlerMethodArgumentResolver} using the given {@link Repositories},
	 * {@link RepositoryInvokerFactory} and {@link ResourceMetadataHandlerMethodArgumentResolver}.
	 *
	 * @param repositories must not be {@literal null}.
	 * @param invokerFactory must not be {@literal null}.
	 * @param resourceMetadataResolver must not be {@literal null}.
	 */
	public RootResourceInformationHandlerMethodArgumentResolver(Repositories repositories,
			RepositoryInvokerFactory invokerFactory, ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver) {

		Assert.notNull(repositories, "Repositories must not be null");
		Assert.notNull(invokerFactory, "invokerFactory must not be null");
		Assert.notNull(resourceMetadataResolver, "ResourceMetadataHandlerMethodArgumentResolver must not be null");

		this.repositories = repositories;
		this.invokerFactory = invokerFactory;
		this.resourceMetadataResolver = resourceMetadataResolver;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return RootResourceInformation.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public RootResourceInformation resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		ResourceMetadata resourceMetadata = resourceMetadataResolver.resolveArgument(parameter, mavContainer, webRequest,
				binderFactory);

		Class<?> domainType = resourceMetadata.getDomainType();
		RepositoryInvoker repositoryInvoker = invokerFactory.getInvokerFor(domainType);
		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(domainType);

		// TODO reject if ResourceMetadata cannot be resolved
		return new RootResourceInformation(resourceMetadata, persistentEntity,
				postProcess(parameter, repositoryInvoker, domainType, webRequest.getParameterMap()));
	}

	/**
	 * Potentially customize the given {@link RepositoryInvoker} for the given domain type. Default implementations simply
	 * returns the given invoker as is.
	 *
	 * @param parameter must not be {@literal null}.
	 * @param invoker will never be {@literal null}.
	 * @param domainType will never be {@literal null}.
	 * @param parameters will never be {@literal null}.
	 * @return the post-processed {@link RepositoryInvoker}.
	 */
	protected RepositoryInvoker postProcess(MethodParameter parameter, RepositoryInvoker invoker, Class<?> domainType,
			Map<String, String[]> parameters) {
		return invoker;
	}
}
