/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.core.invoke.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Meta-information about the root repository resource.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RootResourceInformation {

	private final ResourceMetadata resourceMetadata;
	private final RepositoryInvoker invoker;
	private final PersistentEntity<?, ?> persistentEntity;

	public RootResourceInformation(ResourceMetadata metadata, PersistentEntity<?, ?> entity, RepositoryInvoker invoker) {

		this.resourceMetadata = metadata;
		if (resourceMetadata == null || !resourceMetadata.isExported()) {

			this.invoker = null;
			this.persistentEntity = null;

		} else {
			this.invoker = invoker;
			this.persistentEntity = entity;
		}
	}

	public Class<?> getDomainType() {
		return resourceMetadata.getDomainType();
	}

	public ResourceMetadata getResourceMetadata() {
		return resourceMetadata;
	}

	public SearchResourceMappings getSearchMappings() {
		return resourceMetadata.getSearchResourceMappings();
	}

	public RepositoryInvoker getInvoker() {
		return invoker;
	}

	public PersistentEntity<?, ?> getPersistentEntity() {
		return persistentEntity;
	}

	/**
	 * Returns the supported {@link HttpMethod}s for the given {@link ResourceType}.
	 * 
	 * @param resourcType must not be {@literal null}.
	 * @return
	 */
	public Set<HttpMethod> getSupportedMethods(ResourceType resourcType) {

		Assert.notNull(resourcType, "Resource type must not be null!");

		if (invoker == null) {
			return Collections.emptySet();
		}

		Set<HttpMethod> methods = new HashSet<HttpMethod>();
		methods.add(HttpMethod.OPTIONS);

		switch (resourcType) {
			case COLLECTION:

				if (invoker.exposesFindAll()) {
					methods.add(HttpMethod.GET);
					methods.add(HttpMethod.HEAD);
				}

				if (invoker.exposesSave()) {
					methods.add(HttpMethod.POST);
				}

				break;

			case ITEM:

				if (invoker.exposesDelete() && invoker.hasFindOneMethod()) {
					methods.add(HttpMethod.DELETE);
				}

				if (invoker.exposesFindOne()) {
					methods.add(HttpMethod.GET);
					methods.add(HttpMethod.HEAD);
				}

				if (invoker.exposesSave()) {
					methods.add(HttpMethod.PUT);
					methods.add(HttpMethod.PATCH);
				}

				break;

			default:
				throw new IllegalArgumentException(String.format("Unsupported resource type %s!", resourcType));
		}

		return Collections.unmodifiableSet(methods);
	}

	/**
	 * Returns whether the given {@link HttpMethod} is supported for the given {@link ResourceType}.
	 * 
	 * @param httpMethod must not be {@literal null}.
	 * @param resourceType must not be {@literal null}.
	 * @return
	 */
	public boolean supports(HttpMethod httpMethod, ResourceType resourceType) {

		Assert.notNull(httpMethod, "HTTP method must not be null!");
		Assert.notNull(resourceType, "Resource type must not be null!");

		return getSupportedMethods(resourceType).contains(httpMethod);
	}

	/**
	 * Verifies that the given {@link HttpMethod} is supported for the given {@link ResourceType}.
	 * 
	 * @param httpMethod must not be {@literal null}.
	 * @param resourceType must not be {@literal null}.
	 * @throws ResourceNotFoundException if the repository is not exported at all.
	 * @throws HttpRequestMethodNotSupportedException if the {@link ResourceType} does not support the given
	 *           {@link HttpMethod}. Will contain all supported methods as indicators for clients.
	 */
	public void verifySupportedMethod(HttpMethod httpMethod, ResourceType resourceType)
			throws HttpRequestMethodNotSupportedException, ResourceNotFoundException {

		if (!resourceMetadata.isExported()) {
			throw new ResourceNotFoundException();
		}

		Assert.notNull(httpMethod, "HTTP method must not be null!");
		Assert.notNull(resourceType, "Resource type must not be null!");

		Collection<HttpMethod> supportedMethods = getSupportedMethods(resourceType);

		if (!supportedMethods.contains(httpMethod)) {

			Set<String> stringMethods = new HashSet<String>();

			for (HttpMethod supportedMethod : supportedMethods) {
				stringMethods.add(supportedMethod.name());
			}

			throw new HttpRequestMethodNotSupportedException(httpMethod.name(), stringMethods);
		}
	}
}
