/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import static org.springframework.http.HttpMethod.*;

import lombok.RequiredArgsConstructor;

import java.util.function.Function;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Configuration type to register filters customizing the HTTP methods supported. By default, filters registered are
 * applied globally. Domain type specific filters can be registered via {@link #forDomainType(Class)}. Useful global
 * shortcuts like {@link #disablePutOnItemResources()} do exist as well.
 * 
 * @author Oliver Gierke
 * @see #forDomainType(Class)
 * @since 3.1
 */
public class ExposureConfiguration implements ExposureConfigurer {

	private ComposableFilter<ResourceMetadata, ConfigurableHttpMethods> collection = AggregateResourceHttpMethodsFilter
			.none();
	private ComposableFilter<ResourceMetadata, ConfigurableHttpMethods> item = AggregateResourceHttpMethodsFilter.none();
	private ComposableFilter<PropertyAwareResourceMapping, ConfigurableHttpMethods> property = AssociationResourceHttpMethodsFilter
			.none();

	private Function<Class<?>, Boolean> creationViaPut = __ -> true;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ExposureConfigurer#withCollectionExposure(org.springframework.data.rest.core.mapping.ExposureConfigurer.AggregateResourceHttpMethodsFilter)
	 */
	@Override
	public ExposureConfiguration withCollectionExposure(AggregateResourceHttpMethodsFilter filter) {

		this.collection = filter;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ExposureConfigurer#withItemExposure(org.springframework.data.rest.core.mapping.ExposureConfigurer.AggregateResourceHttpMethodsFilter)
	 */
	@Override
	public ExposureConfiguration withItemExposure(AggregateResourceHttpMethodsFilter filter) {

		this.item = filter;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ExposureConfigurer#withAssociationExposure(org.springframework.data.rest.core.mapping.ExposureConfigurer.AssociationResourceHttpMethodsFilter)
	 */
	@Override
	public ExposureConfiguration withAssociationExposure(AssociationResourceHttpMethodsFilter filter) {

		this.property = filter;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ExposureConfigurer#disablePutForCreation()
	 */
	@Override
	public ExposureConfiguration disablePutForCreation() {

		this.creationViaPut = __ -> false;
		return this;
	}

	/**
	 * Returns a {@link ExposureConfigurer} to allow the registration of type specific
	 * {@link AggregateResourceHttpMethodsFilter} and {@link AssociationResourceHttpMethodsFilter}s which means the
	 * configured filters will only be invoked for aggregates of the given type and properties owned by that type
	 * respectively.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public ExposureConfigurer forDomainType(Class<?> type) {

		Assert.notNull(type, "Type  must not be null!");
		return new TypeBasedExposureConfigurer(type);
	}

	/**
	 * Disables the support for {@link HttpMethod#PUT} for item resources.
	 * 
	 * @return
	 */
	public ExposureConfiguration disablePutOnItemResources() {

		this.item = item.andThen((__, httpMethods) -> httpMethods.disable(PUT));
		return this;
	}

	/**
	 * Disables the support for {@link HttpMethod#PATCH} for item resources.
	 * 
	 * @return
	 */
	public ExposureConfiguration disablePatchOnItemResources() {

		this.item = item.andThen((__, httpMethods) -> httpMethods.disable(PATCH));
		return this;
	}

	/**
	 * Returns whether PUT requests can be used to create new instances for the type backing the given
	 * {@link ResourceMetadata}.
	 * 
	 * @param metadata must not be {@literal null}.
	 * @return
	 */
	public boolean allowsPutForCreation(ResourceMetadata metadata) {

		Assert.notNull(metadata, "ResourceMetadata must not be null!");

		return allowsPutForCreation(metadata.getDomainType());
	}

	/**
	 * Returns whether PUT requests can be used to create new instances of the given domain type.
	 * 
	 * @param metadata must not be {@literal null}.
	 * @return
	 */
	public boolean allowsPutForCreation(Class<?> domainType) {

		Assert.notNull(domainType, "Domain type must not be null!");

		return creationViaPut.apply(domainType);
	}

	HttpMethods filter(ConfigurableHttpMethods methods, ResourceType type, ResourceMetadata metadata) {

		switch (type) {
			case COLLECTION:
				return collection.filter(metadata, methods);
			case ITEM:
				return item.filter(metadata, methods);
		}

		throw new IllegalArgumentException();
	}

	HttpMethods filter(ConfigurableHttpMethods methods, PropertyAwareResourceMapping mapping) {
		return property.filter(mapping, methods);
	}

	private static <T> ComposableFilter<ResourceMetadata, ConfigurableHttpMethods> withTypeFilter(Class<?> type,
			ComposableFilter<ResourceMetadata, ConfigurableHttpMethods> function) {

		return (metadata, httpMethods) -> //
		type.isAssignableFrom(metadata.getDomainType()) //
				? function.filter(metadata, httpMethods)
				: httpMethods;
	}

	/**
	 * An intermediate {@link ExposureConfigurer} to forward the general configuration API but register the configured
	 * filters under the condition that the configured type is matched.
	 *
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	private class TypeBasedExposureConfigurer implements ExposureConfigurer {

		private final Class<?> type;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.mapping.ExposureConfigurer#withCollectionExposure(org.springframework.data.rest.core.mapping.ExposureConfigurer.AggregateResourceHttpMethodsFilter)
		 */
		public ExposureConfigurer withCollectionExposure(AggregateResourceHttpMethodsFilter filter) {

			ExposureConfiguration config = ExposureConfiguration.this;

			config.collection = config.collection.andThen(withTypeFilter(type, filter));
			return this;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.mapping.ExposureConfigurer#withItemExposure(org.springframework.data.rest.core.mapping.ExposureConfigurer.AggregateResourceHttpMethodsFilter)
		 */
		public ExposureConfigurer withItemExposure(AggregateResourceHttpMethodsFilter filter) {

			ExposureConfiguration config = ExposureConfiguration.this;

			config.item = config.item.andThen(withTypeFilter(type, filter));
			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.mapping.ExposureConfiguration.ExposureConfigurer#withAssociationExposure(java.util.function.BiFunction)
		 */
		@Override
		public ExposureConfigurer withAssociationExposure(AssociationResourceHttpMethodsFilter filter) {

			ExposureConfiguration config = ExposureConfiguration.this;

			AssociationResourceHttpMethodsFilter typeFilter = //
					(mapping, methods) -> type.isAssignableFrom(mapping.getProperty().getOwner().getType()) //
							? filter.filter(mapping, methods) //
							: methods;

			config.property = config.property.andThen(typeFilter);
			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.mapping.ExposureConfigurer#disableCreationViaPut()
		 */
		@Override
		public ExposureConfigurer disablePutForCreation() {

			ExposureConfiguration config = ExposureConfiguration.this;

			Function<Class<?>, Boolean> current = config.creationViaPut;

			config.creationViaPut = type -> this.type.isAssignableFrom(type) ? false : current.apply(type);
			return this;
		}
	}
}
