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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.*;

import org.junit.Test;

/**
 * Unit tests for {@link ExposureConfiguration}.
 * 
 * @author Oliver Gierke
 */
public class ExposureConfigurationUnitTests {

	@Test // DATAREST-948
	public void appliesTypeBasedCollectionFiltersOnlyForMatchingTypes() {

		ExposureConfiguration configuration = new ExposureConfiguration();

		configuration.withCollectionExposure((metadata, methods) -> methods.disable(PUT));
		configuration.forDomainType(Sample.class).withCollectionExposure((metadata, methods) -> methods.disable(POST));

		ConfigurableHttpMethods methods = ConfigurableHttpMethods.of(PUT, POST);

		assertThat(applyForCollection(configuration, methods, Sample.class)).isEmpty();
		assertThat(applyForCollection(configuration, methods, Object.class)).containsOnly(POST);
	}

	@Test // DATAREST-948
	public void appliesTypeBasedItemFiltersOnlyForMatchingTypes() {

		ExposureConfiguration configuration = new ExposureConfiguration();

		configuration.withItemExposure((metadata, methods) -> methods.disable(PUT));
		configuration.forDomainType(Sample.class).withItemExposure((metadata, methods) -> methods.disable(POST));

		ConfigurableHttpMethods methods = ConfigurableHttpMethods.of(PUT, POST);

		assertThat(applyForItem(configuration, methods, Sample.class)).isEmpty();
		assertThat(applyForItem(configuration, methods, Object.class)).containsOnly(POST);
	}

	@Test // DATAREST-948
	public void appliesTypeSpecificFilterForAggregateSubTypes() {

		ExposureConfiguration configuration = new ExposureConfiguration();
		configuration.forDomainType(Sample.class).withItemExposure((metadata, methods) -> methods.disable(POST));

		assertThat(applyForItem(configuration, ConfigurableHttpMethods.ALL, SubSample.class)).doesNotContain(POST);
	}

	private static HttpMethods applyForCollection(ExposureConfiguration config, ConfigurableHttpMethods methods,
			Class<?> type) {
		return config.filter(methods, ResourceType.COLLECTION, metdataFor(type));
	}

	private static HttpMethods applyForItem(ExposureConfiguration config, ConfigurableHttpMethods methods,
			Class<?> type) {
		return config.filter(methods, ResourceType.ITEM, metdataFor(type));
	}

	private static ResourceMetadata metdataFor(Class<?> domainType) {

		ResourceMetadata metadata = mock(ResourceMetadata.class);
		doReturn(domainType).when(metadata).getDomainType();

		return metadata;
	}

	static class Sample {}

	static class SubSample extends Sample {}
}
