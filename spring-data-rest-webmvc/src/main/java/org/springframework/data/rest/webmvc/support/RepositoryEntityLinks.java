/*
 * Copyright 2012-2014 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.core.AbstractEntityLinks;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link EntityLinks} implementation that is able to create {@link Link} for domain classes managed by Spring Data
 * REST.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryEntityLinks extends AbstractEntityLinks {

	private final Repositories repositories;
	private final ResourceMappings mappings;
	private final RepositoryRestConfiguration config;
	private final HateoasPageableHandlerMethodArgumentResolver resolver;

	/**
	 * Creates a new {@link RepositoryEntityLinks}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 * @param resolver must not be {@literal null}.
	 */
	@Autowired
	public RepositoryEntityLinks(Repositories repositories, ResourceMappings mappings,
			RepositoryRestConfiguration config, HateoasPageableHandlerMethodArgumentResolver resolver) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");
		Assert.notNull(resolver, "HateoasPageableHandlerMethodArgumentResolver must not be null!");

		this.repositories = repositories;
		this.mappings = mappings;
		this.config = config;
		this.resolver = resolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Class<?> delimiter) {
		return repositories.hasRepositoryFor(delimiter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.EntityLinks#linkFor(java.lang.Class)
	 */
	@Override
	public LinkBuilder linkFor(Class<?> type) {

		ResourceMetadata metadata = mappings.getMappingFor(type);
		return new RepositoryLinkBuilder(metadata, config.getBaseUri());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.EntityLinks#linkFor(java.lang.Class, java.lang.Object[])
	 */
	@Override
	public LinkBuilder linkFor(Class<?> type, Object... parameters) {
		return linkFor(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.EntityLinks#linkToCollectionResource(java.lang.Class)
	 */
	@Override
	public Link linkToCollectionResource(Class<?> type) {

		ResourceMetadata metadata = mappings.getMappingFor(type);

		if (metadata.isPagingResource()) {

			Link link = linkFor(type).withSelfRel();
			String href = link.getHref();
			UriComponents components = UriComponentsBuilder.fromUriString(href).build();
			TemplateVariables variables = resolver.getPaginationTemplateVariables(null, components);

			return new Link(new UriTemplate(href, variables), metadata.getRel());
		}

		return linkFor(type).withRel(metadata.getRel());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.EntityLinks#linkToSingleResource(java.lang.Class, java.lang.Object)
	 */
	@Override
	public Link linkToSingleResource(Class<?> type, Object id) {

		ResourceMetadata metadata = mappings.getMappingFor(type);
		return linkFor(type).slash(id).withRel(metadata.getItemResourceRel());
	}
}
