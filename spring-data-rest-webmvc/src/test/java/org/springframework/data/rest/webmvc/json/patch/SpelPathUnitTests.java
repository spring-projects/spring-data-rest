/*
 * Copyright 2014-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.rest.webmvc.json.patch.SpelPath.TypedSpelPath;
import org.springframework.data.rest.webmvc.json.patch.SpelPath.UntypedSpelPath;

/**
 * Unit tests for {@link SpelPath}.
 *
 * @author Oliver Gierke
 */
class SpelPathUnitTests {

	@Test
	void listIndex() {

		UntypedSpelPath expr = SpelPath.untyped("/1/description");

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		Object value = expr.bindTo(Todo.class).getValue(todos);

		assertThat(value).isEqualTo("B");
	}

	@Test
	void accessesLastCollectionElementWithDash() {

		UntypedSpelPath expr = SpelPath.untyped("/-/description");

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		Object value = expr.bindTo(Todo.class).getValue(todos);

		assertThat(value).isEqualTo("C");
	}

	@Test // DATAREST-1152
	void cachesSpelPath() {

		UntypedSpelPath left = SpelPath.untyped("/description");
		UntypedSpelPath right = SpelPath.untyped("/description");

		assertThat(left).isSameAs(right);
	}

	@Test // DATAREST-1152
	void cachesTypedSpelPath() {

		UntypedSpelPath source = SpelPath.untyped("/description");
		TypedSpelPath left = source.bindTo(Todo.class);
		TypedSpelPath right = source.bindTo(Todo.class);

		assertThat(left).isSameAs(right);
	}

	@Test // DATAREST-1274
	void supportsMultiDigitCollectionIndex() {
		assertThat(SpelPath.untyped("/11/description").bindTo(Todo.class).getLeafType()).isEqualTo(String.class);
	}

	@Test // DATAREST-1338
	void handlesStringMapKeysInPathExpressions() {

		TypedSpelPath path = SpelPath.untyped("people/Dave/name").bindTo(MapWrapper.class);

		assertThat(path.getExpressionString()).isEqualTo("people['Dave'].name");
		assertThat(path.getLeafType()).isEqualTo(String.class);
	}

	@Test // DATAREST-1338
	void handlesIntegerMapKeysInPathExpressions() {

		TypedSpelPath path = SpelPath.untyped("peopleByInt/0/name").bindTo(MapWrapper.class);

		assertThat(path.getExpressionString()).isEqualTo("peopleByInt[0].name");
		assertThat(path.getLeafType()).isEqualTo(String.class);
	}

	// DATAREST-1338

	static class Person {
		String name;
	}

	static class MapWrapper {
		Map<String, Person> people;
		Map<Integer, Person> peopleByInt;
	}
}
