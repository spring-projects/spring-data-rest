/*
 * Copyright 2014-2022 the original author or authors.
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

import java.util.function.Function;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.projection.ProjectionDefinitions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Projector} looking up a projection by name for the given source type.
 *
 * @author Oliver Gierke
 */
public class PersistentEntityProjector extends DefaultExcerptProjector implements Projector {

	private final ProjectionDefinitions definitions;
	private final ProjectionFactory factory;
	private final String projection;

	/**
	 * Creates a new {@link PersistentEntityProjector} using the given {@link ProjectionDefinitions},
	 * {@link ProjectionFactory} and projection name.
	 *
	 * @param projectionDefinitions must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 * @param projection can be empty.
	 */
	public PersistentEntityProjector(ProjectionDefinitions projectionDefinitions, ProjectionFactory factory,
			String projection, ResourceMappings mappings) {

		super(factory, mappings);

		Assert.notNull(projectionDefinitions, "ProjectionDefinitions must not be null!");
		Assert.notNull(factory, "ProjectionFactory must not be null!");

		this.factory = factory;
		this.definitions = projectionDefinitions;
		this.projection = projection;
	}

	public Object project(Object source) {
		return projectWithDefault(source, Function.identity());
	}

	@Override
	public Object projectExcerpt(Object source) {
		return projectWithDefault(source, PersistentEntityProjector.super::projectExcerpt);
	}

	/**
	 * Creates the projection for the given source instance falling back to the given {@link Function} if no explicit
	 * projection is selected.
	 *
	 * @param source must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 * @return
	 */
	private Object projectWithDefault(Object source, Function<Object, Object> converter) {

		Assert.notNull(source, "Projection source must not be null!");
		Assert.notNull(converter, "Converter must not be null!");

		if (!StringUtils.hasText(projection)) {
			return converter.apply(source);
		}

		Class<?> projectionType = definitions.getProjectionType(source.getClass(), projection);
		return projectionType == null ? converter.apply(source) : factory.createProjection(projectionType, source);
	}
}
