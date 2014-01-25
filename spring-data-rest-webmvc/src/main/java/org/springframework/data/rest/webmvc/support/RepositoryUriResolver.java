/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import static org.springframework.util.StringUtils.hasText;

import java.net.URI;

import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.util.Assert;

/**
 * Support class used to resolve a URI to a repository.
 * 
 * @author Nick Weedon
 */
public class RepositoryUriResolver {
	
	private final Repositories repositories;
	private final ResourceMappings mappings;
	
	public RepositoryUriResolver(Repositories repositories,
			ResourceMappings mappings) {
		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");
		
		this.repositories = repositories;
		this.mappings = mappings;
	}

	public ResourceMetadata findRepositoryInfoForUri(URI uri) {
		return findRepositoryInfoForUriPath(uri.getPath());
	}
	
	public ResourceMetadata findRepositoryInfoForUriPath(String requestUriPath) {
			
		if (requestUriPath.startsWith("/")) {
			requestUriPath = requestUriPath.substring(1);
		}
	
		String[] parts = requestUriPath.split("/");
	
		if (parts.length == 0) {
			// Root request
			return null;
		}
	
		return findRepositoryInfoForSegment(parts[0]);
	}
	
	public ResourceMetadata findRepositoryInfoForSegment(String pathSegment) {
	
		if (!hasText(pathSegment)) {
			return null;
		}
	
		for (Class<?> domainType : repositories) {
			ResourceMetadata mapping = mappings.getMappingFor(domainType);
			if (mapping.getPath().matches(pathSegment) && mapping.isExported()) {
				return mapping;
			}
		}
	
		return null;
	}

}
