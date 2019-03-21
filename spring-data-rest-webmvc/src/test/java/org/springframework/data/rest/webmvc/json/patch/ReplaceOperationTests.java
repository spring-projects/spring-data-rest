/*
 * Copyright 2014-2017 the original author or authors.
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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReplaceOperationTests {

	@Test
	public void replaceBooleanPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		ReplaceOperation replace = new ReplaceOperation("/1/complete", true);
		replace.perform(todos, Todo.class);

		assertTrue(todos.get(1).isComplete());
	}

	@Test
	public void replaceTextPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		ReplaceOperation replace = new ReplaceOperation("/1/description", "BBB");
		replace.perform(todos, Todo.class);

		assertEquals("BBB", todos.get(1).getDescription());
	}

	@Test
	public void replaceTextPropertyValueWithANumber() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		ReplaceOperation replace = new ReplaceOperation("/1/description", 22);
		replace.perform(todos, Todo.class);

		assertEquals("22", todos.get(1).getDescription());
	}

	@Test // DATAREST-885
	public void replaceObjectPropertyValue() throws Exception {

		Todo todo = new Todo(1L, "A", false);

		ObjectMapper mapper = new ObjectMapper();
		ReplaceOperation replace = new ReplaceOperation("/type",
				new JsonLateObjectEvaluator(mapper, mapper.readTree("{ \"value\" : \"new\" }")));
		replace.perform(todo, Todo.class);

		assertNotNull(todo.getType());
		assertNotNull(todo.getType().getValue());
		assertTrue(todo.getType().getValue().equals("new"));
	}
}
