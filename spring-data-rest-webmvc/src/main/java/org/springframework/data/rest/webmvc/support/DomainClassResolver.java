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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
 * @since 2.6, 2.5.3
 */
@RequiredArgsConstructor(staticName = "of")
public class DomainClassResolver {

	private final @NonNull Repositories repositories;
	private final @NonNull ResourceMappings mappings;
	private final @NonNull BaseUri baseUri;

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
