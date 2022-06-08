/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.data.rest.core.support;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.util.Assert;

/**
 * A {@link LinkRelationProvider} based on the {@link ResourceMappings} for the registered repositories.
 *
 * @author Oliver Gierke
 */
@Order(Ordered.LOWEST_PRECEDENCE + 10)
public class RepositoryRelProvider implements LinkRelationProvider {

	private final ObjectFactory<ResourceMappings> mappings;

	/**
	 * Creates a new {@link RepositoryRelProvider} for the given {@link ResourceMappings}.
	 *
	 * @param mappings must not be {@literal null}.
	 */
	public RepositoryRelProvider(ObjectFactory<ResourceMappings> mappings) {

		Assert.notNull(mappings, "ResourceMappings must not be null");
		this.mappings = mappings;
	}

	@Override
	public LinkRelation getCollectionResourceRelFor(Class<?> type) {
		return mappings.getObject().getMetadataFor(type).getRel();
	}

	@Override
	public LinkRelation getItemResourceRelFor(Class<?> type) {
		return mappings.getObject().getMetadataFor(type).getItemResourceRel();
	}

	@Override
	public boolean supports(LookupContext context) {
		return mappings.getObject().hasMappingFor(context.getType());
	}
}
