/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A Spring HATEOAS {@link Resource} subclass that holds a reference to the entity's {@link PersistentEntity} metadata.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class PersistentEntityResource extends EntityModel<Object> {

	private static final Iterable<EmbeddedWrapper> NO_EMBEDDEDS = new NoLinksResources<EmbeddedWrapper>(
			Collections.<EmbeddedWrapper> emptyList());

	private final PersistentEntity<?, ?> entity;
	private final Iterable<EmbeddedWrapper> embeddeds;

	/**
	 * Returns whether the content of the resource is a new entity about to be created. Used to distinguish between
	 * creation and updates for incoming requests.
	 *
	 * @return
	 */
	private final @Getter boolean isNew;
	private final @Getter boolean nested;

	/**
	 * Creates a new {@link PersistentEntityResource} for the given {@link PersistentEntity}, content, embedded
	 * {@link Resources}, links and flag whether to render all associations.
	 *
	 * @param entity must not be {@literal null}.
	 * @param content must not be {@literal null}.
	 * @param links must not be {@literal null}.
	 * @param embeddeds can be {@literal null}.
	 */
	@SuppressWarnings("deprecation")
	private PersistentEntityResource(PersistentEntity<?, ?> entity, Object content, Iterable<Link> links,
			Iterable<EmbeddedWrapper> embeddeds, boolean isNew, boolean nested) {

		super(content, links);

		Assert.notNull(entity, "PersistentEntity must not be null!");

		this.entity = entity;
		this.embeddeds = embeddeds == null ? NO_EMBEDDEDS : embeddeds;
		this.isNew = isNew;
		this.nested = nested;
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
	 * Returns the {@link PersistentPropertyAccessor} for the underlying content bean.
	 *
	 * @return
	 */
	public PersistentPropertyAccessor<?> getPropertyAccessor() {
		return entity.getPropertyAccessor(getContent());
	}

	/**
	 * Returns the resources that are supposed to be rendered in the {@code _embedded} clause.
	 *
	 * @return the embeddeds
	 */
	public Iterable<EmbeddedWrapper> getEmbeddeds() {
		return embeddeds;
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

		private Iterable<EmbeddedWrapper> embeddeds;

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
		 * Configures the builder to embed the given {@link EmbeddedWrapper} instances. Creates a {@link Resources} instance
		 * to make sure the {@link EmbeddedWrapper} handling gets applied to the serialization output ignoring the links.
		 *
		 * @param resources can be {@literal null}.
		 * @return the builder
		 */
		public Builder withEmbedded(Iterable<EmbeddedWrapper> resources) {

			this.embeddeds = resources == null ? null : new NoLinksResources<EmbeddedWrapper>(resources);
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

		public Builder withLinks(List<Link> links) {

			Assert.notNull(links, "Links must not be null!");

			this.links.addAll(links);
			return this;
		}

		/**
		 * Finally creates the {@link PersistentEntityResource} instance.
		 *
		 * @return
		 */
		public PersistentEntityResource build() {
			return new PersistentEntityResource(entity, content, links, embeddeds, false, false);
		}

		/**
		 * Finally creates the {@link PersistentEntityResource} instance to symbolize the contained entity is about to be
		 * created.
		 *
		 * @return
		 */
		public PersistentEntityResource forCreation() {
			return new PersistentEntityResource(entity, content, links, embeddeds, true, false);
		}

		public PersistentEntityResource buildNested() {
			return new PersistentEntityResource(entity, content, links, embeddeds, false, true);
		}
	}

	private static class NoLinksResources<T> extends CollectionModel<T> {

		@SuppressWarnings("deprecation")
		public NoLinksResources(Iterable<T> content) {
			super(content);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.RepresentationModel#getLinks()
		 */
		@Override
		@JsonIgnore
		public Links getLinks() {
			return super.getLinks();
		}
	}
}
