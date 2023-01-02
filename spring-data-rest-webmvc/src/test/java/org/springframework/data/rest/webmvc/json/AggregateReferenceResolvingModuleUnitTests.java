/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link AggregateReferenceResolvingModule}.
 *
 * @author Oliver Drotbohm
 */
@ExtendWith(MockitoExtension.class)
class AggregateReferenceResolvingModuleUnitTests {

	@Mock UriToEntityConverter uriToEntityConverter;

	@Test // GH-2033
	void processesArtificialPropertiesCorrectly() {

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		context.getPersistentEntity(Other.class);

		PersistentEntities entities = PersistentEntities.of(context);
		PersistentEntitiesResourceMappings mappings = new PersistentEntitiesResourceMappings(entities);

		ObjectMapper mapper = new ObjectMapper()
				.addMixIn(SomeType.class, SomeTypeMixin.class)
				.registerModule(new AggregateReferenceResolvingModule(uriToEntityConverter, mappings));

		assertThatNoException().isThrownBy(() -> {
			mapper.readValue("{}", SomeType.class);
		});
	}

	public static class SomeType {

		void setSomeProperty(Other other) {}
	}

	@RestResource
	public static class Other {}

	public abstract static class SomeTypeMixin {

		// Rename property to expose a property that's not named like the actual member
		@JsonProperty("foo")
		public abstract void setSomeProperty(Other other);
	}
}
