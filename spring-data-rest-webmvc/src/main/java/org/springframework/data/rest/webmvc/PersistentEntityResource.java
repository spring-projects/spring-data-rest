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
package org.springframework.data.rest.webmvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * A Spring HATEOAS {@link Resource} subclass that holds a reference to the entity's {@link PersistentEntity} metadata.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class PersistentEntityResource extends Resource<Object> {

	private static final Resources<EmbeddedWrapper> NO_EMBEDDEDS = new Resources<EmbeddedWrapper>(
			Collections.<EmbeddedWrapper> emptyList());

	private final PersistentEntity<?, ?> entity;
	private final Resources<EmbeddedWrapper> embeddeds;
	private final boolean enforceAssociationLinks;

	/**
	 * Creates a new {@link PersistentEntityResource} for the given {@link PersistentEntity}, content, embedded
	 * {@link Resources}, links and flag whether to render all associations.
	 * 
	 * @param entity must not be {@literal null}.
	 * @param content must not be {@literal null}.
	 * @param links must not be {@literal null}.
	 * @param renderAllAssociations
	 * @param embeddeds can be {@literal null}.
	 */
	private PersistentEntityResource(PersistentEntity<?, ?> entity, Object content, Iterable<Link> links,
			boolean renderAllAssociations, Resources<EmbeddedWrapper> embeddeds) {

		super(content, links);

		Assert.notNull(entity, "PersistentEntity must not be null!");

		this.entity = entity;
		this.embeddeds = embeddeds == null ? NO_EMBEDDEDS : embeddeds;
		this.enforceAssociationLinks = renderAllAssociations;
	}

	/**
	 * Returns the {@link PersistentEntity} for the underlying instance.
	 * 
	 * @return
	 */
	public PersistentEntity<?, ? extends PersistentProperty<?>> getPersistentEntity() {
		return entity;
	}

	/**
	 * Returns the resources that are supposed to be rendered in the {@code _embedded} clause.
	 * 
	 * @return the embeddeds
	 */
	@JsonUnwrapped
	public Resources<EmbeddedWrapper> getEmbeddeds() {
		return embeddeds;
	}

	/**
	 * Returns whether the given {@link Link} shall be rendered for the resource.
	 * 
	 * @param link must not be {@literal null}.
	 * @return
	 */
	public boolean shouldRenderLink(Link link) {

		Assert.notNull(link, "Link must not be null!");

		if (enforceAssociationLinks) {
			return true;
		}

		for (EmbeddedWrapper wrapper : embeddeds) {
			if (wrapper.hasRel(link.getRel())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates a new {@link Builder} to create {@link PersistentEntityResource}s eventually.
	 * 
	 * @param content must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	public static Builder build(Object content, PersistentEntity<?, ?> entity) {
		return new Builder(content, entity);
	}

	/**
	 * Builder to create {@link PersistentEntityResource} instances.
	 *
	 * @author Oliver Gierke
	 */
	public static class Builder {

		private final Object content;
		private final PersistentEntity<?, ?> entity;
		private final List<Link> links = new ArrayList<Link>();

		private Resources<EmbeddedWrapper> embeddeds;
		private boolean renderAllAssociationLinks = false;

		/**
		 * Creates a new {@link Builder} instance for the given content and {@link PersistentEntity}.
		 * 
		 * @param content must not be {@literal null}.
		 * @param entity must not be {@literal null}.
		 */
		private Builder(Object content, PersistentEntity<?, ?> entity) {

			Assert.notNull(content, "Content must not be null!");
			Assert.notNull(entity, "PersistentEntity must not be null!");

			this.content = content;
			this.entity = entity;
		}

		/**
		 * Configures the builder to embedd the given E
		 * 
		 * @param resources can be {@literal null}.
		 * @return the builder
		 */
		public Builder withEmbedded(Iterable<EmbeddedWrapper> resources) {

			this.embeddeds = resources == null ? null : new Resources<EmbeddedWrapper>(resources);
			return this;
		}

		/**
		 * Configures the builder to render all association links independently of the embedded resources added.
		 * 
		 * @return the builder
		 */
		public Builder renderAllAssociationLinks() {

			this.renderAllAssociationLinks = true;
			return this;
		}

		/**
		 * Adds the given {@link Link} to the {@link PersistentEntityResource}.
		 * 
		 * @param link must not be {@literal null}.
		 * @return the builder
		 */
		public Builder withLink(Link link) {

			Assert.notNull(link, "Link must not be null!");

			this.links.add(link);
			return this;
		}

		/**
		 * Finally creates the {@link PersistentEntityResource} instance.
		 * 
		 * @return
		 */
		public PersistentEntityResource build() {
			return new PersistentEntityResource(entity, content, links, renderAllAssociationLinks, embeddeds);
		}
	}
}
