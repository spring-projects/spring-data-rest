/*
 * Copyright 2016 the original author or authors.
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

import java.lang.reflect.Method;

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
 * @since 2.6, 2.5.3, 2.4.5
 */
public class DomainClassResolver {

	private final Repositories repositories;
	private final ResourceMappings mappings;
	private final BaseUri baseUri;

	/**
	 * Creates a new {@link DomainClassResolver} for the given {@link Repositories} and {@link ResourceMappings}.
	 *
	 * @param repositories must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 * @param baseUri must not be {@literal null}.
	 */
	private DomainClassResolver(Repositories repositories, ResourceMappings mappings, BaseUri baseUri) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(baseUri, "BaseUri must not be null!");

		this.repositories = repositories;
		this.mappings = mappings;
		this.baseUri = baseUri;
	}

	/**
	 * Creates a new {@link DomainClassResolver} for the given {@link Repositories} and {@link ResourceMappings}.
	 *
	 * @param repositories must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 * @param baseUri must not be {@literal null}.
	 */
	public static DomainClassResolver of(Repositories repositories, ResourceMappings mappings, BaseUri baseUri) {
		return new DomainClassResolver(repositories, mappings, baseUri);
	}

	/**
	 * Resolves a domain class that is associated with the {@link NativeWebRequest}
	 *
	 * @param method must not be {@literal null}.
	 * @param webRequest must not be {@literal null}.
	 * @return domain type that is associated with this request.
	 * @throws IllegalArgumentException if there's no repository key associated or no domain type can be resolved.
	 */
	public Class<?> resolve(Method method, NativeWebRequest webRequest) {

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(webRequest, "NativeWebRequest must not be null!");

		String lookupPath = baseUri.getRepositoryLookupPath(webRequest);
		String repositoryKey = UriUtils.findMappingVariable("repository", method, lookupPath);

		if (!StringUtils.hasText(repositoryKey)) {
			throw new IllegalArgumentException(String.format("Could not determine a repository key from %s.", lookupPath));
		}

		for (Class<?> domainType : repositories) {

			ResourceMetadata mapping = mappings.getMetadataFor(domainType);

			if (mapping.getPath().matches(repositoryKey) && mapping.isExported()) {
				return domainType;
			}
		}

		throw new IllegalArgumentException(
				String.format("Could not resolve an exported domain type for %s.", repositoryKey));
	}
}
