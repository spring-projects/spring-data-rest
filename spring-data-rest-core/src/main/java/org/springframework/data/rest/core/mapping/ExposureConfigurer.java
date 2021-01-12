/*
 * Copyright 2018-2021 the original author or authors.
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

import org.springframework.data.mapping.Association;
import org.springframework.http.HttpMethod;

/**
 * Configuration API to register filters to customize the supported HTTP methods by different kinds of resources.
 *
 * @author Oliver Gierke
 * @since 2.1
 */
public interface ExposureConfigurer {

	/**
	 * A filter to post-process the supported HTTP methods by aggregate resources (collection or item resource).
	 *
	 * @author Oliver Gierke
	 */
	interface AggregateResourceHttpMethodsFilter extends ComposableFilter<ResourceMetadata, ConfigurableHttpMethods> {

		ConfigurableHttpMethods filter(ResourceMetadata metdata, ConfigurableHttpMethods httpMethods);

		/**
		 * Returns a default filter that just returns all {@link HttpMethods} as is, i.e. does not apply any filtering.
		 * 
		 * @return
		 */
		static AggregateResourceHttpMethodsFilter none() {
			return (metadata, httpMethods) -> httpMethods;
		}
	}

	/**
	 * A filter to post-process the supported HTTP methods by {@link Association} resources.
	 *
	 * @author Oliver Gierke
	 */
	interface AssociationResourceHttpMethodsFilter
			extends ComposableFilter<PropertyAwareResourceMapping, ConfigurableHttpMethods> {

		ConfigurableHttpMethods filter(PropertyAwareResourceMapping metdata, ConfigurableHttpMethods httpMethods);

		/**
		 * Returns a default filter that just returns all {@link HttpMethods} as is, i.e. does not apply any filtering.
		 * 
		 * @return
		 */
		static AssociationResourceHttpMethodsFilter none() {
			return (metadata, httpMethods) -> httpMethods;
		}
	}

	/**
	 * Registers the given {@link AggregateResourceHttpMethodsFilter} to be used for collection resources.
	 * 
	 * @param filter must not be {@literal null}.
	 * @return
	 */
	ExposureConfigurer withCollectionExposure(AggregateResourceHttpMethodsFilter filter);

	/**
	 * Registers the given {@link AggregateResourceHttpMethodsFilter} to be used for item resources.
	 * 
	 * @param filter
	 * @return
	 */
	ExposureConfigurer withItemExposure(AggregateResourceHttpMethodsFilter filter);

	/**
	 * Registers the given {@link AssociationResourceHttpMethodsFilter}.
	 * 
	 * @param filter
	 * @return
	 */
	ExposureConfigurer withAssociationExposure(AssociationResourceHttpMethodsFilter filter);

	/**
	 * Disables the ability to create new item resources via {@link HttpMethod#PUT}.
	 * 
	 * @return
	 */
	ExposureConfigurer disablePutForCreation();
}
