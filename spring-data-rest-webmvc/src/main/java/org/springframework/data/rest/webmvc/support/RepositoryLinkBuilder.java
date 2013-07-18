/*
 * Copyright 2013 the original author or authors.
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

import java.net.URI;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.RepositoryController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.core.LinkBuilderSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.web.util.UriComponentsBuilder;

public class RepositoryLinkBuilder extends LinkBuilderSupport<RepositoryLinkBuilder> {

	private final ResourceMetadata metadata;

	public RepositoryLinkBuilder(ResourceMetadata metadata, URI baseUri) {
		this(metadata, prepareBuilder(baseUri, metadata));
	}

	private RepositoryLinkBuilder(ResourceMetadata metadata, UriComponentsBuilder builder) {

		super(builder);
		this.metadata = metadata;
	}

	private static UriComponentsBuilder prepareBuilder(URI baseUri, ResourceMetadata metadata) {

		UriComponentsBuilder builder = baseUri != null ? UriComponentsBuilder.fromUri(baseUri) : ControllerLinkBuilder
				.linkTo(RepositoryController.class).toUriComponentsBuilder();
		return builder.path(metadata.getPath().toString());
	}

	@Override
	public RepositoryLinkBuilder slash(Object object) {

		if (object instanceof PersistentProperty) {
			return slash((PersistentProperty<?>) object);
		}

		return super.slash(object);
	}

	public RepositoryLinkBuilder slash(PersistentProperty<?> property) {

		String propName = property.getName();

		if (metadata.isManagedResource(property)) {
			return slash(metadata.getMappingFor(property).getPath());
		} else {
			return slash(propName);
		}
	}

	public Link withResourceRel() {
		return withRel(metadata.getRel());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.core.LinkBuilderSupport#createNewInstance(org.springframework.web.util.UriComponentsBuilder)
	 */
	@Override
	protected RepositoryLinkBuilder createNewInstance(UriComponentsBuilder builder) {
		return new RepositoryLinkBuilder(this.metadata, builder);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.core.LinkBuilderSupport#getThis()
	 */
	@Override
	protected RepositoryLinkBuilder getThis() {
		return this;
	}
}
