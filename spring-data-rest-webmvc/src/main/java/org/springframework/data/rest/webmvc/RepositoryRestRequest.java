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

import static org.springframework.data.rest.core.util.UriUtils.*;
import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.invoke.RepositoryMethodInvoker;
import org.springframework.hateoas.Link;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@SuppressWarnings("deprecation")
class RepositoryRestRequest {

	private final HttpServletRequest request;
	private final URI baseUri;
	private final ResourceMapping repoMapping;
	private final Link repoLink;
	private final Object repository;
	private final RepositoryMethodInvoker repoMethodInvoker;
	private final PersistentEntity<?, ?> persistentEntity;
	private final ResourceMapping entityMapping;

	public RepositoryRestRequest(RepositoryRestConfiguration config, Repositories repositories,
			HttpServletRequest request, URI baseUri, RepositoryInformation repoInfo, ConversionService conversionService) {
		this.request = request;
		this.baseUri = baseUri;
		this.repoMapping = getResourceMapping(config, repoInfo);
		if (null == repoMapping || !repoMapping.isExported()) {
			this.repoLink = null;
			this.repository = null;
			this.repoMethodInvoker = null;
			this.persistentEntity = null;
			this.entityMapping = null;
		} else {
			this.repoLink = new Link(buildUri(baseUri, repoMapping.getPath()).toString(), repoMapping.getRel());
			this.repository = repositories.getRepositoryFor(repoInfo.getDomainType());
			this.persistentEntity = repositories.getPersistentEntity(repoInfo.getDomainType());
			this.repoMethodInvoker = new RepositoryMethodInvoker(repository, repoInfo, conversionService);
			this.entityMapping = getResourceMapping(config, persistentEntity);
		}
	}

	HttpServletRequest getRequest() {
		return request;
	}

	URI getBaseUri() {
		return baseUri;
	}

	ResourceMapping getRepositoryResourceMapping() {
		return repoMapping;
	}

	Link getRepositoryLink() {
		return repoLink;
	}

	RepositoryMethodInvoker getRepositoryMethodInvoker() {
		return repoMethodInvoker;
	}

	PersistentEntity<?, ?> getPersistentEntity() {
		return persistentEntity;
	}

	ResourceMapping getPersistentEntityResourceMapping() {
		return entityMapping;
	}
}
