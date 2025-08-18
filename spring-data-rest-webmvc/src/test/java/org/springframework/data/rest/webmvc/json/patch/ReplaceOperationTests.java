/*
 * Copyright 2014-2025 the original author or authors.
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReplaceOperationTests {

	@Test
	void replaceBooleanPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		ReplaceOperation replace = ReplaceOperation.valueAt("/1/complete").with(true);
		replace.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todos.get(1).isComplete()).isTrue();
	}

	@Test
	void replaceTextPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		ReplaceOperation replace = ReplaceOperation.valueAt("/1/description").with("BBB");
		replace.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todos.get(1).getDescription()).isEqualTo("BBB");
	}

	@Test
	void replaceTextPropertyValueWithANumber() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		ReplaceOperation replace = ReplaceOperation.valueAt("/1/description").with(22);
		replace.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todos.get(1).getDescription()).isEqualTo("22");
	}

	@Test // DATAREST-885
	void replaceObjectPropertyValue() throws Exception {

		Todo todo = new Todo(1L, "A", false);

		ObjectMapper mapper = new ObjectMapper();
		ReplaceOperation replace = ReplaceOperation.valueAt("/type")
				.with(new JsonLateObjectEvaluator(mapper, mapper.readTree("{ \"value\" : \"new\" }")));
		replace.perform(todo, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todo.getType()).isNotNull();
		assertThat(todo.getType().getValue()).isNotNull();
		assertThat(todo.getType().getValue().equals("new")).isTrue();
	}

	@Test // DATAREST-1338
	void replacesMapValueCorrectly() throws Exception {

		Book book = new Book();
		book.characters = new HashMap<>();
		book.characters.put("protagonist", "Pinco");

		ReplaceOperation.valueAt("/characters/protagonist") //
				.with(prepareValue("\"Pallo\"")) //
				.perform(book, Book.class, TestPropertyPathContext.INSTANCE);

		assertThat(book.characters.get("protagonist")).isEqualTo("Pallo");
	}

	private static Object prepareValue(String json) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(json);
		return new JsonLateObjectEvaluator(mapper, node);
	}

	// DATAREST-1338
	class Book {
		public Map<String, String> characters;
	}
}
