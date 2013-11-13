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

import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.invoke.RepositoryInvoker;
import org.springframework.data.rest.core.invoke.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryRestRequestHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final RepositoryInvokerFactory invokerFactory;
	private final ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver;

	/**
	 * Creates a new {@link RepositoryRestRequestHandlerMethodArgumentResolver} using the given {@link Repositories},
	 * {@link RepositoryInvokerFactory} and {@link ResourceMetadataHandlerMethodArgumentResolver}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param invokerFactory must not be {@literal null}.
	 * @param resourceMetadataResolver must not be {@literal null}.
	 */
	public RepositoryRestRequestHandlerMethodArgumentResolver(Repositories repositories,
			RepositoryInvokerFactory invokerFactory, ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(invokerFactory, "invokerFactory must not be null!");
		Assert.notNull(resourceMetadataResolver, "ResourceMetadataHandlerMethodArgumentResolver must not be null!");

		this.repositories = repositories;
		this.invokerFactory = invokerFactory;
		this.resourceMetadataResolver = resourceMetadataResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return RepositoryRestRequest.class.isAssignableFrom(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public RepositoryRestRequest resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		ResourceMetadata repoInfo = resourceMetadataResolver.resolveArgument(parameter, mavContainer, webRequest,
				binderFactory);

		RepositoryInvoker repositoryInvoker = invokerFactory.getInvokerFor(repoInfo.getDomainType());
		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(repoInfo.getDomainType());

		// TODO reject if ResourceMetadata cannot be resolved

		return new RepositoryRestRequest(persistentEntity, webRequest, repoInfo, repositoryInvoker);
	}
}
