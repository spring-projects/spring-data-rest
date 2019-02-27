/*
 * Copyright 2015-2019 the original author or authors.
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

import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A custom {@link ResourceSupport} type to be able to write custom {@link ResourceProcessor}s to add additional links
 * to ones automatically exposed for Spring Data repository query methods.
 *
 * @author Oliver Gierke
 */
public class RepositorySearchesResource extends RepresentationModel<RepositorySearchesResource> {

	private final Class<?> domainType;

	/**
	 * Creates a new {@link RepositorySearchesResource} for the given domain type.
	 *
	 * @param domainType must not be {@literal null}.
	 */
	RepositorySearchesResource(Class<?> domainType) {

		Assert.notNull(domainType, "Domain type must not be null!");
		this.domainType = domainType;
	}

	/**
	 * Returns the domain type for which the resource lists searches.
	 *
	 * @return
	 */
	@JsonIgnore
	public Class<?> getDomainType() {
		return domainType;
	}
}
