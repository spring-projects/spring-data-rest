package org.springframework.data.rest.webmvc.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.repository.mapping.ResourceMappings;
import org.springframework.data.rest.repository.mapping.ResourceMetadata;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.core.AbstractEntityLinks;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryEntityLinks extends AbstractEntityLinks {

	private final Repositories repositories;
	private final ResourceMappings mappings;
	private final RepositoryRestConfiguration config;

	@Autowired
	public RepositoryEntityLinks(Repositories repositories, ResourceMappings mappings, RepositoryRestConfiguration config) {

		Assert.notNull(repositories, "Repositories must not be null!");

		this.repositories = repositories;
		this.mappings = mappings;
		this.config = config;
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
		return linkFor(type).withRel(metadata.getRel());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.EntityLinks#linkToSingleResource(java.lang.Class, java.lang.Object)
	 */
	@Override
	public Link linkToSingleResource(Class<?> type, Object id) {

		ResourceMetadata metadata = mappings.getMappingFor(type);
		return linkFor(type).slash(id).withRel(metadata.getSingleResourceRel());
	}
}
