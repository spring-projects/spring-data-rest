/*
 * Copyright 2012-2021 the original author or authors.
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

import static org.springframework.hateoas.TemplateVariable.VariableType.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.MethodResourceMapping;
import org.springframework.data.rest.core.mapping.ParameterMetadata;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter.DefaultIdConverter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariable.VariableType;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.hateoas.server.core.AbstractEntityLinks;
import org.springframework.plugin.core.PluginRegistry;
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
	private final PagingAndSortingTemplateVariables templateVariables;
	private final PluginRegistry<BackendIdConverter, Class<?>> idConverters;

	public RepositoryEntityLinks(Repositories repositories, ResourceMappings mappings, RepositoryRestConfiguration config,
			PagingAndSortingTemplateVariables templateVariables, PluginRegistry<BackendIdConverter, Class<?>> idConverters) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");
		Assert.notNull(templateVariables, "PagingAndSortingTemplateVariables must not be null!");
		Assert.notNull(idConverters, "BackendIdConverters must not be null!");

		this.repositories = repositories;
		this.mappings = mappings;
		this.config = config;
		this.templateVariables = templateVariables;
		this.idConverters = idConverters;
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
	 * @see org.springframework.hateoas.server.EntityLinks#linkFor(java.lang.Class)
	 */
	@Override
	public LinkBuilder linkFor(Class<?> type) {

		ResourceMetadata metadata = mappings.getMetadataFor(type);
		return new RepositoryLinkBuilder(metadata, new BaseUri(config.getBasePath()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.EntityLinks#linkFor(java.lang.Class, java.lang.Object[])
	 */
	@Override
	public LinkBuilder linkFor(Class<?> type, Object... parameters) {
		return linkFor(type);
	}

	/**
	 * Returns the link to to the paged colelction resource for the given type, pre-expanding the
	 *
	 * @param type must not be {@literal null}.
	 * @param pageable the pageable to can be {@literal null}.
	 * @return
	 */
	public Link linkToPagedResource(Class<?> type, Pageable pageable) {

		ResourceMetadata metadata = mappings.getMetadataFor(type);
		String href = linkFor(type).toString();
		UriComponents components = prepareUri(href, metadata, pageable);

		TemplateVariables variables = getTemplateVariables(components, metadata, pageable).//
				concat(getProjectionVariable(type));

		return Link.of(UriTemplate.of(href).with(variables), metadata.getRel());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.EntityLinks#linkToCollectionResource(java.lang.Class)
	 */
	@Override
	public Link linkToCollectionResource(Class<?> type) {
		return linkToPagedResource(type, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.EntityLinks#linkToItemResource(java.lang.Class, java.lang.Object)
	 */
	@Override
	public Link linkToItemResource(Class<?> type, Object id) {

		Assert.isInstanceOf(Serializable.class, id, "Id must be assignable to Serializable!");

		ResourceMetadata metadata = mappings.getMetadataFor(type);
		String mappedId = idConverters.getPluginFor(type)//
				.orElse(DefaultIdConverter.INSTANCE)//
				.toRequestId((Serializable) id, type);

		Link link = linkFor(type).slash(mappedId).withRel(metadata.getItemResourceRel());
		return Link.of(UriTemplate.of(link.getHref()).with(getProjectionVariable(type)).toString(),
				metadata.getItemResourceRel());
	}

	/**
	 * Returns all links to search resource for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 * @since 2.3
	 */
	public Links linksToSearchResources(Class<?> type) {
		return linksToSearchResources(type, null, null);
	}

	/**
	 * Returns all link to search resources for the given type, pre-expanded with the given {@link Pageable} if
	 * applicable.
	 *
	 * @param type must not be {@literal null}.
	 * @param pageable can be {@literal null}.
	 * @return
	 * @since 2.3
	 */
	public Links linksToSearchResources(Class<?> type, Pageable pageable) {
		return linksToSearchResources(type, pageable, null);
	}

	/**
	 * Returns all link to search resources for the given type, pre-expanded with the given {@link Sort} if applicable.
	 *
	 * @param type must not be {@literal null}.
	 * @param sort can be {@literal null}.
	 * @return
	 * @since 2.3
	 */
	public Links linksToSearchResources(Class<?> type, Sort sort) {
		return linksToSearchResources(type, null, sort);
	}

	/**
	 * Creates the link to the search resource with the given {@link LinkRelation} for a given type.
	 *
	 * @param domainType must not be {@literal null}.
	 * @param relation must not be {@literal null}.
	 * @return
	 * @since 2.3
	 */
	public Link linkToSearchResource(Class<?> domainType, LinkRelation relation) {
		return getSearchResourceLinkFor(domainType, relation, null, null);
	}

	/**
	 * Creates the link to the search resource with the given {@link LinkRelation} for a given type. Uses the given
	 * {@link Pageable} to pre-expand potentially available template variables.
	 *
	 * @param domainType must not be {@literal null}.
	 * @param relation must not be {@literal null}.
	 * @param pageable can be {@literal null}.
	 * @return
	 * @since 2.3
	 */
	public Link linkToSearchResource(Class<?> domainType, LinkRelation relation, Pageable pageable) {
		return getSearchResourceLinkFor(domainType, relation, pageable, null);
	}

	/**
	 * Creates the link to the search resource with the given {@link LinkRelation} for a given type. Uses the given
	 * {@link Sort} to pre-expand potentially available template variables.
	 *
	 * @param domainType must not be {@literal null}.
	 * @param relation must not be {@literal null}.
	 * @param sort can be {@literal null}.
	 * @return
	 * @since 2.3
	 */
	public Link linkToSearchResource(Class<?> domainType, LinkRelation relation, Sort sort) {
		return getSearchResourceLinkFor(domainType, relation, null, sort);
	}

	/**
	 * Returns all links to search resources of the given type. Pre-expands the template with the given {@link Pageable}
	 * and {@link Sort} if applicable.
	 *
	 * @param type must not be {@literal null}.
	 * @param pageable can be {@literal null}.
	 * @param sort can be {@literal null}.
	 * @return
	 */
	private Links linksToSearchResources(Class<?> type, Pageable pageable, Sort sort) {

		return mappings.getSearchResourceMappings(type).getExportedMappings() //
				.map(MethodResourceMapping::getRel) //
				.map(it -> getSearchResourceLinkFor(type, it, pageable, sort)) //
				.collect(Links.collector());
	}

	/**
	 * Returns the link pointing to the search resource with the given {@link LinkRelation} of the given type and
	 * pre-expands the calculated URi template with the given {@link Pageable} and {@link Sort}.
	 *
	 * @param type must not be {@literal null}.
	 * @param rel must not be {@literal null}.
	 * @param pageable can be {@literal null}.
	 * @param sort can be {@literal null}.
	 * @return
	 */
	private Link getSearchResourceLinkFor(Class<?> type, LinkRelation rel, Pageable pageable, Sort sort) {

		Assert.notNull(type, "Domain type must not be null!");
		Assert.notNull(rel, "Relation name must not be null!");

		SearchResourceMappings searchMappings = mappings.getSearchResourceMappings(type);
		MethodResourceMapping mapping = searchMappings.getExportedMethodMappingForRel(rel);

		if (mapping == null) {
			return null;
		}

		LinkBuilder builder = linkFor(type).//
				slash(mappings.getSearchResourceMappings(type).getPath()).//
				slash(mapping.getPath());

		UriComponents uriComponents = prepareUri(builder.toString(), mapping, pageable, sort);

		TemplateVariables variables = getParameterVariables(mapping).//
				concat(getTemplateVariables(uriComponents, mapping, pageable, sort)).//
				concat(getProjectionVariable(mapping.getReturnedDomainType()));

		return Link.of(UriTemplate.of(uriComponents.toString()).with(variables), mapping.getRel());
	}

	/**
	 * Returns the {@link TemplateVariables} to be added for pagination for the given {@link UriComponentsBuilder} in case
	 * the given {@link ResourceMapping} is a paging resource.
	 *
	 * @param components must not be {@literal null}.
	 * @param mapping must not be {@literal null}.
	 * @param pageable can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private TemplateVariables getTemplateVariables(UriComponents components, ResourceMapping mapping, Pageable pageable) {

		if (mapping.isPagingResource()) {
			return templateVariables.getPaginationTemplateVariables(null, components);
		} else {
			return TemplateVariables.NONE;
		}
	}

	/**
	 * Returns all {@link TemplateVariables} that need to be added based on the given {@link UriComponents},
	 * {@link MethodResourceMapping}, {@link Pageable} and {@link Sort}.
	 *
	 * @param components must not be {@literal null}.
	 * @param mapping must not be {@literal null}.
	 * @param pageable can be {@literal null}
	 * @param sort can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private TemplateVariables getTemplateVariables(UriComponents components, MethodResourceMapping mapping,
			Pageable pageable, Sort sort) {

		if (mapping.isSortableResource()) {
			return templateVariables.getSortTemplateVariables(null, components);
		} else {
			return getTemplateVariables(components, mapping, pageable);
		}
	}

	/**
	 * Returns the {@link TemplateVariables} for the projection parameter if projections are vonfigured for the given
	 * type.
	 *
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private TemplateVariables getProjectionVariable(Class<?> type) {

		ProjectionDefinitionConfiguration projectionConfiguration = config.getProjectionConfiguration();

		if (projectionConfiguration.hasProjectionFor(type)) {
			return new TemplateVariables(new TemplateVariable(projectionConfiguration.getParameterName(), REQUEST_PARAM));
		} else {
			return TemplateVariables.NONE;
		}
	}

	/**
	 * Returns the {@link TemplateVariables} for all parameters of the given {@link MethodResourceMapping}.
	 *
	 * @param mapping must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private TemplateVariables getParameterVariables(MethodResourceMapping mapping) {

		List<TemplateVariable> variables = new ArrayList<TemplateVariable>();

		for (ParameterMetadata metadata : mapping.getParametersMetadata()) {
			variables.add(new TemplateVariable(metadata.getName(), VariableType.REQUEST_PARAM));
		}

		return new TemplateVariables(variables);
	}

	private UriComponents prepareUri(String uri, MethodResourceMapping mapping, Pageable pageable, Sort sort) {

		if (mapping.isSortableResource()) {
			UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(uri);
			templateVariables.enhance(uriBuilder, null, sort);
			return uriBuilder.build();
		} else {
			return prepareUri(uri, mapping, pageable);
		}
	}

	private UriComponents prepareUri(String uri, ResourceMapping mapping, Pageable pageable) {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(uri);

		if (mapping.isPagingResource()) {
			templateVariables.enhance(uriBuilder, null, pageable);
		}

		return uriBuilder.build();
	}
}
