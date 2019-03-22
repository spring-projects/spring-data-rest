/*
 * Copyright 2015-2017 original author or authors.
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

/**
 * Unit tests for {@link JsonSchema}.
 * 
 * @author Oliver Gierke
 */
public class JsonSchemaUnitTests {

	static final TypeInformation<?> type = ClassTypeInformation.from(Sample.class);

	@Test // DATAREST-492
	public void considersNumberPrimitivesJsonSchemaNumbers() {

		JsonSchemaProperty property = new JsonSchemaProperty("foo", null, "bar", false);

		assertThat(property.with(type.getRequiredProperty("foo")).type).isEqualTo("number");
	}

	static class Sample {
		double foo;
	}
}
