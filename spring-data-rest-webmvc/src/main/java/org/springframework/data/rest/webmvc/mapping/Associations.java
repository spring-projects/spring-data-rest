/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.rest.webmvc.mapping;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.util.Assert;

import static org.springframework.hateoas.TemplateVariable.VariableType.REQUEST_PARAM;

/**
 * A value object to for {@link Link}s representing associations.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Haroun Pacquee
 * @since 2.1
 */
@RequiredArgsConstructor
public class Associations {

	private final @NonNull @Getter ResourceMappings mappings;
	private final @NonNull RepositoryRestConfiguration config;

	/**
	 * Returns the links to render for the given {@link Association}.
	 *
	 * @param association must not be {@literal null}.
	 * @param path must not be {@literal null}.
	 * @return
	 */
	public List<Link> getLinksFor(Association<? extends PersistentProperty<?>> association, Path path) {

		Assert.notNull(association, "Association must not be null!");
		Assert.notNull(path, "Base path must not be null!");

		if (isLinkableAssociation(association)) {

			PersistentProperty<?> property = association.getInverse();
			ResourceMetadata metadata = mappings.getMetadataFor(property.getOwner().getType());
			ResourceMapping propertyMapping = metadata.getMappingFor(property);

			String href = path.slash(propertyMapping.getPath()).toString();
			String uri = new UriTemplate(href, getProjectionVariable(property)).toString();
			return Collections.singletonList(new Link(uri, propertyMapping.getRel()));
		}

		return Collections.emptyList();
	}

	private TemplateVariables getProjectionVariable(PersistentProperty<?> property) {
		ProjectionDefinitionConfiguration projectionConfiguration = config.getProjectionConfiguration();
		if (isProjectionPresent(property, projectionConfiguration)) {
			return new TemplateVariables(new TemplateVariable(projectionConfiguration.getParameterName(), REQUEST_PARAM));
		} else {
			return TemplateVariables.NONE;
		}
	}

	private boolean isProjectionPresent(PersistentProperty<?> property, ProjectionDefinitionConfiguration projectionConfiguration) {
		return Stream.of(property.getType(),
						property.getActualType(),
						property.getRawType(),
						property.getComponentType(),
						property.getMapValueType())
				.anyMatch(projectionConfiguration::hasProjectionFor);
	}

	/**
	 * Returns the {@link ResourceMetadata} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public ResourceMetadata getMetadataFor(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return mappings.getMetadataFor(type);
	}

	/**
	 * Returns whether the type of the given {@link PersistentProperty} is configured as lookup type.
	 *
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public boolean isLookupType(PersistentProperty<?> property) {

		Assert.notNull(property, "Persistent property must not be null!");

		return config.isLookupType(property.getActualType());
	}

	public boolean isIdExposed(PersistentEntity<?, ?> entity) {
		return config.isIdExposedFor(entity.getType());
	}

	/**
	 * Returns whether the given {@link Association} is linkable.
	 *
	 * @param association must not be {@literal null}.
	 * @return
	 */
	public boolean isLinkableAssociation(Association<? extends PersistentProperty<?>> association) {

		Assert.notNull(association, "Association must not be null!");

		return isLinkableAssociation(association.getInverse());
	}

	/**
	 * Returns whether the given property is an association that is linkable.
	 *
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public boolean isLinkableAssociation(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		if (!property.isAssociation() || config.isLookupType(property.getActualType())) {
			return false;
		}

		ResourceMetadata metadata = mappings.getMetadataFor(property.getOwner().getType());

		if (metadata != null && !metadata.isExported(property)) {
			return false;
		}

		metadata = mappings.getMetadataFor(property.getActualType());
		return metadata == null ? false : metadata.isExported();
	}
}