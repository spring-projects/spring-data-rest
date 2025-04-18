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

import java.util.Optional;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.util.Assert;

/**
 * {@link DefaultedPageable} implementation of {@link ExcerptProjector} that uses the given {@link ProjectionFactory}
 * and considers the given {@link ResourceMappings}.
 *
 * @author Oliver Gierke
 * @since 2.5
 */
public class DefaultExcerptProjector implements ExcerptProjector {

	private final ProjectionFactory factory;
	private final ResourceMappings mappings;

	public DefaultExcerptProjector(ProjectionFactory factory, ResourceMappings mappings) {

		Assert.notNull(factory, "ProjectionFactory must not be null");
		Assert.notNull(mappings, "ResourceMappings must not be null");

		this.factory = factory;
		this.mappings = mappings;
	}

	@Override
	public Object projectExcerpt(Object source) {

		Assert.notNull(source, "Projection source must not be null");

		ResourceMetadata metadata = mappings.getMetadataFor(source.getClass());

		return Optional.ofNullable(metadata) //
				.flatMap(ResourceMetadata::getExcerptProjection) //
				.filter(it -> !it.equals(source.getClass())) //
				.<Object> map(it -> factory.createProjection(it, source)) //
				.orElse(source);

	}

	@Override
	public boolean hasExcerptProjection(Class<?> type) {

		ResourceMetadata metadata = mappings.getMetadataFor(type);

		return metadata == null ? false : metadata.getExcerptProjection().isPresent();
	}
}
