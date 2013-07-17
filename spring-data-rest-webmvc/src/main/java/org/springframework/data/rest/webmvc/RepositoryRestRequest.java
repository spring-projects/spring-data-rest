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

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.repository.invoke.RepositoryInvoker;
import org.springframework.data.rest.repository.mapping.ResourceMetadata;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
class RepositoryRestRequest {

	private final NativeWebRequest request;
	private final URI baseUri;
	private final ResourceMetadata resourceMetadata;
	private final RepositoryInvoker repoInvoker;
	private final PersistentEntity<?, ?> persistentEntity;

	public RepositoryRestRequest(PersistentEntity<?, ?> entity, NativeWebRequest request, URI baseUri,
			ResourceMetadata repoInfo, RepositoryInvoker invoker) {

		this.request = request;
		this.baseUri = baseUri;
		this.resourceMetadata = repoInfo;
		if (resourceMetadata == null || !resourceMetadata.isExported()) {

			this.repoInvoker = null;
			this.persistentEntity = null;

		} else {
			this.repoInvoker = invoker;
			this.persistentEntity = entity;
		}
	}

	NativeWebRequest getRequest() {
		return request;
	}

	HttpMethod getRequestMethod() {
		return HttpMethod.valueOf(request.getNativeRequest(HttpServletRequest.class).getMethod());
	}

	URI getBaseUri() {
		return baseUri;
	}

	ResourceMetadata getResourceMetadata() {
		return resourceMetadata;
	}

	RepositoryInvoker getRepositoryInvoker() {
		return repoInvoker;
	}

	PersistentEntity<?, ?> getPersistentEntity() {
		return persistentEntity;
	}
}
