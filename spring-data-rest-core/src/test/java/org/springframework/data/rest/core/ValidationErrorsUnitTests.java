/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;

/**
 * Unit tests for {@link ValidationErrors}.
 * 
 * @author Oliver Gierke
 */
public class ValidationErrorsUnitTests {

	KeyValueMappingContext context = new KeyValueMappingContext();

	/**
	 * @see DATAREST-798
	 */
	@Test
	public void exposesNestedViolationsCorrectly() {

		ValidationErrors errors = new ValidationErrors(new Foo(), context.getPersistentEntity(Foo.class));

		errors.pushNestedPath("bars[0]");
		errors.rejectValue("field", "asdf");
		errors.popNestedPath();

		assertThat(errors.getFieldError().getField(), is("bars[0].field"));
	}

	static class Foo {
		List<Bar> bars = new ArrayList<Bar>();
	}

	static class Bar {
		String field;
	}
}
