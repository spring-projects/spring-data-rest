/*
 * Copyright 2023 the original author or authors.
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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SlicedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A wrapper for a variety of {@link RepresentationModelAssemblers} to avoid having to depend on all of them from our
 * controllers.
 *
 * @author Oliver Drotbohm
 * @since 4.1
 * @soundtrack The Intersphere - Down (Wanderer, https://www.youtube.com/watch?v=3RIdTFJvDxg)
 */
public class RepresentationModelAssemblers {

	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);

	private final PagedResourcesAssembler<Object> pagedResourcesAssembler;
	private final SlicedResourcesAssembler<Object> slicedResourcesAssembler;
	private final PersistentEntityResourceAssembler persistentEntityResourceAssembler;

	/**
	 * Creates a new {@link RepresentationModelAssemblers} from the given {@link PagedResourcesAssembler},
	 * {@link SlicedResourcesAssembler} and {@link PersistentEntityResourceAssembler}.
	 *
	 * @param pagedResourcesAssembler must not be {@literal null}.
	 * @param slicedResourcesAssembler must not be {@literal null}.
	 * @param persistentEntityResourceAssembler must not be {@literal null}.
	 */
	public RepresentationModelAssemblers(PagedResourcesAssembler<Object> pagedResourcesAssembler,
			SlicedResourcesAssembler<Object> slicedResourcesAssembler,
			PersistentEntityResourceAssembler persistentEntityResourceAssembler) {

		Assert.notNull(pagedResourcesAssembler, "PagedResourcesAssembler must not be null");
		Assert.notNull(slicedResourcesAssembler, "SlicedResourcesAssembler must not be null");
		Assert.notNull(persistentEntityResourceAssembler, "PersistentEntityResourceAssembler must not be null");

		this.pagedResourcesAssembler = pagedResourcesAssembler;
		this.slicedResourcesAssembler = slicedResourcesAssembler;
		this.persistentEntityResourceAssembler = persistentEntityResourceAssembler;
	}

	/**
	 * Creates a new {@link CollectionModel} for the given source {@link Iterable} and domain type for forward into the
	 * model if the {@link Iterable} is empty.
	 *
	 * @param source must not be {@literal null}.
	 * @param domainType must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	CollectionModel<?> toCollectionModel(@Nullable Iterable<?> source, Class<?> domainType) {

		Assert.notNull(source, "Source Iterable must not be null!");
		Assert.notNull(domainType, "Domain type must not be null!");

		if (source instanceof Page page) {
			return entitiesToResources(page, domainType);
		} else if (source instanceof Slice slice) {
			return entitiesToResources(slice, domainType);
		} else if (source instanceof Iterable) {
			return entitiesToResources((Iterable<Object>) source, domainType);
		} else {
			return CollectionModel.empty(domainType);
		}
	}

	/**
	 * @param instance must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see PersistentEntityResourceAssembler#toFullResource(Object)
	 */
	PersistentEntityResource toFullResource(Object instance) {
		return persistentEntityResourceAssembler.toFullResource(instance);
	}

	/**
	 * @param instance must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see PersistentEntityResourceAssembler#toModel(Object)
	 */
	PersistentEntityResource toModel(Object instance) {
		return persistentEntityResourceAssembler.toModel(instance);
	}

	/**
	 * @param instance must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see PersistentEntityResourceAssembler#getExpandedSelfLink(Object)
	 */
	Link getExpandedSelfLink(Object instance) {
		return persistentEntityResourceAssembler.getExpandedSelfLink(instance);
	}

	private CollectionModel<?> entitiesToResources(Page<Object> page, Class<?> domainType) {

		return page.isEmpty()
				? pagedResourcesAssembler.toEmptyModel(page, domainType)
				: pagedResourcesAssembler.toModel(page, persistentEntityResourceAssembler);
	}

	private CollectionModel<?> entitiesToResources(Slice<Object> slice, Class<?> domainType) {

		return slice.isEmpty()
				? slicedResourcesAssembler.toEmptyModel(slice, domainType) //
				: slicedResourcesAssembler.toModel(slice, persistentEntityResourceAssembler);

	}

	private CollectionModel<?> entitiesToResources(Iterable<Object> entities, Class<?> domainType) {

		var selfLink = ControllerUtils.getDefaultSelfLink();

		return !entities.iterator().hasNext()
				? CollectionModel.of(List.of(WRAPPERS.emptyCollectionOf(domainType)), selfLink)
				: persistentEntityResourceAssembler.toCollectionModel(entities).add(selfLink);
	}
}
