/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.rest.core.support;

import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.hateoas.RelProvider;
import org.springframework.util.Assert;

/**
 * A {@link RelProvider} based on the {@link ResourceMappings} for the registered repositories.
 * 
 * @author Oliver Gierke
 */
public class RepositoryRelProvider implements RelProvider {

	private final ResourceMappings mappings;

	/**
	 * Creates a new {@link RepositoryRelProvider} for the given {@link ResourceMappings}.
	 * 
	 * @param mappings must not be {@literal null}.
	 */
	public RepositoryRelProvider(ResourceMappings mappings) {

		Assert.notNull(mappings, "ResourceMappings must not be null!");
		this.mappings = mappings;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.RelProvider#getCollectionResourceRelFor(java.lang.Class)
	 */
	@Override
	public String getCollectionResourceRelFor(Class<?> type) {
		return mappings.getMappingFor(type).getRel();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.RelProvider#getItemResourceRelFor(java.lang.Class)
	 */
	@Override
	public String getItemResourceRelFor(Class<?> type) {
		return mappings.getMappingFor(type).getItemResourceRel();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Class<?> delimiter) {
		return mappings.hasMappingFor(delimiter);
	}
}
