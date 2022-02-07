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
package org.springframework.data.rest.webmvc.mapping;

import static org.springframework.hateoas.TemplateVariable.VariableType.*;

import java.util.Collections;
import java.util.List;

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

/**
 * A value object to for {@link Link}s representing associations.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Haroun Pacquee
 * @since 2.1
 */
public class Associations {

	private final ResourceMappings mappings;
	private final RepositoryRestConfiguration config;

	public Associations(ResourceMappings mappings, RepositoryRestConfiguration config) {

		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");

		this.mappings = mappings;
		this.config = config;
	}

	public ResourceMappings getMappings() {
		return this.mappings;
	}

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
			UriTemplate template = UriTemplate.of(href).with(getProjectionVariable(property));

			return Collections.singletonList(Link.of(template, propertyMapping.getRel()));
		}

		return Collections.emptyList();
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

	private TemplateVariables getProjectionVariable(PersistentProperty<?> property) {

		ProjectionDefinitionConfiguration projectionConfiguration = config.getProjectionConfiguration();

		return projectionConfiguration.hasProjectionFor(property.getActualType()) //
				? new TemplateVariables(new TemplateVariable(projectionConfiguration.getParameterName(), REQUEST_PARAM)) //
				: TemplateVariables.NONE;
	}
}
