/*
 * Copyright 2016-2025 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.util.UriUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Resolves a domain class from a web request. Domain class resolution is only available for {@link NativeWebRequest web
 * requests} related to mapped and exported {@link Repositories}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.6
 */
public class DomainClassResolver {

	private final Repositories repositories;
	private final ResourceMappings mappings;
	private final BaseUri baseUri;

	private DomainClassResolver(Repositories repositories, ResourceMappings mappings,
			BaseUri baseUri) {

		Assert.notNull(repositories, "Repositories must not be null");
		Assert.notNull(mappings, "ResourceMappings must not be null");
		Assert.notNull(baseUri, "BaseUri must not be null");

		this.repositories = repositories;
		this.mappings = mappings;
		this.baseUri = baseUri;
	}

	public static DomainClassResolver of(Repositories repositories,
			ResourceMappings mappings, BaseUri baseUri) {
		return new DomainClassResolver(repositories, mappings, baseUri);
	}

	/**
	 * Resolves a domain class that is associated with the {@link NativeWebRequest}
	 *
	 * @param method must not be {@literal null}.
	 * @param webRequest must not be {@literal null}.
	 * @return domain type that is associated with this request or {@literal null} if no domain class can be resolved.
	 */
	public Class<?> resolve(Method method, NativeWebRequest webRequest) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(webRequest, "NativeWebRequest must not be null");

		String lookupPath = baseUri.getRepositoryLookupPath(webRequest);
		String repositoryKey = UriUtils.findMappingVariable("repository", method, lookupPath);

		if (!StringUtils.hasText(repositoryKey)) {

			List<String> pathSegments = UriUtils.getPathSegments(method);

			if (!pathSegments.isEmpty()) {
				repositoryKey = pathSegments.get(0);
			}
		}

		if (!StringUtils.hasText(repositoryKey)) {
			return null;
		}

		for (Class<?> domainType : repositories) {

			ResourceMetadata mapping = mappings.getMetadataFor(domainType);

			if (mapping.getPath().matches(repositoryKey) && mapping.isExported()) {
				return domainType;
			}
		}

		return null;
	}
}
