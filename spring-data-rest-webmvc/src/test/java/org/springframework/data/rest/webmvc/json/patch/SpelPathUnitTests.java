/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.json.patch;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.data.rest.webmvc.json.patch.SpelPath.TypedSpelPath;

/**
 * Unit tests for {@link SpelPath}.
 *
 * @author Oliver Gierke
 */
public class SpelPathUnitTests {

	@Test
	public void listIndex() {

		SpelPath expr = SpelPath.of("/1/description");

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		assertEquals("B", (String) expr.bindTo(Todo.class).getValue(todos));
	}

	@Test
	public void listTilde() {

		SpelPath expr = SpelPath.of("/~/description");

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		assertEquals("C", (String) expr.bindTo(Todo.class).getValue(todos));
	}

	@Test // DATAREST-1152
	public void cachesSpelPath() {

		SpelPath left = SpelPath.of("/description");
		SpelPath right = SpelPath.of("/description");

		assertSame(left, right);
	}

	@Test // DATAREST-1152
	public void cachesTypedSpelPath() {

		SpelPath source = SpelPath.of("/description");
		TypedSpelPath left = source.bindTo(Todo.class);
		TypedSpelPath right = source.bindTo(Todo.class);

		assertSame(left, right);
	}

	@Test // DATAREST-1274
	public void supportsMultiDigitCollectionIndex() {
		assertThat(SpelPath.of("/11/description").getLeafType(Todo.class), is(typeCompatibleWith(String.class)));
	}
}
