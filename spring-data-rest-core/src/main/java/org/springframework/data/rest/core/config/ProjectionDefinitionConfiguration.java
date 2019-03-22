/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.rest.core.config;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.projection.ProjectionDefinitions;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Wrapper class to register projection definitions for later lookup by name and source type.
 *
 * @author Oliver Gierke
 */
public class ProjectionDefinitionConfiguration implements ProjectionDefinitions {

	private static final String PROJECTION_ANNOTATION_NOT_FOUND = "Projection annotation not found on %s! Either add the annotation or hand source type to the registration manually!";
	private static final String DEFAULT_PROJECTION_PARAMETER_NAME = "projection";

	private final Set<ProjectionDefinition> projectionDefinitions;
	private String parameterName = DEFAULT_PROJECTION_PARAMETER_NAME;

	/**
	 * Creates a new {@link ProjectionDefinitionConfiguration}.
	 */
	public ProjectionDefinitionConfiguration() {
		this.projectionDefinitions = new HashSet<ProjectionDefinition>();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.projection.ProjectionDefinitions#getParameterName()
	 */
	public String getParameterName() {
		return parameterName;
	}

	/**
	 * Configures the request parameter name to be used to accept the projection name to be returned.
	 *
	 * @param parameterName defaults to {@value ProjectionDefinitionConfiguration#DEFAULT_PROJECTION_PARAMETER_NAME}, will
	 *          be set back to this default if {@literal null} or an empty value is configured.
	 */
	public void setParameterName(String parameterName) {
		this.parameterName = StringUtils.hasText(parameterName) ? parameterName : DEFAULT_PROJECTION_PARAMETER_NAME;
	}

	/**
	 * Adds the given projection type to the configuration. The type has to be annotated with {@link Projection} for
	 * additional metadata.
	 *
	 * @param projectionType must not be {@literal null}.
	 * @return
	 * @see Projection
	 */
	public ProjectionDefinitionConfiguration addProjection(Class<?> projectionType) {

		Assert.notNull(projectionType, "Projection type must not be null!");
		Projection annotation = AnnotationUtils.findAnnotation(projectionType, Projection.class);

		if (annotation == null) {
			throw new IllegalArgumentException(String.format(PROJECTION_ANNOTATION_NOT_FOUND, projectionType));
		}

		String name = annotation.name();
		Class<?>[] sourceTypes = annotation.types();

		return StringUtils.hasText(name) ? addProjection(projectionType, name, sourceTypes)
				: addProjection(projectionType, sourceTypes);
	}

	/**
	 * Adds a projection type for the given source types. The name of the projection will be defaulted to the
	 * uncapitalized simply class name.
	 *
	 * @param projectionType must not be {@literal null}.
	 * @param sourceTypes must not be {@literal null} or empty.
	 * @return
	 */
	public ProjectionDefinitionConfiguration addProjection(Class<?> projectionType, Class<?>... sourceTypes) {

		Assert.notNull(projectionType, "Projection type must not be null!");
		return addProjection(projectionType, StringUtils.uncapitalize(projectionType.getSimpleName()), sourceTypes);
	}

	/**
	 * Adds the given projection type for the given source types under the given name.
	 *
	 * @param projectionType must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @param sourceTypes must not be {@literal null} or empty.
	 * @return
	 */
	public ProjectionDefinitionConfiguration addProjection(Class<?> projectionType, String name,
			Class<?>... sourceTypes) {

		Assert.notNull(projectionType, "Projection type must not be null!");
		Assert.hasText(name, "Name must not be null or empty!");
		Assert.notEmpty(sourceTypes, "Source types must not be null!");

		for (Class<?> sourceType : sourceTypes) {
			this.projectionDefinitions.add(ProjectionDefinition.of(sourceType, projectionType, name));
		}

		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.config.ProjectionDefinitions#getProjectionType(java.lang.Class, java.lang.String)
	 */
	@Override
	public Class<?> getProjectionType(Class<?> sourceType, String name) {
		return getProjectionsFor(sourceType).get(name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.projection.ProjectionDefinitions#hasProjectionFor(java.lang.Class)
	 */
	@Override
	public boolean hasProjectionFor(Class<?> sourceType) {

		for (ProjectionDefinition definition : projectionDefinitions) {
			if (definition.sourceType.isAssignableFrom(sourceType)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns all projections registered for the given source type.
	 *
	 * @param sourceType must not be {@literal null}.
	 * @return
	 */
	public Map<String, Class<?>> getProjectionsFor(Class<?> sourceType) {

		Assert.notNull(sourceType, "Source type must not be null!");

		Class<?> userType = ClassUtils.getUserClass(sourceType);
		Map<String, ProjectionDefinition> byName = new HashMap<String, ProjectionDefinition>();
		Map<String, Class<?>> result = new HashMap<String, Class<?>>();

		for (ProjectionDefinition entry : projectionDefinitions) {

			if (!entry.sourceType.isAssignableFrom(userType)) {
				continue;
			}

			ProjectionDefinition existing = byName.get(entry.name);

			if (existing == null || isSubTypeOf(entry.sourceType, existing.sourceType)) {
				byName.put(entry.name, entry);
				result.put(entry.name, entry.targetType);
			}
		}

		return result;
	}

	private static boolean isSubTypeOf(Class<?> left, Class<?> right) {
		return right.isAssignableFrom(left) && !left.equals(right);
	}

	/**
	 * Value object to define lookup keys for projections.
	 *
	 * @author Oliver Gierke
	 */
	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	static final class ProjectionDefinition {

		private final @NonNull Class<?> sourceType, targetType;
		private final @NonNull String name;

		/**
		 * Creates a new {@link ProjectionDefinitionKey} for the given source type and name;
		 *
		 * @param sourceType must not be {@literal null}.
		 * @param targetType must not be {@literal null}.
		 * @param name must not be {@literal null} or empty.
		 */
		static ProjectionDefinition of(Class<?> sourceType, Class<?> targetType, String name) {

			Assert.hasText(name, "Name must not be null or empty!");

			return new ProjectionDefinition(sourceType, targetType, name);
		}
	}
}
