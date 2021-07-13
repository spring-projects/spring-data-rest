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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;

/**
 * Unit tests for {@link PersistentEntitiesResourceMappings}.
 *
 * @author Oliver Gierke
 */
public class PersistentEntitiesResourceMappingsUnitTests {

	@Test // DATAREST-1320
	public void doesNotConsiderCachedNullValuesToIndicateMappingAvailable() {

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();

		PersistentEntitiesResourceMappings mappings = new PersistentEntitiesResourceMappings(
				PersistentEntities.of(context));

		assertThat(mappings.getMetadataFor(String.class)).isNull();
		assertThat(mappings.hasMappingFor(String.class)).isFalse();
	}

	@Test // GH-2033
	public void transparentlyAddsValueToCacheOnHasMappingRequests() {

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		context.getPersistentEntity(Sample.class);

		PersistentEntitiesResourceMappings mappings = new PersistentEntitiesResourceMappings(
				PersistentEntities.of(context));

		assertThat(mappings.hasMappingFor(Sample.class)).isTrue();
	}

	class Sample {}
}
