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

import java.io.Serializable;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.repository.invoke.RepositoryMethodInvoker;
import org.springframework.data.rest.repository.mapping.ResourceMetadata;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
class RepositoryRestRequest {

	private final HttpServletRequest request;
	private final URI baseUri;
	private final ResourceMetadata resourceMetadata;
	private final RepositoryMethodInvoker repoMethodInvoker;
	private final PersistentEntity<?, ?> persistentEntity;

	public RepositoryRestRequest(RepositoryRestConfiguration config, Repositories repositories,
			HttpServletRequest request, URI baseUri, ResourceMetadata repoInfo, ConversionService conversionService) {

		this.request = request;
		this.baseUri = baseUri;
		this.resourceMetadata = repoInfo;
		if (resourceMetadata == null || !resourceMetadata.isExported()) {

			this.repoMethodInvoker = null;
			this.persistentEntity = null;

		} else {

			Class<?> domainType = repoInfo.getDomainType();
			CrudRepository<Object, Serializable> repositoryFor = repositories.getRepositoryFor(domainType);
			RepositoryInformation information = repositories.getRepositoryInformationFor(domainType);

			this.repoMethodInvoker = new RepositoryMethodInvoker(repositoryFor, information, conversionService);
			this.persistentEntity = repositories.getPersistentEntity(domainType);
		}
	}

	HttpServletRequest getRequest() {
		return request;
	}

	URI getBaseUri() {
		return baseUri;
	}

	ResourceMetadata getRepositoryResourceMapping() {
		return resourceMetadata;
	}

	RepositoryMethodInvoker getRepositoryMethodInvoker() {
		return repoMethodInvoker;
	}

	PersistentEntity<?, ?> getPersistentEntity() {
		return persistentEntity;
	}
}
