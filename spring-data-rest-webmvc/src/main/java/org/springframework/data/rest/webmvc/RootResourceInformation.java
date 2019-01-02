/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.HttpMethods;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.ResourceType;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.data.rest.core.mapping.SupportedHttpMethods;
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

	public SupportedHttpMethods getSupportedMethods() {
		return resourceMetadata.getSupportedHttpMethods();
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

		Assert.notNull(httpMethod, "HTTP method must not be null!");
		Assert.notNull(resourceType, "Resource type must not be null!");

		if (!resourceMetadata.isExported()) {
			throw new ResourceNotFoundException();
		}

		SupportedHttpMethods httpMethods = resourceMetadata.getSupportedHttpMethods();
		HttpMethods supportedMethods = httpMethods.getMethodsFor(resourceType);

		if (!supportedMethods.contains(httpMethod)) {
			reject(httpMethod, supportedMethods);
		}
	}

	/**
	 * Verifies that the given {@link HttpMethod} is supported for the given {@link PersistentProperty}.
	 *
	 * @param httpMethod must not be {@literal null}.
	 * @param property must not be {@literal null}.
	 * @throws ResourceNotFoundException if the repository is not exported at all.
	 * @throws HttpRequestMethodNotSupportedException if the {@link PersistentProperty} does not support the given
	 *           {@link HttpMethod}. Will contain all supported methods as indicators for clients.
	 */
	public void verifySupportedMethod(HttpMethod httpMethod, PersistentProperty<?> property)
			throws HttpRequestMethodNotSupportedException {

		Assert.notNull(httpMethod, "HTTP method must not be null!");
		Assert.notNull(property, "Resource type must not be null!");

		if (!resourceMetadata.isExported()) {
			throw new ResourceNotFoundException();
		}

		SupportedHttpMethods httpMethods = resourceMetadata.getSupportedHttpMethods();
		HttpMethods supportedMethods = httpMethods.getMethodsFor(property);

		if (!supportedMethods.contains(httpMethod)) {
			reject(httpMethod, supportedMethods);
		}
	}

	public void verifyPutForCreation() throws HttpRequestMethodNotSupportedException {

		SupportedHttpMethods supportedHttpMethods = resourceMetadata.getSupportedHttpMethods();

		if (!supportedHttpMethods.allowsPutForCreation()) {
			reject(HttpMethod.PUT, supportedHttpMethods.getMethodsFor(ResourceType.ITEM));
		}
	}

	private static void reject(HttpMethod method, HttpMethods supported) throws HttpRequestMethodNotSupportedException {

		Set<String> stringMethods = supported.butWithout(method) //
				.stream() //
				.map(HttpMethod::name) //
				.collect(Collectors.toSet());

		throw new HttpRequestMethodNotSupportedException(method.name(), stringMethods);
	}
}
