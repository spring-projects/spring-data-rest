/*
 * Copyright 2015-2019 original author or authors.
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

import org.junit.Test;
import org.springframework.data.rest.webmvc.json.JsonSchema.JsonSchemaProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.util.Collection;

/**
 * Unit tests for {@link JsonSchema}.
 *
 * @author Oliver Gierke
 * @author Christoph Huber
 */
public class JsonSchemaUnitTests {

	private static final TypeInformation<?> typeWithDoubleField = ClassTypeInformation.from(TypeWithDoubleField.class);
	private static final TypeInformation<?> typeWithAssociations = ClassTypeInformation.from(TypeWithAssociations.class);

	@Test // DATAREST-492
	public void considersNumberPrimitivesJsonSchemaNumbers() {

		JsonSchemaProperty property = new JsonSchemaProperty("foo", null, "bar", false);

		assertThat(property.with(typeWithDoubleField.getRequiredProperty("foo")).type).isEqualTo("number");
	}

	@Test // DATAREST-1096
	public void allowArrayOfUris() {
		JsonSchemaProperty property = new JsonSchemaProperty("foos", null, null, false);

		final JsonSchemaProperty arrayOfUris = property.with(typeWithAssociations.getRequiredProperty("foos")).asAssociation();
		assertThat(arrayOfUris.type).isEqualTo("array");
		assertThat(arrayOfUris.items.get("type")).isEqualTo("string");
		assertThat(arrayOfUris.items.get("format")).isEqualTo("uri");
	}

	private static class TypeWithDoubleField {
		private double foo;
	}

	private static class TypeWithAssociations {
		private Collection<TypeWithDoubleField> foos;
	}
}
