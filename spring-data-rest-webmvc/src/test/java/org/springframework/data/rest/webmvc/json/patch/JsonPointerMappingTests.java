/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.rest.webmvc.json.patch;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.webmvc.json.BindContextFactory;
import org.springframework.data.rest.webmvc.json.PersistentEntitiesBindContextFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JsonPointerMapping}.
 *
 * @author Oliver Drotbohm
 */
public class JsonPointerMappingTests {

	JsonPointerMapping verifier;

	@BeforeEach
	void setUp() {

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		context.getPersistentEntity(Sample.class);

		PersistentEntities entities = new PersistentEntities(Arrays.asList(context));
		BindContextFactory factory = new PersistentEntitiesBindContextFactory(entities);

		ObjectMapper mapper = new ObjectMapper();
		this.verifier = new JsonPointerMapping(factory.getBindContextFor(mapper));
	}

	@Test
	void verifiesSimpleProperty() {
		verifier.forRead("/firstname", Sample.class);
	}

	@Test
	void verifiesPathIntoCollection() {
		verifier.forRead("/collection/27/firstname", Sample.class);
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Sample {
		String firstname;
		Collection<Sample> collection;
	}
}
